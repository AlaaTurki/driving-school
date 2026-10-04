package com.drivingschool.circuit;

public class CircuitNameAlreadyExistsException extends RuntimeException {

    public CircuitNameAlreadyExistsException() {
        super("A circuit with this name already exists");
    }
}
