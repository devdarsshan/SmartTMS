package com.smartlogistics.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_stats")
public class VehicleStats {
    @Id
    private Long vehicleId;
    private Double totalDistanceKm = 0D;
    private Double lastLat;
    private Double lastLng;
    private LocalDateTime lastLocationAt;
    private Integer activeOrderCount = 0;
    private String vehicleStatus;
    private LocalDateTime updatedAt;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(Double totalDistanceKm) {
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

    public LocalDateTime getLastLocationAt() {
        return lastLocationAt;
    }

    public void setLastLocationAt(LocalDateTime lastLocationAt) {
        this.lastLocationAt = lastLocationAt;
    }

    public Integer getActiveOrderCount() {
        return activeOrderCount;
    }

    public void setActiveOrderCount(Integer activeOrderCount) {
        this.activeOrderCount = activeOrderCount;
    }

    public String getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(String vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
