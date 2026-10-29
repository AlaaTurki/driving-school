package com.drivingschool.drivingsession;

public class DrivingSessionResourceInUseException extends RuntimeException {

    public DrivingSessionResourceInUseException(String resource) {
        super("Cannot delete " + resource + " with driving sessions; deactivate it instead");
    }
}
