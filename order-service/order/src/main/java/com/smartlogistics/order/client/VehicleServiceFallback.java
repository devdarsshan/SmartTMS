package com.smartlogistics.order.client;

import com.smartlogistics.order.dto.VehicleDTO;
import com.smartlogistics.order.enums.VehicleStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class VehicleServiceFallback implements VehicleServiceFeignClient {

    @Override
    public ResponseEntity<List<VehicleDTO>> findVehiclesForOrder(String from, String to) {
        // Fallback: return empty list if vehicle service is down
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Collections.emptyList());
    }

    @Override
    public ResponseEntity<VehicleDTO> assignOrderToVehicle(Long vehicleId, Long orderId) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<VehicleDTO> removeOrderFromVehicle(Long vehicleId, Long orderId) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @Override
    public ResponseEntity<VehicleDTO> updateVehicleStatus(Long vehicleId, VehicleStatus vehicleStatus) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}
