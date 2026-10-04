package com.drivingschool.circuit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CircuitRepository extends JpaRepository<Circuit, UUID> {

    @Query("""
            select circuit from Circuit circuit
            where (:active is null or circuit.active = :active)
              and (:search = ''
                   or lower(circuit.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(circuit.location, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Circuit> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable
    );

    Optional<Circuit> findById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
