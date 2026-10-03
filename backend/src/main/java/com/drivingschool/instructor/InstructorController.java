package com.drivingschool.instructor;

import com.drivingschool.instructor.dto.CreateInstructorRequest;
import com.drivingschool.instructor.dto.InstructorPageResponse;
import com.drivingschool.instructor.dto.InstructorResponse;
import com.drivingschool.instructor.dto.UpdateInstructorRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/instructors")
@PreAuthorize("hasRole('ADMIN')")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @PostMapping
    public ResponseEntity<InstructorResponse> create(@Valid @RequestBody CreateInstructorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(instructorService.create(request));
    }

    @GetMapping
    public InstructorPageResponse findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InstructorStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        return InstructorPageResponse.from(instructorService.findAll(
                search,
                status,
                PageRequest.of(page, size, Sort.by("lastName").ascending().and(Sort.by("firstName").ascending()))
        ));
    }

    @GetMapping("/{id}")
    public InstructorResponse findById(@PathVariable UUID id) {
        return instructorService.findById(id);
    }

    @PutMapping("/{id}")
    public InstructorResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInstructorRequest request
    ) {
        return instructorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        instructorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
