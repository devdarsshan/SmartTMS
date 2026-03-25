package com.smartlogistics.vehicleservice.controller;

import com.smartlogistics.vehicleservice.dto.*;
import com.smartlogistics.vehicleservice.entity.Vehicle;
import com.smartlogistics.vehicleservice.enums.VehicleStatus;
import com.smartlogistics.vehicleservice.enums.VehicleType;
import com.smartlogistics.vehicleservice.service.VehicleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicle")
public class VehicleController {
    private static final Logger logger = LoggerFactory.getLogger(VehicleController.class);

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping("/create")
    public ResponseEntity<Void> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        logger.info("Creating vehicle with code: {}", request.getVehicleCode());
        vehicleService.createVehicle(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update")
    public ResponseEntity<Vehicle> updateVehicle(@Valid @RequestBody Vehicle vehicle) {
        logger.info("Updating vehicle with id: {}", vehicle.getVehicleId());
        Vehicle updatedVehicle = vehicleService.updateVehicle(vehicle);
        return ResponseEntity.ok(updatedVehicle);
    }

    @GetMapping("/findIdle")
    public ResponseEntity<List<Vehicle>> findIdleVehicles(
            @RequestParam VehicleType vehicleType,
            @RequestParam VehicleStatus vehicleStatus,
            @RequestParam String registeredCity) {
        logger.info("Finding idle vehicles with type: {}, status: {}, city: {}", vehicleType, vehicleStatus, registeredCity);
        return vehicleService.findIdleVehicle(vehicleType, vehicleStatus, registeredCity);
    }

    @PostMapping("/assignDriver/{vehicleId}/{driverId}")
    public ResponseEntity<VehicleWithDriverDTO> assignDriver(
            @PathVariable Long vehicleId,
            @PathVariable Long driverId) {
        logger.info("Assigning driver {} to vehicle {}", driverId, vehicleId);
        VehicleWithDriverDTO result = vehicleService.assignDriverToVehicle(vehicleId, driverId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/unassignDriver/{vehicleId}")
    public ResponseEntity<VehicleWithDriverDTO> unassignDriver(@PathVariable Long vehicleId) {
        logger.info("Unassigning driver from vehicle {}", vehicleId);
        VehicleWithDriverDTO result = vehicleService.unassignDriverFromVehicle(vehicleId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleWithDriverDTO> getVehicleWithDriver(@PathVariable Long vehicleId) {
        logger.info("Fetching vehicle {} with driver info", vehicleId);
        VehicleWithDriverDTO result = vehicleService.getVehicleWithDriver(vehicleId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all/withDrivers")
    public ResponseEntity<List<VehicleWithDriverDTO>> getAllVehiclesWithDrivers() {
        logger.info("Fetching all vehicles with driver information");
        return vehicleService.getAllVehiclesWithDrivers();
    }

    @GetMapping("/occupied/withDrivers")
    public ResponseEntity<List<VehicleWithDriverDTO>> getOccupiedVehiclesWithDrivers() {
        logger.info("Fetching all occupied vehicles with driver information");
        return vehicleService.getOccupiedVehiclesWithDrivers();
    }

    @GetMapping("/driver/{driverId}/currentVehicle")
    public ResponseEntity<VehicleWithDriverDTO> getDriverCurrentVehicle(@PathVariable Long driverId) {
        logger.info("Fetching current vehicle for driver {}", driverId);
        VehicleWithDriverDTO result = vehicleService.getDriverCurrentVehicle(driverId);
        return ResponseEntity.ok(result);
    }
}


