package com.drivingschool.drivingsession.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateDrivingSessionRequest(
        @NotNull UUID candidateId,
        UUID instructorId,
        UUID vehicleId,
        UUID circuitId,
        @NotNull LocalDate sessionDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @PositiveOrZero @Digits(integer = 7, fraction = 3) BigDecimal price,
        @Size(max = 10000) String notes
) {

    @jakarta.validation.constraints.AssertTrue(message = "endTime must be after startTime")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
