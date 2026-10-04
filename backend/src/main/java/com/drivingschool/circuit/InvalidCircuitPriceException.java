package com.drivingschool.circuit;

public class InvalidCircuitPriceException extends RuntimeException {

    public InvalidCircuitPriceException() {
        super("Circuit price must be zero or greater and have at most three decimal places");
    }
}
