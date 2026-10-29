package com.drivingschool.drivingsession;

import com.drivingschool.drivingsession.dto.ScheduleDayResponse;
import com.drivingschool.security.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/schedule")
@PreAuthorize("hasRole('CANDIDATE')")
public class MeScheduleController {

    private final ScheduleService scheduleService;

    public MeScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleDayResponse> getMySchedule(@AuthenticationPrincipal AppUserDetails candidate) {
        return scheduleService.getMyUpcomingSchedule(candidate);
    }
}
