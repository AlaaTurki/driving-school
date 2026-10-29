package com.drivingschool.drivingsession.dto;

import com.drivingschool.drivingsession.DrivingSession;
import com.drivingschool.drivingsession.DrivingSessionStatus;

import java.time.LocalTime;
import java.util.UUID;

public record ScheduleEntryResponse(
        UUID sessionId,
        UUID instructorId,
        String instructorName,
        LocalTime startTime,
        LocalTime endTime,
        String candidateName,
        String circuit,
        String vehicle,
        DrivingSessionStatus status
) {

    public static ScheduleEntryResponse from(DrivingSession session) {
        String vehicle = session.getVehicle() == null
                ? null
                : session.getVehicle().getRegistrationNumber() + " - "
                        + session.getVehicle().getBrand() + " " + session.getVehicle().getModel();
        return new ScheduleEntryResponse(
                session.getId(),
                session.getInstructor().getId(),
                session.getInstructor().getFirstName() + " " + session.getInstructor().getLastName(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCandidate().getFirstName() + " " + session.getCandidate().getLastName(),
                session.getCircuit() == null ? null : session.getCircuit().getName(),
                vehicle,
                session.getStatus()
        );
    }
}
