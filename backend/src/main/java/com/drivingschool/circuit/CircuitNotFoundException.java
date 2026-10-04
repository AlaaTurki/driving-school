package com.drivingschool.circuit;

import java.util.UUID;

public class CircuitNotFoundException extends RuntimeException {

    public CircuitNotFoundException(UUID id) {
        super("Circuit not found: " + id);
    }
}
