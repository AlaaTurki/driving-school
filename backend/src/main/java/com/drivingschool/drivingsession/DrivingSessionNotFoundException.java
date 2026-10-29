package com.drivingschool.drivingsession;

import java.util.UUID;

public class DrivingSessionNotFoundException extends RuntimeException {

    public DrivingSessionNotFoundException(UUID id) {
        super("Driving session not found: " + id);
    }
}
