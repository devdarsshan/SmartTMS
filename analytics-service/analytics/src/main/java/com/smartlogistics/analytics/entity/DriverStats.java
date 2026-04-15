package com.smartlogistics.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_stats")
public class DriverStats {
    @Id
    private Long driverId;
    private Long currentVehicleId;
    private Long deliveredOrderCount = 0L;
    private Integer activeOrderCount = 0;
    private Double totalDistanceKm = 0D;
    private LocalDateTime updatedAt;

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

    public Long getDeliveredOrderCount() {
        return deliveredOrderCount;
    }

    public void setDeliveredOrderCount(Long deliveredOrderCount) {
        this.deliveredOrderCount = deliveredOrderCount;
    }

    public Integer getActiveOrderCount() {
        return activeOrderCount;
    }

    public void setActiveOrderCount(Integer activeOrderCount) {
        this.activeOrderCount = activeOrderCount;
    }

    public Double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(Double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
