#!/usr/bin/env python3
"""
Long-running live location simulator for SmartTMS tracking.

By default it keeps replaying the provided route until you stop it with Ctrl+C.
Optional end conditions:
- --max-updates
- --duration-seconds

Examples:

python scripts/live_location_provider.py ^
  --username driver.arjun ^
  --password Driver@123 ^
  --vehicle-id 1 ^
  --start-lat 19.0760 ^
  --start-lng 72.8777

python scripts/live_location_provider.py ^
  --username driver.meera ^
  --password Driver@123 ^
  --vehicle-id 2 ^
  --points "12.9716,77.5946;12.9760,77.6000;12.9800,77.6050" ^
  --interval 2 ^
  --duration-seconds 120
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import List, Sequence
from urllib import error, request


DEFAULT_BASE_URL = "http://localhost:7082"
DEFAULT_INTERVAL_SECONDS = 2.0
DEFAULT_SPEED = 35.0
DEFAULT_TIMEOUT_SECONDS = 10.0
DEFAULT_ROUTE_RADIUS_METERS = 750.0
DEFAULT_GENERATED_POINTS = 16

DEFAULT_VEHICLE_ROUTE_PRESETS = {
    1: (19.0760, 72.8777),   # Mumbai
    2: (12.9716, 77.5946),   # Bengaluru
    3: (18.5204, 73.8567),   # Pune
    4: (23.0225, 72.5714),   # Ahmedabad
}


@dataclass(frozen=True)
class Coordinate:
    lat: float
    lng: float


@dataclass(frozen=True)
class AuthSession:
    token: str
    user_id: int


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Simulate a driver sending live location updates to SmartTMS tracking."
    )
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="Gateway base URL")
    parser.add_argument("--username", required=True, help="Driver username")
    parser.add_argument("--password", required=True, help="Driver password")
    parser.add_argument("--vehicle-id", required=True, type=int, help="Vehicle id assigned to the driver")
    parser.add_argument(
        "--points",
        help='Inline points like "12.9716,77.5946;12.9760,77.6000;12.9800,77.6050"',
    )
    parser.add_argument(
        "--points-file",
        help="Path to a CSV or JSON file containing route points",
    )
    parser.add_argument("--start-lat", type=float, help="Starting latitude for auto-generated route")
    parser.add_argument("--start-lng", type=float, help="Starting longitude for auto-generated route")
    parser.add_argument(
        "--route-radius-meters",
        type=float,
        default=DEFAULT_ROUTE_RADIUS_METERS,
        help="Radius for auto-generated looping route",
    )
    parser.add_argument(
        "--generated-points",
        type=int,
        default=DEFAULT_GENERATED_POINTS,
        help="How many points to generate for the auto-generated route",
    )
    parser.add_argument(
        "--interval",
        type=float,
        default=DEFAULT_INTERVAL_SECONDS,
        help="Delay in seconds between updates",
    )
    parser.add_argument(
        "--speed",
        type=float,
        default=DEFAULT_SPEED,
        help="Speed value to send in each payload",
    )
    parser.add_argument(
        "--max-updates",
        type=int,
        help="Stop automatically after sending this many updates",
    )
    parser.add_argument(
        "--duration-seconds",
        type=float,
        help="Stop automatically after this many seconds",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=DEFAULT_TIMEOUT_SECONDS,
        help="HTTP timeout in seconds",
    )
    parser.add_argument(
        "--skip-vehicle-check",
        action="store_true",
        help="Skip validating that the logged-in driver matches the vehicle assignment",
    )
    args = parser.parse_args()

    route_source_count = sum(
        1
        for is_set in (
            bool(args.points),
            bool(args.points_file),
            args.start_lat is not None or args.start_lng is not None,
        )
        if is_set
    )
    if route_source_count > 1:
        parser.error("Use only one route source: --points, --points-file, or --start-lat/--start-lng")
    if (args.start_lat is None) != (args.start_lng is None):
        parser.error("--start-lat and --start-lng must be provided together")
    if args.interval <= 0:
        parser.error("--interval must be greater than 0")
    if args.speed < 0:
        parser.error("--speed must be non-negative")
    if args.max_updates is not None and args.max_updates <= 0:
        parser.error("--max-updates must be greater than 0")
    if args.duration_seconds is not None and args.duration_seconds <= 0:
        parser.error("--duration-seconds must be greater than 0")
    if args.route_radius_meters <= 0:
        parser.error("--route-radius-meters must be greater than 0")
    if args.generated_points < 4:
        parser.error("--generated-points must be at least 4")

    return args


def main() -> int:
    args = parse_args()
    route = load_route(
        points_arg=args.points,
        points_file=args.points_file,
        vehicle_id=args.vehicle_id,
        start_lat=args.start_lat,
        start_lng=args.start_lng,
        route_radius_meters=args.route_radius_meters,
        generated_points=args.generated_points,
    )
    if len(route) < 1:
        raise SystemExit("No route points were parsed")

    print(f"Loaded {len(route)} route point(s)")

    session = login(
        base_url=args.base_url,
        username=args.username,
        password=args.password,
        timeout=args.timeout,
    )
    print(f"Logged in as '{args.username}' with auth user id {session.user_id}")

    if not args.skip_vehicle_check:
        validate_vehicle_assignment(
            base_url=args.base_url,
            session=session,
            vehicle_id=args.vehicle_id,
            timeout=args.timeout,
        )
        print(f"Vehicle {args.vehicle_id} is assigned to driver auth user id {session.user_id}")

    send_route(
        base_url=args.base_url,
        session=session,
        vehicle_id=args.vehicle_id,
        route=route,
        speed=args.speed,
        interval_seconds=args.interval,
        timeout=args.timeout,
        max_updates=args.max_updates,
        duration_seconds=args.duration_seconds,
    )
    return 0


def load_route(
    points_arg: str | None,
    points_file: str | None,
    vehicle_id: int,
    start_lat: float | None,
    start_lng: float | None,
    route_radius_meters: float,
    generated_points: int,
) -> List[Coordinate]:
    if points_arg:
        return parse_inline_points(points_arg)
    if points_file:
        return parse_points_file(Path(points_file))
    if start_lat is not None and start_lng is not None:
        return build_generated_loop(
            center=Coordinate(lat=start_lat, lng=start_lng),
            radius_meters=route_radius_meters,
            point_count=generated_points,
        )
    preset = DEFAULT_VEHICLE_ROUTE_PRESETS.get(vehicle_id)
    if preset is not None:
        return build_generated_loop(
            center=Coordinate(lat=preset[0], lng=preset[1]),
            radius_meters=route_radius_meters,
            point_count=generated_points,
        )
    raise SystemExit(
        "No route source was provided. Use --points, or provide --start-lat/--start-lng. "
        "Built-in presets only exist for seeded vehicle ids 1-4."
    )


def parse_inline_points(raw_points: str) -> List[Coordinate]:
    points: List[Coordinate] = []
    for raw_pair in raw_points.split(";"):
        raw_pair = raw_pair.strip()
        if not raw_pair:
            continue
        lat_str, lng_str = [part.strip() for part in raw_pair.split(",", 1)]
        points.append(Coordinate(lat=float(lat_str), lng=float(lng_str)))
    return points


def parse_points_file(path: Path) -> List[Coordinate]:
    if not path.exists():
        raise SystemExit(f"Points file not found: {path}")

    suffix = path.suffix.lower()
    if suffix == ".csv":
        return parse_csv_points(path)
    if suffix == ".json":
        return parse_json_points(path)

    raise SystemExit("Points file must be .csv or .json")


def parse_csv_points(path: Path) -> List[Coordinate]:
    points: List[Coordinate] = []
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        field_names = {name.lower(): name for name in (reader.fieldnames or [])}
        if "lat" not in field_names or "lng" not in field_names:
            raise SystemExit("CSV points file must contain 'lat' and 'lng' columns")

        for row in reader:
            points.append(
                Coordinate(
                    lat=float(row[field_names["lat"]]),
                    lng=float(row[field_names["lng"]]),
                )
            )
    return points


def parse_json_points(path: Path) -> List[Coordinate]:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)

    if not isinstance(data, list):
        raise SystemExit("JSON points file must be a list of objects with 'lat' and 'lng'")

    points: List[Coordinate] = []
    for item in data:
        if not isinstance(item, dict) or "lat" not in item or "lng" not in item:
            raise SystemExit("Each JSON point must contain 'lat' and 'lng'")
        points.append(Coordinate(lat=float(item["lat"]), lng=float(item["lng"])))
    return points


def build_generated_loop(center: Coordinate, radius_meters: float, point_count: int) -> List[Coordinate]:
    lat_offset = radius_meters / 111_320.0
    lng_scale = max(math.cos(math.radians(center.lat)), 0.1)
    lng_offset = radius_meters / (111_320.0 * lng_scale)

    points: List[Coordinate] = []
    for index in range(point_count):
        angle = (2.0 * math.pi * index) / point_count
        lat = center.lat + (lat_offset * math.sin(angle))
        lng = center.lng + (lng_offset * math.cos(angle))
        points.append(Coordinate(lat=lat, lng=lng))
    return points


def login(base_url: str, username: str, password: str, timeout: float) -> AuthSession:
    payload = {"username": username, "password": password}
    response = http_json(
        method="POST",
        url=f"{base_url.rstrip('/')}/auth/login",
        body=payload,
        timeout=timeout,
    )
    token = response.get("token")
    user_id = response.get("userId")
    if not token or user_id is None:
        raise SystemExit("Login response did not contain token and userId")
    return AuthSession(token=token, user_id=int(user_id))


def validate_vehicle_assignment(base_url: str, session: AuthSession, vehicle_id: int, timeout: float) -> None:
    response = http_json(
        method="GET",
        url=f"{base_url.rstrip('/')}/vehicle/{vehicle_id}",
        token=session.token,
        timeout=timeout,
    )
    driver = response.get("driver")
    if not isinstance(driver, dict):
        raise SystemExit(f"Vehicle {vehicle_id} has no assigned driver")

    driver_auth_user_id = driver.get("authUserId")
    if driver_auth_user_id is None:
        raise SystemExit(
            "Vehicle response did not include driver.authUserId. "
            "If your running service is older than the current code, update/restart it or use --skip-vehicle-check."
        )

    if int(driver_auth_user_id) != session.user_id:
        raise SystemExit(
            f"Vehicle {vehicle_id} is assigned to auth user id {driver_auth_user_id}, "
            f"but logged-in driver is {session.user_id}"
        )


def send_route(
    base_url: str,
    session: AuthSession,
    vehicle_id: int,
    route: Sequence[Coordinate],
    speed: float,
    interval_seconds: float,
    timeout: float,
    max_updates: int | None,
    duration_seconds: float | None,
) -> None:
    start_time = time.time()
    updates_sent = 0
    iterations = 0
    print("Streaming started. Press Ctrl+C to stop.")

    try:
        while True:
            iterations += 1
            print(f"Starting route pass {iterations}")
            for index, point in enumerate(route):
                if should_stop(start_time, updates_sent, max_updates, duration_seconds):
                    print_stop_summary(updates_sent, start_time)
                    return

                heading = calculate_heading(
                    route[index - 1] if index > 0 else point,
                    point,
                    route[index + 1] if index + 1 < len(route) else point,
                )

                payload = {
                    "vehicleId": vehicle_id,
                    "driverId": session.user_id,
                    "lat": point.lat,
                    "lng": point.lng,
                    "speed": speed,
                    "heading": heading,
                    "timestamp": int(time.time()),
                }

                response = http_json(
                    method="POST",
                    url=f"{base_url.rstrip('/')}/track/location",
                    body=payload,
                    token=session.token,
                    timeout=timeout,
                )
                updates_sent += 1
                print(
                    f"[pass {iterations} | update {updates_sent}] "
                    f"sent lat={point.lat:.6f}, lng={point.lng:.6f}, heading={heading:.2f} -> {response}"
                )

                if should_stop(start_time, updates_sent, max_updates, duration_seconds):
                    print_stop_summary(updates_sent, start_time)
                    return

                time.sleep(interval_seconds)
    except KeyboardInterrupt:
        print("\nStopped by user.")
        print_stop_summary(updates_sent, start_time)


def should_stop(
    start_time: float,
    updates_sent: int,
    max_updates: int | None,
    duration_seconds: float | None,
) -> bool:
    if max_updates is not None and updates_sent >= max_updates:
        return True
    if duration_seconds is not None and (time.time() - start_time) >= duration_seconds:
        return True
    return False


def print_stop_summary(updates_sent: int, start_time: float) -> None:
    elapsed = max(time.time() - start_time, 0.0)
    print(f"Finished after {updates_sent} update(s) over {elapsed:.1f} second(s).")


def calculate_heading(previous_point: Coordinate, current_point: Coordinate, next_point: Coordinate) -> float:
    source = previous_point if previous_point != current_point else current_point
    target = next_point if next_point != current_point else current_point

    lat1 = math.radians(source.lat)
    lat2 = math.radians(target.lat)
    diff_lng = math.radians(target.lng - source.lng)

    x = math.sin(diff_lng) * math.cos(lat2)
    y = math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(diff_lng)

    if x == 0 and y == 0:
        return 0.0

    heading = math.degrees(math.atan2(x, y))
    return (heading + 360.0) % 360.0


def http_json(
    method: str,
    url: str,
    body: dict | None = None,
    token: str | None = None,
    timeout: float = DEFAULT_TIMEOUT_SECONDS,
) -> dict:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")

    req = request.Request(url=url, data=data, headers=headers, method=method)
    try:
        with request.urlopen(req, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        raise SystemExit(f"HTTP {exc.code} calling {url}: {raw}") from exc
    except error.URLError as exc:
        raise SystemExit(f"Network error calling {url}: {exc.reason}") from exc


if __name__ == "__main__":
    sys.exit(main())
