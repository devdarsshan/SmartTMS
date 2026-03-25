package com.smartlogistics.vehicleservice.exceptions;

public class VehicleDoesNotExistException extends RuntimeException {
    public VehicleDoesNotExistException(String message) {
        super(message);
    }
}
