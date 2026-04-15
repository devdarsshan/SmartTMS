package com.smartlogistics.analytics.dto;

public class DriverAnalyticsResponse {
    private Long driverId;
    private Long currentVehicleId;
    private int activeWorkload;
    private long deliveredOrderCount;
    private double totalTrackedDistanceKm;

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

    public int getActiveWorkload() {
        return activeWorkload;
    }

    public void setActiveWorkload(int activeWorkload) {
        this.activeWorkload = activeWorkload;
    }

    public long getDeliveredOrderCount() {
        return deliveredOrderCount;
    }

    public void setDeliveredOrderCount(long deliveredOrderCount) {
        this.deliveredOrderCount = deliveredOrderCount;
    }

    public double getTotalTrackedDistanceKm() {
        return totalTrackedDistanceKm;
    }

    public void setTotalTrackedDistanceKm(double totalTrackedDistanceKm) {
        this.totalTrackedDistanceKm = totalTrackedDistanceKm;
    }
}
