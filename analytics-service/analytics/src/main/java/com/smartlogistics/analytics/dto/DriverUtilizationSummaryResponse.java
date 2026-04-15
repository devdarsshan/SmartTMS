package com.smartlogistics.analytics.dto;

public class DriverUtilizationSummaryResponse {
    private Long driverId;
    private Long currentVehicleId;
    private int activeOrderCount;
    private long deliveredOrderCount;
    private double totalDistanceKm;
    private String utilizationStatus;

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getCurrentVehicleId() {
        return currentVehicleId;
    }

    public void setCurrentVehicleId(Long currentVehicleId) {
        this.currentVehicleId = currentVehicleId;
    }

    public int getActiveOrderCount() {
        return activeOrderCount;
    }

    public void setActiveOrderCount(int activeOrderCount) {
        this.activeOrderCount = activeOrderCount;
    }

    public long getDeliveredOrderCount() {
        return deliveredOrderCount;
    }

    public void setDeliveredOrderCount(long deliveredOrderCount) {
        this.deliveredOrderCount = deliveredOrderCount;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public String getUtilizationStatus() {
        return utilizationStatus;
    }

    public void setUtilizationStatus(String utilizationStatus) {
        this.utilizationStatus = utilizationStatus;
    }
}
