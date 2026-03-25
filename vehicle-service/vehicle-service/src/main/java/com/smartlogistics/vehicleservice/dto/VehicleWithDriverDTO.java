package com.smartlogistics.vehicleservice.dto;

import com.smartlogistics.vehicleservice.enums.VehicleStatus;
import com.smartlogistics.vehicleservice.enums.VehicleType;

public class VehicleWithDriverDTO {
    private Long vehicleId;
    private String vehicleName;
    private String vehicleCode;
    private VehicleType vehicleType;
    private String registeredCity;
    private VehicleStatus vehicleStatus;
    private DriverInfoDTO driver;

    public VehicleWithDriverDTO() {}

    public VehicleWithDriverDTO(Long vehicleId, String vehicleName, String vehicleCode, VehicleType vehicleType, String registeredCity, VehicleStatus vehicleStatus, DriverInfoDTO driver) {
        this.vehicleId = vehicleId;
        this.vehicleName = vehicleName;
        this.vehicleCode = vehicleCode;
        this.vehicleType = vehicleType;
        this.registeredCity = registeredCity;
        this.vehicleStatus = vehicleStatus;
        this.driver = driver;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getVehicleCode() {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode) {
        this.vehicleCode = vehicleCode;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getRegisteredCity() {
        return registeredCity;
    }

    public void setRegisteredCity(String registeredCity) {
        this.registeredCity = registeredCity;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public DriverInfoDTO getDriver() {
        return driver;
    }

    public void setDriver(DriverInfoDTO driver) {
        this.driver = driver;
    }
}

