package com.drivingschool.drivingsession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record UpdateDrivingSessionRequest(
        @NotNull UUID candidateId,
        @NotNull UUID instructorId,
        UUID vehicleId,
        UUID circuitId,
        @NotNull LocalDate sessionDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Size(max = 10000) String notes
) {

    @jakarta.validation.constraints.AssertTrue(message = "endTime must be after startTime")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
