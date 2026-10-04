package com.drivingschool.circuit;

import com.drivingschool.circuit.dto.CircuitPageResponse;
import com.drivingschool.circuit.dto.CircuitResponse;
import com.drivingschool.circuit.dto.SaveCircuitRequest;
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
@RequestMapping("/api/circuits")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
public class CircuitController {

    private final CircuitService circuitService;

    public CircuitController(CircuitService circuitService) {
        this.circuitService = circuitService;
    }

    @GetMapping
    public CircuitPageResponse findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100"
            );
        }
        return CircuitPageResponse.from(circuitService.findAll(
                search,
                active,
                PageRequest.of(page, size, Sort.by("name").ascending())
        ));
    }

    @GetMapping("/{id}")
    public CircuitResponse findById(@PathVariable UUID id) {
        return circuitService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CircuitResponse> create(@Valid @RequestBody SaveCircuitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(circuitService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CircuitResponse update(@PathVariable UUID id, @Valid @RequestBody SaveCircuitRequest request) {
        return circuitService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        circuitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
