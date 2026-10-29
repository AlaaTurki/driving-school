package com.drivingschool.drivingsession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleConflictDetails(
        UUID sessionId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String candidateName,
        String instructorName,
        String vehicleRegistration
) {

    static ScheduleConflictDetails from(DrivingSession session) {
        return new ScheduleConflictDetails(
                session.getId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCandidate().getFirstName() + " " + session.getCandidate().getLastName(),
                session.getInstructor().getFirstName() + " " + session.getInstructor().getLastName(),
                session.getVehicle() == null ? null : session.getVehicle().getRegistrationNumber()
        );
    }
}
