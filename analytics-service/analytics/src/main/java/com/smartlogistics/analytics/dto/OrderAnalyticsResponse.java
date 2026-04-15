package com.smartlogistics.analytics.dto;

public class OrderAnalyticsResponse {
    private Long orderId;
    private String orderType;
    private String createdAt;
    private String assignedAt;
    private String inTransitAt;
    private String deliveredAt;
    private Long assignmentDurationSeconds;
    private Long deliveryDurationSeconds;
    private Long vehicleId;

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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(String assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getInTransitAt() {
        return inTransitAt;
    }

    public void setInTransitAt(String inTransitAt) {
        this.inTransitAt = inTransitAt;
    }

    public String getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(String deliveredAt) {
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
}
