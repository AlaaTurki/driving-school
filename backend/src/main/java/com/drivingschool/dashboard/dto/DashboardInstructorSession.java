package com.drivingschool.dashboard.dto;

import com.drivingschool.drivingsession.DrivingSession;
import com.drivingschool.drivingsession.DrivingSessionStatus;
import com.drivingschool.drivingsession.dto.SessionCircuitResponse;
import com.drivingschool.drivingsession.dto.SessionPersonResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DashboardInstructorSession(
        UUID id,
        SessionPersonResponse candidate,
        SessionCircuitResponse circuit,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        DrivingSessionStatus status
) {

    public static DashboardInstructorSession from(DrivingSession session) {
        return new DashboardInstructorSession(
                session.getId(),
                new SessionPersonResponse(
                        session.getCandidate().getId(),
                        session.getCandidate().getFirstName() + " " + session.getCandidate().getLastName()
                ),
                session.getCircuit() == null ? null : new SessionCircuitResponse(
                        session.getCircuit().getId(),
                        session.getCircuit().getName()
                ),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus()
        );
    }
}
