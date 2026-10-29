package com.drivingschool.drivingsession.dto;

import com.drivingschool.drivingsession.DrivingSessionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeDrivingSessionStatusRequest(
        @NotNull DrivingSessionStatus status,
        @Size(max = 10000) String notes
) {
}
