package com.drivingschool.dashboard.dto;

import com.drivingschool.drivingsession.DrivingSessionStatus;

public record DashboardStatusCount(
        DrivingSessionStatus status,
        String label,
        long value,
        int height
) {
}
