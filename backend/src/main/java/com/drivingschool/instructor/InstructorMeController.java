package com.drivingschool.instructor;

import com.drivingschool.instructor.dto.InstructorResponse;
import com.drivingschool.security.AppUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructors/me")
@PreAuthorize("hasRole('INSTRUCTOR')")
public class InstructorMeController {

    private final InstructorService instructorService;

    public InstructorMeController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @GetMapping
    public InstructorResponse getMyProfile(@AuthenticationPrincipal AppUserDetails user) {
        return instructorService.findForUser(user);
    }
}
