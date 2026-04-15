package com.smartlogistics.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogistics.analytics.entity.DriverStats;
import com.smartlogistics.analytics.entity.OrderAnalytics;
import com.smartlogistics.analytics.entity.ProcessedEvent;
import com.smartlogistics.analytics.entity.VehicleStats;
import com.smartlogistics.analytics.repo.DriverStatsRepository;
import com.smartlogistics.analytics.repo.OrderAnalyticsRepository;
import com.smartlogistics.analytics.repo.ProcessedEventRepository;
import com.smartlogistics.analytics.repo.VehicleStatsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
public class AnalyticsEventProcessor {
    private final ObjectMapper objectMapper;
    private final VehicleStatsRepository vehicleStatsRepository;
    private final OrderAnalyticsRepository orderAnalyticsRepository;
    private final DriverStatsRepository driverStatsRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final double maxLocationJumpKm;

    public AnalyticsEventProcessor(ObjectMapper objectMapper,
                                   VehicleStatsRepository vehicleStatsRepository,
                                   OrderAnalyticsRepository orderAnalyticsRepository,
                                   DriverStatsRepository driverStatsRepository,
                                   ProcessedEventRepository processedEventRepository,
                                   @Value("${app.analytics.location.jump-threshold-km}") double maxLocationJumpKm) {
        this.objectMapper = objectMapper;
        this.vehicleStatsRepository = vehicleStatsRepository;
        this.orderAnalyticsRepository = orderAnalyticsRepository;
        this.driverStatsRepository = driverStatsRepository;
        this.processedEventRepository = processedEventRepository;
        this.maxLocationJumpKm = maxLocationJumpKm;
    }

    @Transactional
    public void processOrderEvent(String payload) {
        JsonNode node = readTree(payload);
        if (!markIfUnprocessed(node)) {
            return;
        }

        String eventType = text(node, "eventType");
        Long orderId = longValue(node, "orderId");
        if (orderId == null) {
            return;
        }

        OrderAnalytics orderAnalytics = orderAnalyticsRepository.findById(orderId).orElseGet(OrderAnalytics::new);
        orderAnalytics.setOrderId(orderId);
        orderAnalytics.setOrderType(text(node, "orderType"));
        orderAnalytics.setVehicleId(longValue(node, "vehicleId"));
        orderAnalytics.setFromCity(text(node, "from"));
        orderAnalytics.setToCity(text(node, "to"));
        orderAnalytics.setCreatedByUserId(longValue(node, "createdByUserId"));

        if ("ORDER_CREATED".equals(eventType)) {
            orderAnalytics.setCreatedAt(parseDateTime(text(node, "createdAt")));
        } else if ("ORDER_ASSIGNED".equals(eventType)) {
            orderAnalytics.setAssignedAt(parseDateTime(text(node, "assignedAt")));
            if (orderAnalytics.getCreatedAt() != null && orderAnalytics.getAssignedAt() != null) {
                orderAnalytics.setAssignmentDurationSeconds(ChronoUnit.SECONDS.between(orderAnalytics.getCreatedAt(), orderAnalytics.getAssignedAt()));
            }
        } else if ("ORDER_IN_TRANSIT".equals(eventType)) {
            orderAnalytics.setInTransitAt(parseDateTime(text(node, "inTransitAt")));
        } else if ("ORDER_DELIVERED".equals(eventType)) {
            orderAnalytics.setDeliveredAt(parseDateTime(text(node, "deliveredAt")));
            if (orderAnalytics.getInTransitAt() != null && orderAnalytics.getDeliveredAt() != null) {
                orderAnalytics.setDeliveryDurationSeconds(ChronoUnit.SECONDS.between(orderAnalytics.getInTransitAt(), orderAnalytics.getDeliveredAt()));
            }
            if (orderAnalytics.getVehicleId() != null) {
                driverStatsRepository.findByCurrentVehicleId(orderAnalytics.getVehicleId()).ifPresent(driverStats -> {
                    long deliveredCount = driverStats.getDeliveredOrderCount() == null ? 0L : driverStats.getDeliveredOrderCount();
                    driverStats.setDeliveredOrderCount(deliveredCount + 1);
                    driverStats.setUpdatedAt(nowUtc());
                    driverStatsRepository.save(driverStats);
                });
            }
        }

        orderAnalyticsRepository.save(orderAnalytics);
    }

    @Transactional
    public void processVehicleEvent(String payload) {
        JsonNode node = readTree(payload);
        if (!markIfUnprocessed(node)) {
            return;
        }

        String eventType = text(node, "eventType");
        Long vehicleId = longValue(node, "vehicleId");
        Long driverId = longValue(node, "driverId");
        int activeOrderCount = node.hasNonNull("orderIds") ? node.get("orderIds").size() : 0;

        if (vehicleId != null) {
            VehicleStats vehicleStats = vehicleStatsRepository.findById(vehicleId).orElseGet(VehicleStats::new);
            vehicleStats.setVehicleId(vehicleId);
            vehicleStats.setVehicleStatus(text(node, "vehicleStatus"));
            vehicleStats.setActiveOrderCount(activeOrderCount);
            vehicleStats.setUpdatedAt(nowUtc());
            vehicleStatsRepository.save(vehicleStats);
        }

        if ("DRIVER_ASSIGNED_TO_VEHICLE".equals(eventType) && driverId != null) {
            DriverStats driverStats = driverStatsRepository.findById(driverId).orElseGet(DriverStats::new);
            driverStats.setDriverId(driverId);
            driverStats.setCurrentVehicleId(vehicleId);
            driverStats.setActiveOrderCount(activeOrderCount);
            driverStats.setUpdatedAt(nowUtc());
            driverStatsRepository.save(driverStats);
            return;
        }

        if ("DRIVER_UNASSIGNED_FROM_VEHICLE".equals(eventType) && driverId != null) {
            DriverStats driverStats = driverStatsRepository.findById(driverId).orElseGet(DriverStats::new);
            driverStats.setDriverId(driverId);
            driverStats.setCurrentVehicleId(null);
            driverStats.setActiveOrderCount(0);
            driverStats.setUpdatedAt(nowUtc());
            driverStatsRepository.save(driverStats);
            return;
        }

        if ("VEHICLE_STATUS_UPDATED".equals(eventType) && driverId != null) {
            DriverStats driverStats = driverStatsRepository.findById(driverId).orElseGet(DriverStats::new);
            driverStats.setDriverId(driverId);
            driverStats.setCurrentVehicleId(vehicleId);
            driverStats.setActiveOrderCount(activeOrderCount);
            driverStats.setUpdatedAt(nowUtc());
            driverStatsRepository.save(driverStats);
        }
    }

    @Transactional
    public void processLocationEvent(String payload) {
        JsonNode node = readTree(payload);
        if (!markIfUnprocessed(node)) {
            return;
        }

        Long vehicleId = longValue(node, "vehicleId");
        Long driverId = longValue(node, "driverId");
        if (vehicleId == null) {
            return;
        }

        double lat = node.get("lat").asDouble();
        double lng = node.get("lng").asDouble();
        LocalDateTime locationAt = parseEpoch(node.get("timestamp").asLong());

        VehicleStats vehicleStats = vehicleStatsRepository.findById(vehicleId).orElseGet(VehicleStats::new);
        vehicleStats.setVehicleId(vehicleId);

        double deltaKm = 0D;
        if (vehicleStats.getLastLat() != null && vehicleStats.getLastLng() != null) {
            deltaKm = haversineKm(vehicleStats.getLastLat(), vehicleStats.getLastLng(), lat, lng);
            if (deltaKm > maxLocationJumpKm) {
                deltaKm = 0D;
            }
        }

        double currentDistance = vehicleStats.getTotalDistanceKm() == null ? 0D : vehicleStats.getTotalDistanceKm();
        vehicleStats.setTotalDistanceKm(currentDistance + deltaKm);
        vehicleStats.setLastLat(lat);
        vehicleStats.setLastLng(lng);
        vehicleStats.setLastLocationAt(locationAt);
        vehicleStats.setUpdatedAt(nowUtc());
        vehicleStatsRepository.save(vehicleStats);

        if (driverId != null) {
            DriverStats driverStats = driverStatsRepository.findById(driverId).orElseGet(DriverStats::new);
            driverStats.setDriverId(driverId);
            driverStats.setCurrentVehicleId(vehicleId);
            double driverDistance = driverStats.getTotalDistanceKm() == null ? 0D : driverStats.getTotalDistanceKm();
            driverStats.setTotalDistanceKm(driverDistance + deltaKm);
            driverStats.setUpdatedAt(nowUtc());
            driverStatsRepository.save(driverStats);
        }
    }

    private boolean markIfUnprocessed(JsonNode node) {
        String eventId = text(node, "eventId");
        if (eventId == null) {
            return false;
        }
        if (processedEventRepository.existsById(eventId)) {
            return false;
        }
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventId(eventId);
        processedEvent.setEventType(text(node, "eventType"));
        processedEvent.setProcessedAt(nowUtc());
        processedEventRepository.save(processedEvent);
        return true;
    }

    private JsonNode readTree(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse analytics event payload", ex);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null ? null : LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
    }

    private LocalDateTime parseEpoch(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
