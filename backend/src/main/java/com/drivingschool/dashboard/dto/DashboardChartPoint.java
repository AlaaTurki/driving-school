package com.drivingschool.dashboard.dto;

import java.math.BigDecimal;

public record DashboardChartPoint(String label, BigDecimal value, int height) {
}
