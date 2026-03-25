package com.smartlogistics.vehicleservice.dto;

import com.smartlogistics.vehicleservice.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateVehicleRequest {

    @NotBlank(message = "Vehicle name is required")
    private String vehicleName;
    @NotBlank(message = "Vehicle type is required")
    @Pattern(regexp = "TEMPO|MINITRUCK|TRUCK|PICKUP", message = "Vehicle type must be one of: TEMPO, MINITRUCK, TRUCK, PICKUP")
    private VehicleType vehicleType;
    @NotBlank(message = "Vehicle Code is required")
    private String vehicleCode;
    @NotBlank(message = "Registered city is required")
    @Size(min = 2, max = 50, message = "Registered city must be between 2 and 50 characters")
    private String registeredCity;

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleCode() {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode) {
        this.vehicleCode = vehicleCode;
    }

    public String getRegisteredCity() {
        return registeredCity;
    }

    public void setRegisteredCity(String registeredCity) {
        this.registeredCity = registeredCity;
    }
}
