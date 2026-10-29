package com.drivingschool.drivingsession.dto;

import com.drivingschool.drivingsession.DrivingSession;
import com.drivingschool.drivingsession.DrivingSessionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DrivingSessionResponse(
        UUID id,
        SessionPersonResponse candidate,
        SessionPersonResponse instructor,
        SessionVehicleResponse vehicle,
        SessionCircuitResponse circuit,
        LocalDate sessionDate,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal durationHours,
        BigDecimal price,
        DrivingSessionStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static DrivingSessionResponse from(DrivingSession session) {
        Duration duration = Duration.between(session.getStartTime(), session.getEndTime());
        BigDecimal durationHours = BigDecimal.valueOf(duration.toSeconds())
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
        return new DrivingSessionResponse(
                session.getId(),
                new SessionPersonResponse(
                        session.getCandidate().getId(),
                        session.getCandidate().getFirstName() + " " + session.getCandidate().getLastName()
                ),
                new SessionPersonResponse(
                        session.getInstructor().getId(),
                        session.getInstructor().getFirstName() + " " + session.getInstructor().getLastName()
                ),
                session.getVehicle() == null ? null : new SessionVehicleResponse(
                        session.getVehicle().getId(),
                        session.getVehicle().getRegistrationNumber(),
                        session.getVehicle().getBrand(),
                        session.getVehicle().getModel()
                ),
                session.getCircuit() == null ? null : new SessionCircuitResponse(
                        session.getCircuit().getId(),
                        session.getCircuit().getName()
                ),
                session.getSessionDate(),
                session.getSessionDate().getDayOfWeek(),
                session.getStartTime(),
                session.getEndTime(),
                durationHours,
                session.getPrice(),
                session.getStatus(),
                session.getNotes(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
