package com.smartlogistics.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_analytics")
public class OrderAnalytics {
    @Id
    private Long orderId;
    private String orderType;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime inTransitAt;
    private LocalDateTime deliveredAt;
    private Long assignmentDurationSeconds;
    private Long deliveryDurationSeconds;
    private Long vehicleId;
    private Long createdByUserId;
    private String fromCity;
    private String toCity;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getInTransitAt() {
        return inTransitAt;
    }

    public void setInTransitAt(LocalDateTime inTransitAt) {
        this.inTransitAt = inTransitAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Long getAssignmentDurationSeconds() {
        return assignmentDurationSeconds;
    }

    public void setAssignmentDurationSeconds(Long assignmentDurationSeconds) {
        this.assignmentDurationSeconds = assignmentDurationSeconds;
    }

    public Long getDeliveryDurationSeconds() {
        return deliveryDurationSeconds;
    }

    public void setDeliveryDurationSeconds(Long deliveryDurationSeconds) {
        this.deliveryDurationSeconds = deliveryDurationSeconds;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getFromCity() {
        return fromCity;
    }

    public void setFromCity(String fromCity) {
        this.fromCity = fromCity;
    }

    public String getToCity() {
        return toCity;
    }

    public void setToCity(String toCity) {
        this.toCity = toCity;
    }
}
