package com.drivingschool.payment;

public class PaymentResourceInUseException extends RuntimeException {

    public PaymentResourceInUseException(String resource) {
        super("Cannot delete " + resource + " with payments");
    }
}
