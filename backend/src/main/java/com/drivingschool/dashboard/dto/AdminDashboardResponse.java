package com.drivingschool.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long candidatesCount,
        long instructorsCount,
        long sessionsToday,
        long sessionsThisWeek,
        long sessionsDone,
        long sessionsPlanned,
        BigDecimal totalPaid,
        BigDecimal totalRemaining,
        List<DashboardChartPoint> sessionsPerDay,
        List<DashboardChartPoint> paymentsPerMonth,
        List<DashboardStatusCount> sessionsByStatus
) {
}
