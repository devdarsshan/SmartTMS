# Product Requirements Document: Tracking and Analytics Services

## 1. Purpose

This document defines the backend-only product and engineering requirements for:

- `tracking-service`
- `analytics-service`

It is written to fit the current SmartTMS Spring Boot microservice architecture and to integrate with the services that already exist in the repository.

This PRD does not include UI plans. Notification requirements have been moved to a separate PRD.

---

## 2. Current System Context

### 2.1 Existing Services in Repository

- `auth-service`
- `user-service`
- `vehicle-service`
- `order-service`
- `apigateway-service`
- `eureka-server`
- `tracking-service` scaffold only
- `analytics-service` scaffold only

### 2.2 Existing Flows Already Implemented

The current codebase already supports:

- authentication and JWT issuance
- user profile creation and lookup
- vehicle creation and route matching
- driver assignment and unassignment to vehicles
- order creation
- order assignment to vehicles
- order lifecycle transitions:
  - `CREATED`
  - `ASSIGNED`
  - `IN_TRANSIT`
  - `DELIVERED`

### 2.3 Existing Service Integrations Confirmed in Code

- `order-service` calls `user-service` for creator role/profile validation
- `order-service` calls `vehicle-service` to:
  - find route-matching vehicles
  - assign order to vehicle
  - update vehicle status
  - remove order from vehicle
- `vehicle-service` calls `user-service` to validate driver role and fetch driver details

### 2.4 Current Gaps

- `tracking-service` has no APIs, no Redis logic, and no Kafka publishing
- `analytics-service` has no consumers, no storage model, and no APIs
- operational services do not emit domain events today

---


## 4. Goals

### 4.1 Primary Goals

- ingest live vehicle tracking updates
- maintain latest vehicle state in Redis
- publish tracking and operational events to Kafka
- generate derived analytics from tracking and business lifecycle events
- expose backend APIs for tracking and analytics queries

### 4.2 Non-Goals

- any UI, frontend, or dashboard implementation details
- mobile app implementation
- SMS, email, or in-app notification design
- advanced ETA prediction in phase 1

---

## 5. Required System Integration

## 5.1 API Gateway

Current gateway routes already include:

- `/track/**` -> tracking service

Required addition:

- `/analytics/**` -> analytics service

Note:

- the current gateway uses `/track/**`, so this PRD keeps tracking endpoints under `/track`

## 5.2 Eureka

Both services must:

- register with Eureka
- use stable service names
- be discoverable by gateway and peer services

## 5.3 Authentication and Authorization

Both services must:

- trust JWT validation performed by gateway
- enforce role-based access where needed
- use propagated `X-User-Id` for audit and authorization checks

---

## 6. Target Backend Architecture

```text
Driver Client / Internal Caller
        |
        v
   API Gateway
        |
        v
 Tracking Service ----> Redis
        |
        v
      Kafka <---- Order Service / Vehicle Service
        |
        v
  Analytics Service ----> MySQL
```

Core rule:

- all significant operational state changes should emit events so analytics can remain decoupled from operational writes

---

## 7. Tracking Service PRD

## 7.1 Responsibilities

- receive vehicle location updates
- validate vehicle identity
- validate driver-to-vehicle linkage when driver context is provided
- store latest vehicle location in Redis
- publish normalized location events to Kafka
- expose latest location read APIs
- expose order-linked tracking summary APIs

## 7.2 Primary Dependencies

- `vehicle-service`
  - validate vehicle exists
  - verify driver assignment if required
- `order-service`
  - fetch active order/vehicle relationship for order-linked tracking queries
- Redis
  - store latest live location
- Kafka
  - publish location events

## 7.3 APIs

### POST `/track/location`

Purpose:

- ingest live location updates

Request:

```json
{
  "vehicleId": 101,
  "driverId": 12,
  "lat": 12.9716,
  "lng": 77.5946,
  "speed": 42.5,
  "heading": 180,
  "timestamp": 1710000000
}
```

Behavior:

- validate required payload fields
- validate vehicle exists
- validate driver is assigned to vehicle when `driverId` is provided
- write latest location to Redis
- publish `VEHICLE_LOCATION_UPDATED` event to Kafka
- return success only when ingestion is accepted

### GET `/track/vehicle/{vehicleId}`

Purpose:

- return latest live location from Redis for a vehicle

### GET `/track/order/{orderId}`

Purpose:

- return order-linked tracking context

Response should include:

- `orderId`
- `orderStatus`
- `vehicleId`
- latest live location if available
- `lastUpdatedAt`

## 7.4 Redis Model

### Key

```text
live:vehicle:{vehicleId}
```

### Value

```json
{
  "vehicleId": 101,
  "driverId": 12,
  "lat": 12.9716,
  "lng": 77.5946,
  "speed": 42.5,
  "heading": 180,
  "timestamp": 1710000000,
  "status": "LIVE"
}
```

### TTL

- 60 seconds in phase 1

## 7.5 Kafka Topic

Recommended topic:

```text
vehicle-location-events
```

Partition key:

```text
vehicleId
```

## 7.6 Tracking Event Contract

```json
{
  "eventId": "uuid",
  "eventType": "VEHICLE_LOCATION_UPDATED",
  "occurredAt": "2026-04-11T15:00:00Z",
  "vehicleId": 101,
  "driverId": 12,
  "orderIds": [501],
  "lat": 12.9716,
  "lng": 77.5946,
  "speed": 42.5,
  "heading": 180,
  "timestamp": 1710000000,
  "source": "tracking-service"
}
```

## 7.7 Failure Handling

- Redis write failure:
  - log failure
  - return degraded error or partial failure response based on implementation choice
- Kafka publish failure:
  - ingestion should fail unless an outbox/retry mechanism is introduced
- invalid vehicle or driver linkage:
  - reject request
- stale Redis data:
  - must not be treated as active transit proof

---

## 8. Analytics Service PRD

## 8.1 Responsibilities

- consume tracking and business events
- compute derived operational metrics
- persist aggregated analytics in MySQL
- expose backend analytics APIs

## 8.2 Input Events

Phase 1 consumers should handle:

- `VEHICLE_LOCATION_UPDATED`
- `ORDER_CREATED`
- `ORDER_ASSIGNED`
- `ORDER_IN_TRANSIT`
- `ORDER_DELIVERED`
- `DRIVER_ASSIGNED_TO_VEHICLE`
- `DRIVER_UNASSIGNED_FROM_VEHICLE`
- `VEHICLE_STATUS_UPDATED`

## 8.3 Phase 1 Metrics

- total distance traveled per vehicle
- last known location per vehicle
- active vehicles count
- in-transit orders count
- delivered orders count
- average assignment time
- average delivery time
- driver utilization summary
- route volume by source and destination

## 8.4 Storage Model

Analytics tables must be separate from operational tables.

### `vehicle_stats`

| column | type |
|--------|------|
| vehicle_id | BIGINT |
| total_distance_km | DOUBLE |
| last_lat | DOUBLE |
| last_lng | DOUBLE |
| last_location_at | TIMESTAMP |
| active_order_count | INT |
| vehicle_status | VARCHAR |
| updated_at | TIMESTAMP |

### `order_analytics`

| column | type |
|--------|------|
| order_id | BIGINT |
| order_type | VARCHAR |
| created_at | TIMESTAMP |
| assigned_at | TIMESTAMP |
| in_transit_at | TIMESTAMP |
| delivered_at | TIMESTAMP |
| assignment_duration_seconds | BIGINT |
| delivery_duration_seconds | BIGINT |
| vehicle_id | BIGINT |
| created_by_user_id | BIGINT |

### `driver_stats`

| column | type |
|--------|------|
| driver_id | BIGINT |
| current_vehicle_id | BIGINT |
| delivered_order_count | BIGINT |
| active_order_count | INT |
| total_distance_km | DOUBLE |
| updated_at | TIMESTAMP |

## 8.5 APIs

### GET `/analytics/overview`

Returns:

- active vehicles
- live tracked vehicles count
- in-transit orders
- delivered today
- average delivery time

### GET `/analytics/vehicle/{vehicleId}`

Returns:

- total distance
- last known coordinates
- last location timestamp
- active order count
- current analytics status

### GET `/analytics/order/{orderId}`

Returns:

- order type
- lifecycle timestamps
- assignment duration
- delivery duration
- linked vehicle

### GET `/analytics/driver/{driverId}`

Returns:

- current vehicle
- active workload
- delivered order count
- total tracked distance

## 8.6 Distance Calculation

For each location event:

- load previous known location for that vehicle
- compute delta distance
- add to cumulative total distance

Phase 1 recommendation:

- use Haversine distance
- ignore invalid location jumps using configurable thresholds

## 8.7 Analytics Processing Rules

- consumers must be idempotent
- duplicate Kafka events must not corrupt aggregate data
- analytics must be eventually consistent, not transactionally coupled to operational writes

---

## 9. Required Changes in Existing Services

## 9.1 Order Service

Required changes:

- add `orderType`
- emit lifecycle events:
  - `ORDER_CREATED`
  - `ORDER_ASSIGNED`
  - `ORDER_IN_TRANSIT`
  - `ORDER_DELIVERED`

### Recommended `ORDER_CREATED` Event

```json
{
  "eventId": "uuid",
  "eventType": "ORDER_CREATED",
  "occurredAt": "2026-04-11T15:00:00Z",
  "orderId": 501,
  "orderType": "CUSTOMER_ORDER",
  "createdByUserId": 44,
  "createdByRole": "USER",
  "from": "Bangalore",
  "to": "Chennai",
  "status": "CREATED"
}
```

## 9.2 Vehicle Service

Required changes:

- emit vehicle domain events:
  - `DRIVER_ASSIGNED_TO_VEHICLE`
  - `DRIVER_UNASSIGNED_FROM_VEHICLE`
  - `VEHICLE_STATUS_UPDATED`
- expose enough data for tracking-service to validate vehicle-driver linkage

### Recommended `VEHICLE_STATUS_UPDATED` Event

```json
{
  "eventId": "uuid",
  "eventType": "VEHICLE_STATUS_UPDATED",
  "occurredAt": "2026-04-11T15:30:00Z",
  "vehicleId": 101,
  "vehicleStatus": "IN_TRANSIT"
}
```

---

## 10. Data Ownership

- `order-service` remains source of truth for orders
- `vehicle-service` remains source of truth for vehicles, order assignment on vehicles, and driver assignment
- `user-service` remains source of truth for user profiles and roles
- `tracking-service` owns latest live tracking state and location event publication
- `analytics-service` owns derived aggregates only

No analytics table may become the operational source of truth.

---

## 11. Non-Functional Requirements

### 11.1 Reliability

- tracking ingestion must tolerate high write rates
- analytics consumers must be idempotent
- Kafka consumer failures must not silently lose offsets

### 11.2 Performance

- target at least 333 tracking events/sec in phase 1
- Redis reads for latest location must remain low latency

### 11.3 Security

- all external APIs must be protected by JWT
- service logs must include actor and correlation information when available
- sensitive user data should not be over-published in Kafka events

### 11.4 Observability

- every event should include `eventId`, `eventType`, `occurredAt`, and `source`
- failures in Redis, Kafka, and analytics consumers must be logged with enough context to replay

---

## 12. Phased Delivery

## Phase 1

- add order type support in `order-service`
- update order creation authorization rules
- implement tracking ingestion and latest-location APIs
- integrate Redis in `tracking-service`
- integrate Kafka publishers in `order-service`, `vehicle-service`, and `tracking-service`
- implement analytics consumers and analytics query APIs

## Phase 2

- stale tracking detection
- route deviation and delay analytics
- richer operational aggregations

---

## 13. Final Summary

This PRD keeps the scope backend-only and focuses only on `tracking-service` and `analytics-service`.

It aligns with the current SmartTMS codebase by:

- extending current `order-service` and `vehicle-service` instead of replacing them
- using Redis for live tracking state
- using Kafka for decoupled event-driven analytics
- keeping analytics as a derived read model rather than operational truth

The target result is a Spring Boot microservice implementation that supports:

- live vehicle tracking
- event-driven analytics over order, vehicle, driver, and tracking flows
