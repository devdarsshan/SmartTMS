package com.smartlogistics.analytics.dto;

public class AnalyticsOverviewResponse {
    private long activeVehicles;
    private long liveTrackedVehiclesCount;
    private long inTransitOrders;
    private long deliveredToday;
    private double averageDeliveryTimeSeconds;

    public long getActiveVehicles() {
        return activeVehicles;
    }

    public void setActiveVehicles(long activeVehicles) {
        this.activeVehicles = activeVehicles;
    }

    public long getLiveTrackedVehiclesCount() {
        return liveTrackedVehiclesCount;
    }

    public void setLiveTrackedVehiclesCount(long liveTrackedVehiclesCount) {
        this.liveTrackedVehiclesCount = liveTrackedVehiclesCount;
    }

    public long getInTransitOrders() {
        return inTransitOrders;
    }

    public void setInTransitOrders(long inTransitOrders) {
        this.inTransitOrders = inTransitOrders;
    }

    public long getDeliveredToday() {
        return deliveredToday;
    }

    public void setDeliveredToday(long deliveredToday) {
        this.deliveredToday = deliveredToday;
    }

    public double getAverageDeliveryTimeSeconds() {
        return averageDeliveryTimeSeconds;
    }

    public void setAverageDeliveryTimeSeconds(double averageDeliveryTimeSeconds) {
        this.averageDeliveryTimeSeconds = averageDeliveryTimeSeconds;
    }
}
