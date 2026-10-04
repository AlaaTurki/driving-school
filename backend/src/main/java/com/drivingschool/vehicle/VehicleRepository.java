package com.drivingschool.vehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    @Query("""
            select vehicle from Vehicle vehicle
            where (:active is null or vehicle.active = :active)
              and (:search = ''
                   or lower(vehicle.registrationNumber) like lower(concat('%', :search, '%'))
                   or lower(vehicle.brand) like lower(concat('%', :search, '%'))
                   or lower(vehicle.model) like lower(concat('%', :search, '%')))
            """)
    Page<Vehicle> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    Optional<Vehicle> findById(UUID id);

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    boolean existsByRegistrationNumberIgnoreCaseAndIdNot(String registrationNumber, UUID id);
}
