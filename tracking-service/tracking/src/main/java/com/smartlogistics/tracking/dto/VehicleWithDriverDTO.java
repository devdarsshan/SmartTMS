package com.smartlogistics.tracking.dto;

import java.util.ArrayList;
import java.util.List;

public class VehicleWithDriverDTO {
    private Long vehicleId;
    private String vehicleName;
    private String vehicleCode;
    private String vehicleType;
    private String from;
    private String to;
    private List<String> through = new ArrayList<>();
    private List<Long> orderIds = new ArrayList<>();
    private String vehicleStatus;
    private DriverInfoDTO driver;

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

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
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

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }

    public String getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(String vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public DriverInfoDTO getDriver() {
        return driver;
    }

    public void setDriver(DriverInfoDTO driver) {
        this.driver = driver;
    }
}
