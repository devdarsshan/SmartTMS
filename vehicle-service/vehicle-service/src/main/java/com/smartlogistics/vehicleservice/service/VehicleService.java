package com.smartlogistics.vehicleservice.service;

import com.smartlogistics.vehicleservice.enums.VehicleStatus;
import com.smartlogistics.vehicleservice.enums.VehicleType;
import com.smartlogistics.vehicleservice.exceptions.*;
import com.smartlogistics.vehicleservice.dto.*;
import com.smartlogistics.vehicleservice.entity.Vehicle;
import com.smartlogistics.vehicleservice.repo.VehicleRepositiory;
import com.smartlogistics.vehicleservice.spec.VehicleSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehicleService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepositiory vehicleRepositiory;
    private final UserServiceClient userServiceClient;

    public VehicleService(VehicleRepositiory vehicleRepositiory, UserServiceClient userServiceClient) {
        this.vehicleRepositiory = vehicleRepositiory;
        this.userServiceClient = userServiceClient;
    }

    public void createVehicle(CreateVehicleRequest request) {
        logger.info("Entering CreateVehicle");
        if(vehicleRepositiory.findByVehicleCode(request.getVehicleCode()).isPresent()) {
            logger.info("Vehicle already exists");
            throw new VehicleExistsException("Vehicle code already exists");
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleName(request.getVehicleName());
        vehicle.setVehicleCode(request.getVehicleCode());
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setRegisteredCity(request.getRegisteredCity());
        vehicleRepositiory.save(vehicle);
        logger.info("Vehicle created");
    }

    public Vehicle updateVehicle(Vehicle vehicle) {
        logger.info("Entering UpdateVehicle");
        Vehicle existingVehicle = vehicleRepositiory.findByVehicleCode(vehicle.getVehicleCode()).orElse(null);
        if(existingVehicle == null) {
            throw new VehicleDoesNotExistException("Vehicle with code " + vehicle.getVehicleCode() + " does not exist");
        }
        if(existingVehicle.getVehicleId() != vehicle.getVehicleId() && existingVehicle.getVehicleCode().equals(vehicle.getVehicleCode())) {
            throw new VehicleExistsException("Another Vehicle with code " + vehicle.getVehicleCode() + " exist already!");
        }
        Vehicle updatedVehicle = vehicleRepositiory.save(vehicle);
        logger.info("Vehicle updated");
        return updatedVehicle;
    }

    public ResponseEntity<List<Vehicle>> findIdleVehicle(VehicleType vehicleType, VehicleStatus vehicleStatus, String registeredCity) {
        logger.info("Entering findIdleVehicle with status " + vehicleStatus + " and vehicleType " + vehicleType);
        Specification<Vehicle> spec = Specification
                .where(VehicleSpecification.hasVehicleType(vehicleType))
                .and(VehicleSpecification.hasVehicleStatus(vehicleStatus))
                .and(VehicleSpecification.hasRegisteredCity(registeredCity));
        List<Vehicle> vehicles = vehicleRepositiory.findAll(spec);
        logger.info("Exiting findIdleVehicleOnType with " + vehicles.size() + " vehicles found");
        return ResponseEntity.ok(vehicles);
    }

    public VehicleWithDriverDTO assignDriverToVehicle(Long vehicleId, Long driverId) {
        logger.info("Entering assignDriverToVehicle with vehicleId: {} and driverId: {}", vehicleId, driverId);
        
        Vehicle vehicle = vehicleRepositiory.findById(vehicleId)
                .orElseThrow(() -> new VehicleDoesNotExistException("Vehicle with id " + vehicleId + " not found"));
        
        if (vehicle.getVehicleStatus() == VehicleStatus.OCCUPIED) {
            throw new InvalidOperationException("Vehicle is already occupied. Cannot assign another driver.");
        }
        
        DriverInfoDTO driverInfo = userServiceClient.getDriverInfo(driverId);
        if (driverInfo == null) {
            throw new DriverNotFoundException("Driver with id " + driverId + " not found");
        }
        
        vehicle.setDriverId(driverId);
        vehicle.setVehicleStatus(VehicleStatus.OCCUPIED);
        Vehicle updatedVehicle = vehicleRepositiory.save(vehicle);
        
        logger.info("Driver assigned to vehicle successfully");
        return mapToVehicleWithDriverDTO(updatedVehicle, driverInfo);
    }

    public VehicleWithDriverDTO unassignDriverFromVehicle(Long vehicleId) {
        logger.info("Entering unassignDriverFromVehicle with vehicleId: {}", vehicleId);
        
        Vehicle vehicle = vehicleRepositiory.findById(vehicleId)
                .orElseThrow(() -> new VehicleDoesNotExistException("Vehicle with id " + vehicleId + " not found"));
        
        if (vehicle.getDriverId() == null) {
            throw new InvalidOperationException("No driver is assigned to this vehicle");
        }
        
        Long driverId = vehicle.getDriverId();
        vehicle.setDriverId(null);
        vehicle.setVehicleStatus(VehicleStatus.IDLE);
        Vehicle updatedVehicle = vehicleRepositiory.save(vehicle);
        
        logger.info("Driver unassigned from vehicle successfully");
        
        DriverInfoDTO driverInfo = userServiceClient.getDriverInfo(driverId);
        return mapToVehicleWithDriverDTO(updatedVehicle, driverInfo);
    }

    public VehicleWithDriverDTO getVehicleWithDriver(Long vehicleId) {
        logger.info("Entering getVehicleWithDriver with vehicleId: {}", vehicleId);
        
        Vehicle vehicle = vehicleRepositiory.findById(vehicleId)
                .orElseThrow(() -> new VehicleDoesNotExistException("Vehicle with id " + vehicleId + " not found"));
        
        DriverInfoDTO driverInfo = null;
        if (vehicle.getDriverId() != null) {
            driverInfo = userServiceClient.getDriverInfo(vehicle.getDriverId());
        }
        
        logger.info("Vehicle with driver retrieved successfully");
        return mapToVehicleWithDriverDTO(vehicle, driverInfo);
    }

    public ResponseEntity<List<VehicleWithDriverDTO>> getAllVehiclesWithDrivers() {
        logger.info("Entering getAllVehiclesWithDrivers");
        List<Vehicle> vehicles = vehicleRepositiory.findAll();
        List<VehicleWithDriverDTO> vehiclesWithDrivers = vehicles.stream()
                .map(vehicle -> {
                    DriverInfoDTO driverInfo = null;
                    if (vehicle.getDriverId() != null) {
                        driverInfo = userServiceClient.getDriverInfo(vehicle.getDriverId());
                    }
                    return mapToVehicleWithDriverDTO(vehicle, driverInfo);
                })
                .collect(Collectors.toList());
        
        logger.info("Retrieved {} vehicles with driver information", vehiclesWithDrivers.size());
        return ResponseEntity.ok(vehiclesWithDrivers);
    }

    public ResponseEntity<List<VehicleWithDriverDTO>> getOccupiedVehiclesWithDrivers() {
        logger.info("Entering getOccupiedVehiclesWithDrivers");
        List<Vehicle> occupiedVehicles = vehicleRepositiory.findAll()
                .stream()
                .filter(vehicle -> VehicleStatus.OCCUPIED.equals(vehicle.getVehicleStatus()))
                .collect(Collectors.toList());
        
        List<VehicleWithDriverDTO> vehiclesWithDrivers = occupiedVehicles.stream()
                .map(vehicle -> {
                    DriverInfoDTO driverInfo = userServiceClient.getDriverInfo(vehicle.getDriverId());
                    return mapToVehicleWithDriverDTO(vehicle, driverInfo);
                })
                .collect(Collectors.toList());
        
        logger.info("Retrieved {} occupied vehicles with driver information", vehiclesWithDrivers.size());
        return ResponseEntity.ok(vehiclesWithDrivers);
    }

    public VehicleWithDriverDTO getDriverCurrentVehicle(Long driverId) {
        logger.info("Entering getDriverCurrentVehicle with driverId: {}", driverId);
        
        DriverInfoDTO driverInfo = userServiceClient.getDriverInfo(driverId);
        if (driverInfo == null) {
            throw new DriverNotFoundException("Driver with id " + driverId + " not found");
        }
        
        List<Vehicle> vehicles = vehicleRepositiory.findAll()
                .stream()
                .filter(vehicle -> driverId.equals(vehicle.getDriverId()))
                .collect(Collectors.toList());
        
        if (vehicles.isEmpty()) {
            throw new InvalidOperationException("Driver with id " + driverId + " is not assigned to any vehicle");
        }
        
        Vehicle vehicle = vehicles.get(0);
        logger.info("Current vehicle retrieved successfully for driver {}", driverId);
        return mapToVehicleWithDriverDTO(vehicle, driverInfo);
    }

    private VehicleWithDriverDTO mapToVehicleWithDriverDTO(Vehicle vehicle, DriverInfoDTO driverInfo) {
        return new VehicleWithDriverDTO(
                vehicle.getVehicleId(),
                vehicle.getVehicleName(),
                vehicle.getVehicleCode(),
                vehicle.getVehicleType(),
                vehicle.getRegisteredCity(),
                vehicle.getVehicleStatus(),
                driverInfo
        );
    }
}
