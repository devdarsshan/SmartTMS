package com.smartlogistics.order.client;

import com.smartlogistics.order.dto.VehicleDTO;
import com.smartlogistics.order.enums.VehicleStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "vehicle-service")
public interface VehicleServiceFeignClient {

    @GetMapping("/vehicle/route-match")
    ResponseEntity<List<VehicleDTO>> findVehiclesForOrder(@RequestParam("from") String from,
                                                          @RequestParam("to") String to);

    @PostMapping("/vehicle/{vehicleId}/orders/{orderId}")
    ResponseEntity<VehicleDTO> assignOrderToVehicle(@PathVariable("vehicleId") Long vehicleId,
                                                    @PathVariable("orderId") Long orderId);

    @DeleteMapping("/vehicle/{vehicleId}/orders/{orderId}")
    ResponseEntity<VehicleDTO> removeOrderFromVehicle(@PathVariable("vehicleId") Long vehicleId,
                                                      @PathVariable("orderId") Long orderId);

    @PutMapping("/vehicle/{vehicleId}/status")
    ResponseEntity<VehicleDTO> updateVehicleStatus(@PathVariable("vehicleId") Long vehicleId,
                                                   @RequestParam("vehicleStatus") VehicleStatus vehicleStatus);
}
