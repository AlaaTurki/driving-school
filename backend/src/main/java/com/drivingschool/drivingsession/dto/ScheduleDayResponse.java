package com.drivingschool.drivingsession.dto;

import java.time.LocalDate;
import java.util.List;

public record ScheduleDayResponse(LocalDate date, List<ScheduleEntryResponse> sessions) {
}
