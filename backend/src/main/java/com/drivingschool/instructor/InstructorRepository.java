package com.drivingschool.instructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InstructorRepository extends JpaRepository<Instructor, UUID> {

    @EntityGraph(attributePaths = "user")
    @Query("""
            select instructor from Instructor instructor
            where (:status is null or instructor.status = :status)
              and (:search = ''
                   or lower(concat(instructor.firstName, ' ', instructor.lastName)) like lower(concat('%', :search, '%'))
                   or lower(instructor.email) like lower(concat('%', :search, '%'))
                   or lower(instructor.phone) like lower(concat('%', :search, '%'))
                   or lower(instructor.licenseNumber) like lower(concat('%', :search, '%')))
            """)
    Page<Instructor> search(
            @Param("search") String search,
            @Param("status") InstructorStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "user")
    Optional<Instructor> findById(UUID id);

    @EntityGraph(attributePaths = "user")
    Optional<Instructor> findByUser_Id(UUID userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, UUID id);
}
