package com.drivingschool.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

    @EntityGraph(attributePaths = "user")
    List<CandidateProfile> findAllByOrderByFullNameAsc();
}
