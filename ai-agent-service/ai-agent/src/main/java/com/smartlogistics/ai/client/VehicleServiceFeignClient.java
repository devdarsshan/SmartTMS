package com.smartlogistics.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@FeignClient(name = "vehicle-service")
public interface VehicleServiceFeignClient {

    @GetMapping("/vehicle/findIdle")
    ResponseEntity<List<Map<String, Object>>> findIdleVehicles();

    @GetMapping("/vehicle/{vehicleId}")
    ResponseEntity<Map<String, Object>> getVehicleStatus(@PathVariable("vehicleId") Long vehicleId);
    
    @PostMapping("/vehicle/{vehicleId}/orders/{orderId}")
    ResponseEntity<Map<String, Object>> assignOrderToVehicle(@PathVariable("vehicleId") Long vehicleId,
                                                    @PathVariable("orderId") Long orderId);
}
