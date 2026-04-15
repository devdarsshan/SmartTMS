package com.smartlogistics.tracking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class LocationUpdateRequest {
    @NotNull(message = "vehicleId is required")
    private Long vehicleId;

    @NotNull(message = "driverId is required")
    private Long driverId;

    @NotNull(message = "lat is required")
    @DecimalMin(value = "-90.0", message = "lat must be >= -90")
    @DecimalMax(value = "90.0", message = "lat must be <= 90")
    private Double lat;

    @NotNull(message = "lng is required")
    @DecimalMin(value = "-180.0", message = "lng must be >= -180")
    @DecimalMax(value = "180.0", message = "lng must be <= 180")
    private Double lng;

    @NotNull(message = "speed is required")
    @PositiveOrZero(message = "speed must be non-negative")
    private Double speed;

    @NotNull(message = "heading is required")
    @DecimalMin(value = "0.0", message = "heading must be >= 0")
    @DecimalMax(value = "360.0", message = "heading must be <= 360")
    private Double heading;

    @NotNull(message = "timestamp is required")
    private Long timestamp;

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Double getHeading() {
        return heading;
    }

    public void setHeading(Double heading) {
        this.heading = heading;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
