package com.drivingschool.balance.dto;

import com.drivingschool.drivingsession.DrivingSessionStatus;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record StatementLineResponse(
        UUID sessionId,
        DayOfWeek day,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal hours,
        BigDecimal price,
        DrivingSessionStatus status,
        BigDecimal paidAmount,
        BigDecimal remaining
) {
}
