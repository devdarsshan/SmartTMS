package com.smartlogistics.vehicleservice.entity;


import com.smartlogistics.vehicleservice.enums.VehicleStatus;
import com.smartlogistics.vehicleservice.enums.VehicleType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleId;
    private Long driverId;
    @Column(nullable = false)
    private String vehicleName;
    @Column(nullable = false, unique = true)
    private String vehicleCode;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;
    @Column(nullable = false)
    private String from;
    @Column(nullable = false)
    private String to;
    @ElementCollection
    @CollectionTable(name = "vehicle_through_points", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "through_point", nullable = false)
    private List<String> through = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "vehicle_orders", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "order_id", nullable = false)
    private List<Long> orderIds = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    private VehicleStatus vehicleStatus = VehicleStatus.IDLE;

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

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public String getVehicleCode() {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode) {
        this.vehicleCode = vehicleCode;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<String> getThrough() {
        return through;
    }

    public void setThrough(List<String> through) {
        this.through = through;
    }

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }

    public VehicleStatus getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(VehicleStatus vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }
}
