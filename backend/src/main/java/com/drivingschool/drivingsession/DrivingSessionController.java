package com.drivingschool.drivingsession;

import com.drivingschool.drivingsession.dto.ChangeDrivingSessionStatusRequest;
import com.drivingschool.drivingsession.dto.CreateDrivingSessionRequest;
import com.drivingschool.drivingsession.dto.DrivingSessionPageResponse;
import com.drivingschool.drivingsession.dto.DrivingSessionResponse;
import com.drivingschool.drivingsession.dto.UpdateDrivingSessionRequest;
import com.drivingschool.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/driving-sessions")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class DrivingSessionController {

    private final DrivingSessionService sessionService;

    public DrivingSessionController(DrivingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public DrivingSessionPageResponse findAll(
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) UUID candidateId,
            @RequestParam(required = false) UUID instructorId,
            @RequestParam(required = false) DrivingSessionStatus status,
            @RequestParam(required = false) UUID circuitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        return DrivingSessionPageResponse.from(sessionService.findAll(
                dateFrom,
                dateTo,
                candidateId,
                instructorId,
                status,
                circuitId,
                PageRequest.of(page, size, Sort.by("sessionDate").descending().and(Sort.by("startTime").descending())),
                actor
        ));
    }

    @GetMapping("/{id}")
    public DrivingSessionResponse findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return sessionService.findById(id, actor);
    }

    @PostMapping
    public ResponseEntity<DrivingSessionResponse> create(
            @Valid @RequestBody CreateDrivingSessionRequest request,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(request, actor));
    }

    @PutMapping("/{id}")
    public DrivingSessionResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDrivingSessionRequest request,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return sessionService.update(id, request, actor);
    }

    @PatchMapping("/{id}/status")
    public DrivingSessionResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeDrivingSessionStatusRequest request,
            @AuthenticationPrincipal AppUserDetails actor
    ) {
        return sessionService.changeStatus(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        sessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
