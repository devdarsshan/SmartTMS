package com.smartlogistics.tracking.controller;

import com.smartlogistics.tracking.dto.LiveVehicleLocation;
import com.smartlogistics.tracking.dto.LocationUpdateRequest;
import com.smartlogistics.tracking.dto.OrderTrackingSummaryResponse;
import com.smartlogistics.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/track")
public class TrackingController {
    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/location")
    public ResponseEntity<Map<String, String>> ingestLocation(@Valid @RequestBody LocationUpdateRequest request,
                                                              @RequestHeader("X-User-Id") String userIdHeader) {
        trackingService.ingestLocation(request, userIdHeader);
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<LiveVehicleLocation> fetchLatestVehicleLocation(@PathVariable("vehicleId") Long vehicleId) {
        return ResponseEntity.ok(trackingService.fetchLatestVehicleLocation(vehicleId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<OrderTrackingSummaryResponse> fetchOrderTracking(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(trackingService.fetchOrderTracking(orderId));
    }
}
