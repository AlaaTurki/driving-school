package com.drivingschool.vehicle;

public class VehicleRegistrationAlreadyExistsException extends RuntimeException {

    public VehicleRegistrationAlreadyExistsException() {
        super("A vehicle with this registration number already exists");
    }
}
