package com.smartlogistics.vehicleservice.dto;

import com.smartlogistics.vehicleservice.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateVehicleRequest {

    @NotBlank(message = "Vehicle name is required")
    private String vehicleName;
    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;
    @NotBlank(message = "Vehicle Code is required")
    private String vehicleCode;
    @NotBlank(message = "From is required")
    @Size(min = 2, max = 50, message = "From must be between 2 and 50 characters")
    private String from;
    @NotBlank(message = "To is required")
    @Size(min = 2, max = 50, message = "To must be between 2 and 50 characters")
    private String to;
    @NotEmpty(message = "Through points are required")
    private List<@NotBlank(message = "Through point cannot be blank") String> through;

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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<String> getThrough() {
        return through;
    }

    public void setThrough(List<String> through) {
        this.through = through;
    }
}
