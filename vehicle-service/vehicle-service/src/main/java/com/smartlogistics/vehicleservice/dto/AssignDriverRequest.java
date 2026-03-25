package com.smartlogistics.vehicleservice.dto;

public class AssignDriverRequest {
    private Long driverId;
    private Long vehicleId;

    public AssignDriverRequest() {}

    public AssignDriverRequest(Long driverId, Long vehicleId) {
        this.driverId = driverId;
        this.vehicleId = vehicleId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }
}

