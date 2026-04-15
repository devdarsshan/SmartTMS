package com.smartlogistics.tracking.dto;

public class OrderTrackingSummaryResponse {
    private Long orderId;
    private String orderStatus;
    private Long vehicleId;
    private LiveVehicleLocation latestLocation;
    private String lastUpdatedAt;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LiveVehicleLocation getLatestLocation() {
        return latestLocation;
    }

    public void setLatestLocation(LiveVehicleLocation latestLocation) {
        this.latestLocation = latestLocation;
    }

    public String getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(String lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
