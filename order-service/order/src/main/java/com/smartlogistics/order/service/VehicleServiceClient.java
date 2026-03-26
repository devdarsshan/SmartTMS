package com.smartlogistics.order.service;

import com.smartlogistics.order.client.VehicleServiceFeignClient;
import com.smartlogistics.order.dto.VehicleDTO;
import com.smartlogistics.order.enums.VehicleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class VehicleServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(VehicleServiceClient.class);

    private final VehicleServiceFeignClient vehicleServiceFeignClient;

    public VehicleServiceClient(VehicleServiceFeignClient vehicleServiceFeignClient) {
        this.vehicleServiceFeignClient = vehicleServiceFeignClient;
    }

    public List<VehicleDTO> findVehiclesForOrder(String from, String to) {
        logger.info("Fetching vehicles for route from {} to {}", from, to);
        try {
            ResponseEntity<List<VehicleDTO>> response = vehicleServiceFeignClient.findVehiclesForOrder(from, to);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching vehicles from vehicle-service", e);
            throw new RuntimeException("Failed to communicate with vehicle-service", e);
        }
    }

    public VehicleDTO assignOrderToVehicle(Long vehicleId, Long orderId) {
        logger.info("Assigning order {} to vehicle {}", orderId, vehicleId);
        try {
            ResponseEntity<VehicleDTO> response = vehicleServiceFeignClient.assignOrderToVehicle(vehicleId, orderId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Vehicle-service returned an empty response while assigning order");
        } catch (Exception e) {
            logger.error("Error assigning order {} to vehicle {}", orderId, vehicleId, e);
            throw new RuntimeException("Failed to communicate with vehicle-service", e);
        }
    }

    public VehicleDTO removeOrderFromVehicle(Long vehicleId, Long orderId) {
        logger.info("Removing order {} from vehicle {}", orderId, vehicleId);
        try {
            ResponseEntity<VehicleDTO> response = vehicleServiceFeignClient.removeOrderFromVehicle(vehicleId, orderId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Vehicle-service returned an empty response while removing order");
        } catch (Exception e) {
            logger.error("Error removing order {} from vehicle {}", orderId, vehicleId, e);
            throw new RuntimeException("Failed to communicate with vehicle-service", e);
        }
    }

    public VehicleDTO updateVehicleStatus(Long vehicleId, VehicleStatus vehicleStatus) {
        logger.info("Updating vehicle {} status to {}", vehicleId, vehicleStatus);
        try {
            ResponseEntity<VehicleDTO> response = vehicleServiceFeignClient.updateVehicleStatus(vehicleId, vehicleStatus);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Vehicle-service returned an empty response while updating status");
        } catch (Exception e) {
            logger.error("Error updating vehicle status for vehicle {}", vehicleId, e);
            throw new RuntimeException("Failed to communicate with vehicle-service", e);
        }
    }
}
