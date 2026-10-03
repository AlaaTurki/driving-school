package com.drivingschool.candidate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    @EntityGraph(attributePaths = "user")
    @Query("""
            select candidate from Candidate candidate
            where (:status is null or candidate.status = :status)
              and (:search = ''
                   or lower(concat(candidate.firstName, ' ', candidate.lastName)) like lower(concat('%', :search, '%'))
                   or lower(candidate.email) like lower(concat('%', :search, '%'))
                   or lower(candidate.phone) like lower(concat('%', :search, '%')))
            """)
    Page<Candidate> search(
            @Param("search") String search,
            @Param("status") CandidateStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "user")
    Optional<Candidate> findById(UUID id);

    @EntityGraph(attributePaths = "user")
    Optional<Candidate> findByUser_Id(UUID userId);

    boolean existsByEmailIgnoreCase(String email);
}
