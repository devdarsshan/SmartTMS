package com.smartlogistics.analytics.service;

import com.smartlogistics.analytics.dto.AnalyticsOverviewResponse;
import com.smartlogistics.analytics.dto.AverageAssignmentTimeResponse;
import com.smartlogistics.analytics.dto.DriverAnalyticsResponse;
import com.smartlogistics.analytics.dto.DriverUtilizationSummaryResponse;
import com.smartlogistics.analytics.dto.OrderAnalyticsResponse;
import com.smartlogistics.analytics.dto.RouteVolumeSummaryResponse;
import com.smartlogistics.analytics.dto.VehicleAnalyticsResponse;
import com.smartlogistics.analytics.entity.DriverStats;
import com.smartlogistics.analytics.entity.OrderAnalytics;
import com.smartlogistics.analytics.entity.VehicleStats;
import com.smartlogistics.analytics.exceptions.ResourceNotFoundException;
import com.smartlogistics.analytics.repo.DriverStatsRepository;
import com.smartlogistics.analytics.repo.OrderAnalyticsRepository;
import com.smartlogistics.analytics.repo.RouteVolumeView;
import com.smartlogistics.analytics.repo.VehicleStatsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsQueryService {
    private final VehicleStatsRepository vehicleStatsRepository;
    private final OrderAnalyticsRepository orderAnalyticsRepository;
    private final DriverStatsRepository driverStatsRepository;
    private final long liveWindowSeconds;

    public AnalyticsQueryService(VehicleStatsRepository vehicleStatsRepository,
                                 OrderAnalyticsRepository orderAnalyticsRepository,
                                 DriverStatsRepository driverStatsRepository,
                                 @Value("${app.analytics.live-window-seconds}") long liveWindowSeconds) {
        this.vehicleStatsRepository = vehicleStatsRepository;
        this.orderAnalyticsRepository = orderAnalyticsRepository;
        this.driverStatsRepository = driverStatsRepository;
        this.liveWindowSeconds = liveWindowSeconds;
    }

    public AnalyticsOverviewResponse fetchOverview() {
        AnalyticsOverviewResponse response = new AnalyticsOverviewResponse();
        response.setActiveVehicles(vehicleStatsRepository.countByVehicleStatusIn(List.of("OCCUPIED", "IN_TRANSIT")));
        response.setLiveTrackedVehiclesCount(vehicleStatsRepository.countByLastLocationAtAfter(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(liveWindowSeconds)));
        response.setInTransitOrders(orderAnalyticsRepository.countByInTransitAtNotNullAndDeliveredAtIsNull());

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        response.setDeliveredToday(orderAnalyticsRepository.countByDeliveredAtBetween(
                todayUtc.atStartOfDay(),
                todayUtc.plusDays(1).atStartOfDay()
        ));

        Double averageDeliveryTimeSeconds = orderAnalyticsRepository.averageDeliveryDurationSeconds();
        response.setAverageDeliveryTimeSeconds(averageDeliveryTimeSeconds == null ? 0D : averageDeliveryTimeSeconds);
        return response;
    }

    public AverageAssignmentTimeResponse fetchAverageAssignmentTime() {
        AverageAssignmentTimeResponse response = new AverageAssignmentTimeResponse();
        Double averageAssignmentTimeSeconds = orderAnalyticsRepository.averageAssignmentDurationSeconds();
        response.setAverageAssignmentTimeSeconds(averageAssignmentTimeSeconds == null ? 0D : averageAssignmentTimeSeconds);
        return response;
    }

    public VehicleAnalyticsResponse fetchVehicleAnalytics(Long vehicleId) {
        VehicleStats vehicleStats = vehicleStatsRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle analytics not found for vehicle " + vehicleId));

        VehicleAnalyticsResponse response = new VehicleAnalyticsResponse();
        response.setVehicleId(vehicleStats.getVehicleId());
        response.setTotalDistanceKm(vehicleStats.getTotalDistanceKm() == null ? 0D : vehicleStats.getTotalDistanceKm());
        response.setLastLat(vehicleStats.getLastLat());
        response.setLastLng(vehicleStats.getLastLng());
        response.setLastLocationAt(toIso(vehicleStats.getLastLocationAt()));
        response.setActiveOrderCount(vehicleStats.getActiveOrderCount() == null ? 0 : vehicleStats.getActiveOrderCount());
        response.setCurrentAnalyticsStatus(vehicleStats.getVehicleStatus());
        return response;
    }

    public OrderAnalyticsResponse fetchOrderAnalytics(Long orderId) {
        OrderAnalytics orderAnalytics = orderAnalyticsRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order analytics not found for order " + orderId));

        OrderAnalyticsResponse response = new OrderAnalyticsResponse();
        response.setOrderId(orderAnalytics.getOrderId());
        response.setOrderType(orderAnalytics.getOrderType());
        response.setCreatedAt(toIso(orderAnalytics.getCreatedAt()));
        response.setAssignedAt(toIso(orderAnalytics.getAssignedAt()));
        response.setInTransitAt(toIso(orderAnalytics.getInTransitAt()));
        response.setDeliveredAt(toIso(orderAnalytics.getDeliveredAt()));
        response.setAssignmentDurationSeconds(orderAnalytics.getAssignmentDurationSeconds());
        response.setDeliveryDurationSeconds(orderAnalytics.getDeliveryDurationSeconds());
        response.setVehicleId(orderAnalytics.getVehicleId());
        return response;
    }

    public DriverAnalyticsResponse fetchDriverAnalytics(Long driverId) {
        DriverStats driverStats = driverStatsRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver analytics not found for driver " + driverId));

        DriverAnalyticsResponse response = new DriverAnalyticsResponse();
        response.setDriverId(driverStats.getDriverId());
        response.setCurrentVehicleId(driverStats.getCurrentVehicleId());
        response.setActiveWorkload(driverStats.getActiveOrderCount() == null ? 0 : driverStats.getActiveOrderCount());
        response.setDeliveredOrderCount(driverStats.getDeliveredOrderCount() == null ? 0L : driverStats.getDeliveredOrderCount());
        response.setTotalTrackedDistanceKm(driverStats.getTotalDistanceKm() == null ? 0D : driverStats.getTotalDistanceKm());
        return response;
    }

    public List<RouteVolumeSummaryResponse> fetchRouteVolumeSummary() {
        return orderAnalyticsRepository.fetchRouteVolumes().stream()
                .map(this::toRouteVolumeSummary)
                .collect(Collectors.toList());
    }

    public List<DriverUtilizationSummaryResponse> fetchDriverUtilizationSummary() {
        return driverStatsRepository.findAllByOrderByDriverIdAsc().stream()
                .map(this::toDriverUtilizationSummary)
                .collect(Collectors.toList());
    }

    private String toIso(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toInstant().toString();
    }

    private RouteVolumeSummaryResponse toRouteVolumeSummary(RouteVolumeView view) {
        RouteVolumeSummaryResponse response = new RouteVolumeSummaryResponse();
        response.setFromCity(view.getFromCity());
        response.setToCity(view.getToCity());
        response.setOrderCount(view.getOrderCount());
        return response;
    }

    private DriverUtilizationSummaryResponse toDriverUtilizationSummary(DriverStats driverStats) {
        DriverUtilizationSummaryResponse response = new DriverUtilizationSummaryResponse();
        int activeOrderCount = driverStats.getActiveOrderCount() == null ? 0 : driverStats.getActiveOrderCount();
        response.setDriverId(driverStats.getDriverId());
        response.setCurrentVehicleId(driverStats.getCurrentVehicleId());
        response.setActiveOrderCount(activeOrderCount);
        response.setDeliveredOrderCount(driverStats.getDeliveredOrderCount() == null ? 0L : driverStats.getDeliveredOrderCount());
        response.setTotalDistanceKm(driverStats.getTotalDistanceKm() == null ? 0D : driverStats.getTotalDistanceKm());
        response.setUtilizationStatus(activeOrderCount > 0 ? "BUSY" : "IDLE");
        return response;
    }
}
