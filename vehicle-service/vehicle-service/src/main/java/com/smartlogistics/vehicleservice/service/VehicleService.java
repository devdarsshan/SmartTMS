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

import java.util.ArrayList;
import java.util.Comparator;
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
        vehicle.setFrom(request.getFrom());
        vehicle.setTo(request.getTo());
        vehicle.setThrough(new ArrayList<>(request.getThrough()));
        vehicle.setOrderIds(new ArrayList<>());
        vehicleRepositiory.save(vehicle);
        logger.info("Vehicle created");
    }

    public Vehicle updateVehicle(Vehicle vehicle) {
        logger.info("Entering UpdateVehicle");
        Vehicle existingVehicle = vehicleRepositiory.findById(vehicle.getVehicleId()).orElse(null);
        if(existingVehicle == null) {
            throw new VehicleDoesNotExistException("Vehicle with id " + vehicle.getVehicleId() + " does not exist");
        }
        Vehicle duplicateVehicle = vehicleRepositiory.findByVehicleCode(vehicle.getVehicleCode()).orElse(null);
        if(duplicateVehicle != null && !duplicateVehicle.getVehicleId().equals(vehicle.getVehicleId())) {
            throw new VehicleExistsException("Another Vehicle with code " + vehicle.getVehicleCode() + " exist already!");
        }
        if (vehicle.getThrough() == null || vehicle.getThrough().isEmpty()) {
            throw new InvalidOperationException("Vehicle through points cannot be empty");
        }
        if (vehicle.getOrderIds() == null) {
            vehicle.setOrderIds(new ArrayList<>());
        }
        syncVehicleStatusWithOrders(vehicle);
        Vehicle updatedVehicle = vehicleRepositiory.save(vehicle);
        logger.info("Vehicle updated");
        return updatedVehicle;
    }

    public ResponseEntity<List<Vehicle>> findIdleVehicle(VehicleType vehicleType, VehicleStatus vehicleStatus, String from) {
        logger.info("Entering findIdleVehicle with status {} and vehicleType {}", vehicleStatus, vehicleType);
        Specification<Vehicle> spec = Specification
                .where(VehicleSpecification.hasVehicleType(vehicleType))
                .and(VehicleSpecification.hasFrom(from));
        List<Vehicle> vehicles = vehicleRepositiory.findAll(spec)
                .stream()
                .filter(vehicle -> matchesRequestedStatus(vehicle, vehicleStatus))
                .sorted(Comparator.comparing(Vehicle::getVehicleId))
                .collect(Collectors.toList());
        logger.info("Exiting findIdleVehicleOnType with " + vehicles.size() + " vehicles found");
        return ResponseEntity.ok(vehicles);
    }

    public ResponseEntity<List<Vehicle>> findVehiclesForOrder(String from, String to, VehicleType vehicleType) {
        logger.info("Finding vehicles for order from {} to {}", from, to);
        List<Vehicle> vehicles = vehicleRepositiory.findAll()
                .stream()
                .filter(vehicle -> vehicleType == null || vehicle.getVehicleType() == vehicleType)
                .filter(vehicle -> hasText(vehicle.getFrom()) && vehicle.getFrom().equalsIgnoreCase(from))
                .filter(vehicle -> vehicle.getVehicleStatus() == VehicleStatus.IDLE || vehicle.getVehicleStatus() == VehicleStatus.OCCUPIED)
                .filter(vehicle -> routeMatches(vehicle, to))
                .sorted(Comparator
                        .comparing((Vehicle vehicle) -> vehicle.getVehicleStatus() == VehicleStatus.IDLE ? 0 : 1)
                        .thenComparing(this::getOrderCount)
                        .thenComparing(Vehicle::getVehicleId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(vehicles);
    }

    public Vehicle assignOrderToVehicle(Long vehicleId, Long orderId) {
        logger.info("Assigning order {} to vehicle {}", orderId, vehicleId);
        Vehicle vehicle = getVehicleById(vehicleId);
        if (vehicle.getOrderIds() == null) {
            vehicle.setOrderIds(new ArrayList<>());
        }
        if (vehicle.getVehicleStatus() != VehicleStatus.IDLE && vehicle.getVehicleStatus() != VehicleStatus.OCCUPIED) {
            throw new InvalidOperationException("Orders can only be assigned to IDLE or OCCUPIED vehicles");
        }
        if (vehicle.getOrderIds().contains(orderId)) {
            throw new InvalidOperationException("Order " + orderId + " is already assigned to vehicle " + vehicleId);
        }
        vehicle.getOrderIds().add(orderId);
        if (vehicle.getVehicleStatus() == VehicleStatus.IDLE) {
            vehicle.setVehicleStatus(VehicleStatus.OCCUPIED);
        }
        return vehicleRepositiory.save(vehicle);
    }

    public Vehicle removeOrderFromVehicle(Long vehicleId, Long orderId) {
        logger.info("Removing order {} from vehicle {}", orderId, vehicleId);
        Vehicle vehicle = getVehicleById(vehicleId);
        if (vehicle.getOrderIds() == null) {
            vehicle.setOrderIds(new ArrayList<>());
        }
        boolean removed = vehicle.getOrderIds().remove(orderId);
        if (!removed) {
            throw new InvalidOperationException("Order " + orderId + " is not assigned to vehicle " + vehicleId);
        }
        syncVehicleStatusWithOrders(vehicle);
        return vehicleRepositiory.save(vehicle);
    }

    public Vehicle updateVehicleStatus(Long vehicleId, VehicleStatus vehicleStatus) {
        logger.info("Updating vehicle {} status to {}", vehicleId, vehicleStatus);
        Vehicle vehicle = getVehicleById(vehicleId);
        if (vehicleStatus != VehicleStatus.IN_TRANSIT && vehicleStatus != VehicleStatus.IDLE) {
            throw new InvalidOperationException("Vehicle status update only supports IN_TRANSIT or IDLE");
        }
        if (vehicle.getOrderIds() == null) {
            vehicle.setOrderIds(new ArrayList<>());
        }
        if (vehicleStatus == VehicleStatus.IN_TRANSIT && vehicle.getOrderIds().isEmpty()) {
            throw new InvalidOperationException("Vehicle cannot move to IN_TRANSIT without assigned orders");
        }
        if (vehicleStatus == VehicleStatus.IDLE && !vehicle.getOrderIds().isEmpty()) {
            throw new InvalidOperationException("Vehicle cannot move to IDLE while orders are still assigned");
        }
        vehicle.setVehicleStatus(vehicleStatus);
        return vehicleRepositiory.save(vehicle);
    }

    public VehicleWithDriverDTO assignDriverToVehicle(Long vehicleId, Long driverId) {
        logger.info("Entering assignDriverToVehicle with vehicleId: {} and driverId: {}", vehicleId, driverId);

        Vehicle vehicle = getVehicleById(vehicleId);

        if (vehicle.getDriverId() != null) {
            throw new InvalidOperationException("Vehicle already has a driver assigned.");
        }
        if (vehicle.getVehicleStatus() == VehicleStatus.MAINTENANCE || vehicle.getVehicleStatus() == VehicleStatus.DISCARDED) {
            throw new InvalidOperationException("Driver cannot be assigned to a vehicle in " + vehicle.getVehicleStatus() + " state.");
        }

        DriverInfoDTO driverInfo = userServiceClient.getDriverInfo(driverId);
        if (driverInfo == null) {
            throw new DriverNotFoundException("Driver with id " + driverId + " not found");
        }
        if (driverInfo.getUserRole() == null || !driverInfo.getUserRole().equalsIgnoreCase("DRIVER")) {
            throw new InvalidOperationException("User with id " + driverId + " is not eligible to be assigned as a driver");
        }
        vehicleRepositiory.findByDriverId(driverId)
                .filter(existingVehicle -> !existingVehicle.getVehicleId().equals(vehicleId))
                .ifPresent(existingVehicle -> {
                    throw new DriverNotAvailableException(
                            "Driver with id " + driverId + " is already assigned to vehicle " + existingVehicle.getVehicleId()
                    );
                });

        vehicle.setDriverId(driverId);
        Vehicle updatedVehicle = vehicleRepositiory.save(vehicle);

        logger.info("Driver assigned to vehicle successfully");
        return mapToVehicleWithDriverDTO(updatedVehicle, driverInfo);
    }

    public VehicleWithDriverDTO unassignDriverFromVehicle(Long vehicleId) {
        logger.info("Entering unassignDriverFromVehicle with vehicleId: {}", vehicleId);
        
        Vehicle vehicle = getVehicleById(vehicleId);
        
        if (vehicle.getDriverId() == null) {
            throw new InvalidOperationException("No driver is assigned to this vehicle");
        }
        
        Long driverId = vehicle.getDriverId();
        vehicle.setDriverId(null);
        syncVehicleStatusWithOrders(vehicle);
        Vehicle updatedVehicle = vehicleRepositiory.save(vehicle);
        
        logger.info("Driver unassigned from vehicle successfully");
        
        DriverInfoDTO driverInfo = userServiceClient.getDriverInfo(driverId);
        return mapToVehicleWithDriverDTO(updatedVehicle, driverInfo);
    }

    public VehicleWithDriverDTO getVehicleWithDriver(Long vehicleId) {
        logger.info("Entering getVehicleWithDriver with vehicleId: {}", vehicleId);
        
        Vehicle vehicle = getVehicleById(vehicleId);
        
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
                    DriverInfoDTO driverInfo = vehicle.getDriverId() != null
                            ? userServiceClient.getDriverInfo(vehicle.getDriverId())
                            : null;
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
        
        Vehicle vehicle = vehicleRepositiory.findByDriverId(driverId)
                .orElseThrow(() -> new InvalidOperationException("Driver with id " + driverId + " is not assigned to any vehicle"));
        logger.info("Current vehicle retrieved successfully for driver {}", driverId);
        return mapToVehicleWithDriverDTO(vehicle, driverInfo);
    }

    private VehicleWithDriverDTO mapToVehicleWithDriverDTO(Vehicle vehicle, DriverInfoDTO driverInfo) {
        return new VehicleWithDriverDTO(
                vehicle.getVehicleId(),
                vehicle.getVehicleName(),
                vehicle.getVehicleCode(),
                vehicle.getVehicleType(),
                vehicle.getFrom(),
                vehicle.getTo(),
                copyStringList(vehicle.getThrough()),
                copyLongList(vehicle.getOrderIds()),
                vehicle.getVehicleStatus(),
                driverInfo
        );
    }

    private Vehicle getVehicleById(Long vehicleId) {
        return vehicleRepositiory.findById(vehicleId)
                .orElseThrow(() -> new VehicleDoesNotExistException("Vehicle with id " + vehicleId + " not found"));
    }

    private boolean matchesRequestedStatus(Vehicle vehicle, VehicleStatus requestedStatus) {
        if (requestedStatus == null || requestedStatus == VehicleStatus.IDLE || requestedStatus == VehicleStatus.OCCUPIED) {
            return vehicle.getVehicleStatus() == VehicleStatus.IDLE || vehicle.getVehicleStatus() == VehicleStatus.OCCUPIED;
        }
        return vehicle.getVehicleStatus() == requestedStatus;
    }

    private boolean routeMatches(Vehicle vehicle, String destination) {
        if (!hasText(destination)) {
            return false;
        }
        if (hasText(vehicle.getTo()) && vehicle.getTo().equalsIgnoreCase(destination)) {
            return true;
        }
        if (vehicle.getThrough() == null) {
            return false;
        }
        return vehicle.getThrough().stream()
                .filter(this::hasText)
                .anyMatch(throughPoint -> throughPoint.equalsIgnoreCase(destination));
    }

    private void syncVehicleStatusWithOrders(Vehicle vehicle) {
        if (vehicle.getOrderIds() == null) {
            vehicle.setOrderIds(new ArrayList<>());
        }
        if (vehicle.getOrderIds().isEmpty()) {
            if (vehicle.getVehicleStatus() != VehicleStatus.MAINTENANCE && vehicle.getVehicleStatus() != VehicleStatus.DISCARDED) {
                vehicle.setVehicleStatus(VehicleStatus.IDLE);
            }
            return;
        }
        if (vehicle.getVehicleStatus() == null || vehicle.getVehicleStatus() == VehicleStatus.IDLE) {
            vehicle.setVehicleStatus(VehicleStatus.OCCUPIED);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int getOrderCount(Vehicle vehicle) {
        return vehicle.getOrderIds() == null ? 0 : vehicle.getOrderIds().size();
    }

    private List<String> copyStringList(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private List<Long> copyLongList(List<Long> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
