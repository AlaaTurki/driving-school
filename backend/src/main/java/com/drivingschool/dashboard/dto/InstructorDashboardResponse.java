package com.drivingschool.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record InstructorDashboardResponse(
        List<DashboardInstructorSession> todaySessions,
        List<DashboardInstructorSession> upcomingSessions,
        long candidatesCount,
        HoursTaught hoursTaught
) {

    public record HoursTaught(BigDecimal total, BigDecimal currentMonth) {
    }
}
