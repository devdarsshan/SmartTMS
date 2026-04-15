package com.smartlogistics.analytics.dto;

public class VehicleAnalyticsResponse {
    private Long vehicleId;
    private double totalDistanceKm;
    private Double lastLat;
    private Double lastLng;
    private String lastLocationAt;
    private int activeOrderCount;
    private String currentAnalyticsStatus;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public Double getLastLat() {
        return lastLat;
    }

    public void setLastLat(Double lastLat) {
        this.lastLat = lastLat;
    }

    public Double getLastLng() {
        return lastLng;
    }

    public void setLastLng(Double lastLng) {
        this.lastLng = lastLng;
    }

    public String getLastLocationAt() {
        return lastLocationAt;
    }

    public void setLastLocationAt(String lastLocationAt) {
        this.lastLocationAt = lastLocationAt;
    }

    public int getActiveOrderCount() {
        return activeOrderCount;
    }

    public void setActiveOrderCount(int activeOrderCount) {
        this.activeOrderCount = activeOrderCount;
    }

    public String getCurrentAnalyticsStatus() {
        return currentAnalyticsStatus;
    }

    public void setCurrentAnalyticsStatus(String currentAnalyticsStatus) {
        this.currentAnalyticsStatus = currentAnalyticsStatus;
    }
}
