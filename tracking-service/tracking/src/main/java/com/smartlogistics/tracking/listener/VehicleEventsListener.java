package com.smartlogistics.tracking.listener;

import com.smartlogistics.tracking.service.VehicleAssignmentCacheService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VehicleEventsListener {
    private final VehicleAssignmentCacheService vehicleAssignmentCacheService;

    public VehicleEventsListener(VehicleAssignmentCacheService vehicleAssignmentCacheService) {
        this.vehicleAssignmentCacheService = vehicleAssignmentCacheService;
    }

    @KafkaListener(topics = "${app.kafka.topics.vehicle-events}", groupId = "${spring.application.name}-vehicle-events")
    public void consumeVehicleEvent(String payload) {
        vehicleAssignmentCacheService.updateFromVehicleEvent(payload);
    }
}
