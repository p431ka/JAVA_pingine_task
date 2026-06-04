package com.pingine.fleetpulse.exception;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(String vehicleId) {
        super("Vehicle not found: " + vehicleId);
    }
}
