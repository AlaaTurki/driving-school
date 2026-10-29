package com.drivingschool.drivingsession;

public class ScheduleConflictException extends RuntimeException {

    private final String resource;
    private final ScheduleConflictDetails conflictingSession;

    public ScheduleConflictException(String resource, String message, ScheduleConflictDetails conflictingSession) {
        super(message);
        this.resource = resource;
        this.conflictingSession = conflictingSession;
    }

    public String getResource() {
        return resource;
    }

    public ScheduleConflictDetails getConflictingSession() {
        return conflictingSession;
    }
}
