package com.drivingschool.vehicle;

import com.drivingschool.vehicle.dto.SaveVehicleRequest;
import com.drivingschool.vehicle.dto.VehicleResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public Page<VehicleResponse> findAll(String search, Boolean active, Pageable pageable) {
        String normalizedSearch = search == null ? "" : search.trim();
        return vehicleRepository.search(normalizedSearch, active, pageable).map(VehicleService::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleResponse findById(UUID id) {
        return vehicleRepository.findById(id)
                .map(VehicleService::toResponse)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    @Transactional
    public VehicleResponse create(SaveVehicleRequest request) {
        String registrationNumber = normalizeRegistrationNumber(request.registrationNumber());
        if (vehicleRepository.existsByRegistrationNumberIgnoreCase(registrationNumber)) {
            throw new VehicleRegistrationAlreadyExistsException();
        }
        return toResponse(save(new Vehicle(
                registrationNumber,
                request.brand().trim(),
                request.model().trim(),
                request.type(),
                request.active()
        )));
    }

    @Transactional
    public VehicleResponse update(UUID id, SaveVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        String registrationNumber = normalizeRegistrationNumber(request.registrationNumber());
        if (!vehicle.getRegistrationNumber().equals(registrationNumber)
                && vehicleRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(registrationNumber, id)) {
            throw new VehicleRegistrationAlreadyExistsException();
        }
        vehicle.update(
                registrationNumber,
                request.brand().trim(),
                request.model().trim(),
                request.type(),
                request.active()
        );
        return toResponse(save(vehicle));
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
        // TODO(Phase 5): refuse deletion if sessions reference this vehicle; advise setting active=false.
        vehicleRepository.delete(vehicle);
    }

    private Vehicle save(Vehicle vehicle) {
        try {
            return vehicleRepository.saveAndFlush(vehicle);
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, "uk_vehicles_registration_number")) {
                throw new VehicleRegistrationAlreadyExistsException();
            }
            throw exception;
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

    private static String normalizeRegistrationNumber(String registrationNumber) {
        return registrationNumber.trim().toUpperCase(Locale.ROOT);
    }

    private static VehicleResponse toResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistrationNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getType(),
                vehicle.isActive(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
}
