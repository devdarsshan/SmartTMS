package com.smartlogistics.analytics.controller;

import com.smartlogistics.analytics.dto.AnalyticsOverviewResponse;
import com.smartlogistics.analytics.dto.AverageAssignmentTimeResponse;
import com.smartlogistics.analytics.dto.DriverAnalyticsResponse;
import com.smartlogistics.analytics.dto.DriverUtilizationSummaryResponse;
import com.smartlogistics.analytics.dto.OrderAnalyticsResponse;
import com.smartlogistics.analytics.dto.RouteVolumeSummaryResponse;
import com.smartlogistics.analytics.dto.VehicleAnalyticsResponse;
import com.smartlogistics.analytics.service.AnalyticsAccessService;
import com.smartlogistics.analytics.service.AnalyticsQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsAccessService analyticsAccessService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService,
                               AnalyticsAccessService analyticsAccessService) {
        this.analyticsQueryService = analyticsQueryService;
        this.analyticsAccessService = analyticsAccessService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> fetchOverview(@RequestHeader("X-User-Id") String userIdHeader) {
        analyticsAccessService.requireOperationalAnalyticsAccess(userIdHeader);
        return ResponseEntity.ok(analyticsQueryService.fetchOverview());
    }

    @GetMapping("/summary/assignment-time")
    public ResponseEntity<AverageAssignmentTimeResponse> fetchAverageAssignmentTime(@RequestHeader("X-User-Id") String userIdHeader) {
        analyticsAccessService.requireOperationalAnalyticsAccess(userIdHeader);
        return ResponseEntity.ok(analyticsQueryService.fetchAverageAssignmentTime());
    }

    @GetMapping("/summary/routes")
    public ResponseEntity<List<RouteVolumeSummaryResponse>> fetchRouteVolumeSummary(@RequestHeader("X-User-Id") String userIdHeader) {
        analyticsAccessService.requireOperationalAnalyticsAccess(userIdHeader);
        return ResponseEntity.ok(analyticsQueryService.fetchRouteVolumeSummary());
    }

    @GetMapping("/summary/drivers")
    public ResponseEntity<List<DriverUtilizationSummaryResponse>> fetchDriverUtilizationSummary(@RequestHeader("X-User-Id") String userIdHeader) {
        analyticsAccessService.requireOperationalAnalyticsAccess(userIdHeader);
        return ResponseEntity.ok(analyticsQueryService.fetchDriverUtilizationSummary());
    }

    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<VehicleAnalyticsResponse> fetchVehicleAnalytics(@PathVariable("vehicleId") Long vehicleId,
                                                                          @RequestHeader("X-User-Id") String userIdHeader) {
        analyticsAccessService.requireOperationalAnalyticsAccess(userIdHeader);
        return ResponseEntity.ok(analyticsQueryService.fetchVehicleAnalytics(vehicleId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<OrderAnalyticsResponse> fetchOrderAnalytics(@PathVariable("orderId") Long orderId,
                                                                      @RequestHeader("X-User-Id") String userIdHeader) {
        analyticsAccessService.requireOperationalAnalyticsAccess(userIdHeader);
        return ResponseEntity.ok(analyticsQueryService.fetchOrderAnalytics(orderId));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<DriverAnalyticsResponse> fetchDriverAnalytics(@PathVariable("driverId") Long driverId,
                                                                        @RequestHeader("X-User-Id") String userIdHeader) {
        analyticsAccessService.requireOperationalAnalyticsAccess(userIdHeader);
        return ResponseEntity.ok(analyticsQueryService.fetchDriverAnalytics(driverId));
    }
}
