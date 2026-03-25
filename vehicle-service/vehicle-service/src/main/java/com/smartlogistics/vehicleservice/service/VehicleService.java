package com.smartlogistics.vehicleservice.service;

import com.smartlogistics.vehicleservice.enums.VehicleStatus;
import com.smartlogistics.vehicleservice.enums.VehicleType;
import com.smartlogistics.vehicleservice.exceptions.VehicleExistsException;
import com.smartlogistics.vehicleservice.dto.CreateVehicleRequest;
import com.smartlogistics.vehicleservice.entity.Vehicle;
import com.smartlogistics.vehicleservice.exceptions.VehicleDoesNotExistException;
import com.smartlogistics.vehicleservice.repo.VehicleRepositiory;
import com.smartlogistics.vehicleservice.spec.VehicleSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {
    private static final Logger logger = LoggerFactory.getLogger(VehicleService.class);

    private VehicleRepositiory vehicleRepositiory;

    public VehicleService(VehicleRepositiory vehicleRepositiory) {
        this.vehicleRepositiory = vehicleRepositiory;
    }

    public void createVehicle(CreateVehicleRequest request) {
        logger.info("Entering CreateVehicle");
        if(vehicleRepositiory.findByVehicleCode(request.getVehicleCode()).isPresent()) {
            logger.info("Vehicle already exists");
            throw new VehicleExistsException("Vehicle code already exists");
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleName(request.getVehicleName());
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
        if(existingVehicle != null) {
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


}
