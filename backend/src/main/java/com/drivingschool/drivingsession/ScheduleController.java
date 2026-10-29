package com.drivingschool.drivingsession;

import com.drivingschool.drivingsession.dto.ScheduleAvailabilityResponse;
import com.drivingschool.drivingsession.dto.ScheduleDayResponse;
import com.drivingschool.security.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedule")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleDayResponse> getSchedule(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) UUID instructorId,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return scheduleService.getSchedule(from, to, instructorId, actor);
    }

    @GetMapping("/availability")
    public ScheduleAvailabilityResponse getAvailability(
            @RequestParam LocalDate date,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime
    ) {
        return scheduleService.getAvailability(date, startTime, endTime);
    }
}
