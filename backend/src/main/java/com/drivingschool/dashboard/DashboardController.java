package com.drivingschool.dashboard;

import com.drivingschool.dashboard.dto.AdminDashboardResponse;
import com.drivingschool.dashboard.dto.InstructorDashboardResponse;
import com.drivingschool.security.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardResponse getAdminDashboard() {
        return dashboardService.getAdminDashboard();
    }

    @GetMapping("/instructor")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public InstructorDashboardResponse getInstructorDashboard(@AuthenticationPrincipal AppUserDetails principal) {
        return dashboardService.getInstructorDashboard(principal.id());
    }
}
