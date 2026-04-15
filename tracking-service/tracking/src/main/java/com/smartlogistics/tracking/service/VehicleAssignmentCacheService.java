package com.smartlogistics.tracking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlogistics.tracking.client.VehicleServiceFeignClient;
import com.smartlogistics.tracking.dto.VehicleWithDriverDTO;
import com.smartlogistics.tracking.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VehicleAssignmentCacheService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleAssignmentCacheService.class);

    private final VehicleServiceFeignClient vehicleServiceFeignClient;
    private final ObjectMapper objectMapper;
    private final Map<Long, VehicleAssignmentSnapshot> cache = new ConcurrentHashMap<>();

    public VehicleAssignmentCacheService(VehicleServiceFeignClient vehicleServiceFeignClient,
                                         ObjectMapper objectMapper) {
        this.vehicleServiceFeignClient = vehicleServiceFeignClient;
        this.objectMapper = objectMapper;
    }

    public VehicleAssignmentSnapshot resolveVehicle(Long vehicleId) {
        VehicleAssignmentSnapshot snapshot = cache.get(vehicleId);
        if (snapshot != null) {
            return snapshot;
        }
        return refreshVehicle(vehicleId);
    }

    public void updateFromVehicleEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long vehicleId = readLong(node, "vehicleId");
            if (vehicleId == null) {
                return;
            }

            VehicleAssignmentSnapshot snapshot = new VehicleAssignmentSnapshot();
            snapshot.setVehicleId(vehicleId);
            snapshot.setDriverId(readLong(node, "driverId"));
            snapshot.setVehicleStatus(readText(node, "vehicleStatus"));
            snapshot.setOrderIds(readOrderIds(node));
            snapshot.setUpdatedAt(Instant.now());
            cache.put(vehicleId, snapshot);
        } catch (JsonProcessingException ex) {
            logger.error("Failed to process vehicle event payload for tracking cache", ex);
        }
    }

    @Scheduled(fixedDelayString = "${app.tracking.cache.sync-interval-ms}")
    public void syncAllVehicles() {
        try {
            ResponseEntity<List<VehicleWithDriverDTO>> response = vehicleServiceFeignClient.fetchAllVehiclesWithDrivers();
            List<VehicleWithDriverDTO> vehicles = response.getBody();
            if (vehicles == null) {
                return;
            }
            for (VehicleWithDriverDTO vehicle : vehicles) {
                cache.put(vehicle.getVehicleId(), toSnapshot(vehicle));
            }
        } catch (RuntimeException ex) {
            logger.error("Failed to synchronize tracking vehicle cache", ex);
        }
    }

    private VehicleAssignmentSnapshot refreshVehicle(Long vehicleId) {
        ResponseEntity<VehicleWithDriverDTO> response = vehicleServiceFeignClient.fetchVehicle(vehicleId);
        VehicleWithDriverDTO body = response.getBody();
        if (body == null) {
            throw new ResourceNotFoundException("Vehicle with id " + vehicleId + " not found");
        }

        VehicleAssignmentSnapshot snapshot = toSnapshot(body);
        cache.put(vehicleId, snapshot);
        return snapshot;
    }

    private VehicleAssignmentSnapshot toSnapshot(VehicleWithDriverDTO vehicle) {
        VehicleAssignmentSnapshot snapshot = new VehicleAssignmentSnapshot();
        snapshot.setVehicleId(vehicle.getVehicleId());
        snapshot.setDriverId(vehicle.getDriver() == null ? null : vehicle.getDriver().getAuthUserId());
        snapshot.setVehicleStatus(vehicle.getVehicleStatus());
        snapshot.setOrderIds(vehicle.getOrderIds() == null ? new ArrayList<>() : new ArrayList<>(vehicle.getOrderIds()));
        snapshot.setUpdatedAt(Instant.now());
        return snapshot;
    }

    private Long readLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<Long> readOrderIds(JsonNode node) {
        JsonNode value = node.get("orderIds");
        if (value == null || value.isNull()) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(value, new TypeReference<List<Long>>() {});
    }
}
