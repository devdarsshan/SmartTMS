package com.smartlogistics.tracking.client;

import com.smartlogistics.tracking.dto.VehicleWithDriverDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "vehicle-service")
public interface VehicleServiceFeignClient {

    @GetMapping("/vehicle/{vehicleId}")
    ResponseEntity<VehicleWithDriverDTO> fetchVehicle(@PathVariable("vehicleId") Long vehicleId);

    @GetMapping("/vehicle/all/withDrivers")
    ResponseEntity<List<VehicleWithDriverDTO>> fetchAllVehiclesWithDrivers();
}
