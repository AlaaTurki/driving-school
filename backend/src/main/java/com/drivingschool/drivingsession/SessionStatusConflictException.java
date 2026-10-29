package com.drivingschool.drivingsession;

public class SessionStatusConflictException extends RuntimeException {

    public SessionStatusConflictException(String message) {
        super(message);
    }
}
