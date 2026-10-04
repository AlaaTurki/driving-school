package com.drivingschool.vehicle;

import java.util.UUID;

public class VehicleNotFoundException extends RuntimeException {

    public VehicleNotFoundException(UUID id) {
        super("Vehicle not found: " + id);
    }
}
