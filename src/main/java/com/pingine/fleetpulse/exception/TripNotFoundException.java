package com.pingine.fleetpulse.exception;

public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(String vehicleId) {
        super("Completed trip not found for vehicle: " + vehicleId);
    }
}
