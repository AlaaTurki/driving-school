package com.drivingschool.balance;

import com.drivingschool.drivingsession.DrivingSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DrivingSessionAllocation(
        UUID sessionId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        DrivingSessionStatus status
) {
}
