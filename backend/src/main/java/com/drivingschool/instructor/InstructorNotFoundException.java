package com.drivingschool.instructor;

import java.util.UUID;

public class InstructorNotFoundException extends RuntimeException {

    public InstructorNotFoundException(UUID id) {
        super("Instructor not found: " + id);
    }
}
