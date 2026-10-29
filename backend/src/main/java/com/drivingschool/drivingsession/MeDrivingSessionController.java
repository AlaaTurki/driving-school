package com.drivingschool.drivingsession;

import com.drivingschool.drivingsession.dto.DrivingSessionPageResponse;
import com.drivingschool.security.AppUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/me/sessions")
@PreAuthorize("hasRole('CANDIDATE')")
public class MeDrivingSessionController {

    private final DrivingSessionService sessionService;

    public MeDrivingSessionController(DrivingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public DrivingSessionPageResponse findMySessions(
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails candidate
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        return DrivingSessionPageResponse.from(sessionService.findForCandidate(
                dateFrom,
                dateTo,
                PageRequest.of(page, size, Sort.by("sessionDate").descending().and(Sort.by("startTime").descending())),
                candidate
        ));
    }
}
