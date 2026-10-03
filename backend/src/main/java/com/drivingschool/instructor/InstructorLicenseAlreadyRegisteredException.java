package com.drivingschool.instructor;

public class InstructorLicenseAlreadyRegisteredException extends RuntimeException {

    public InstructorLicenseAlreadyRegisteredException() {
        super("An instructor with this license number already exists");
    }
}
