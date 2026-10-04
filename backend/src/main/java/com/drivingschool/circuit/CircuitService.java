package com.drivingschool.circuit;

import com.drivingschool.circuit.dto.CircuitResponse;
import com.drivingschool.circuit.dto.SaveCircuitRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
public class CircuitService {

    private final CircuitRepository circuitRepository;

    public CircuitService(CircuitRepository circuitRepository) {
        this.circuitRepository = circuitRepository;
    }

    @Transactional(readOnly = true)
    public Page<CircuitResponse> findAll(String search, Boolean active, Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim();
        return circuitRepository.search(normalizedSearch, active, pageable).map(CircuitService::toResponse);
    }

    @Transactional(readOnly = true)
    public CircuitResponse findById(UUID id) {
        return circuitRepository.findById(id)
                .map(CircuitService::toResponse)
                .orElseThrow(() -> new CircuitNotFoundException(id));
    }

    @Transactional
    public CircuitResponse create(SaveCircuitRequest request) {
        validatePrice(request.price());
        String name = request.name().trim();
        if (circuitRepository.existsByNameIgnoreCase(name)) {
            throw new CircuitNameAlreadyExistsException();
        }
        return toResponse(save(new Circuit(
                name,
                trimToNull(request.description()),
                trimToNull(request.location()),
                request.price(),
                request.active()
        )));
    }

    @Transactional
    public CircuitResponse update(UUID id, SaveCircuitRequest request) {
        validatePrice(request.price());
        Circuit circuit = circuitRepository.findById(id)
                .orElseThrow(() -> new CircuitNotFoundException(id));
        String name = request.name().trim();
        if (!circuit.getName().equalsIgnoreCase(name)
                && circuitRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new CircuitNameAlreadyExistsException();
        }
        circuit.update(
                name,
                trimToNull(request.description()),
                trimToNull(request.location()),
                request.price(),
                request.active()
        );
        return toResponse(save(circuit));
    }

    @Transactional
    public void delete(UUID id) {
        Circuit circuit = circuitRepository.findById(id)
                .orElseThrow(() -> new CircuitNotFoundException(id));
        // TODO(Phase 5): refuse deletion if sessions reference this circuit; advise setting active=false.
        circuitRepository.delete(circuit);
    }

    private Circuit save(Circuit circuit) {
        try {
            return circuitRepository.saveAndFlush(circuit);
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, "uk_circuits_name")) {
                throw new CircuitNameAlreadyExistsException();
            }
            throw exception;
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price.signum() < 0 || price.precision() - price.scale() > 7 || price.stripTrailingZeros().scale() > 3) {
            throw new InvalidCircuitPriceException();
        }
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static CircuitResponse toResponse(Circuit circuit) {
        return new CircuitResponse(
                circuit.getId(),
                circuit.getName(),
                circuit.getDescription(),
                circuit.getLocation(),
                circuit.getPrice(),
                circuit.isActive(),
                circuit.getCreatedAt(),
                circuit.getUpdatedAt()
        );
    }
}
