package com.drivingschool.drivingsession;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DrivingSessionSpecifications {

    private DrivingSessionSpecifications() {
    }

    public static Specification<DrivingSession> forSchedule(
            LocalDate from,
            LocalDate to,
            UUID instructorId,
            UUID candidateUserId,
            boolean upcomingOnly,
            LocalDate today,
            LocalTime currentTime
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("sessionDate"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("sessionDate"), to));
            }
            if (instructorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("instructor").get("id"), instructorId));
            }
            if (candidateUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("candidate").get("user").get("id"), candidateUserId));
            }
            if (upcomingOnly) {
                predicates.add(criteriaBuilder.equal(root.get("status"), DrivingSessionStatus.PLANNED));
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.greaterThan(root.get("sessionDate"), today),
                        criteriaBuilder.and(
                                criteriaBuilder.equal(root.get("sessionDate"), today),
                                criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), currentTime)
                        )
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<DrivingSession> search(
            LocalDate dateFrom,
            LocalDate dateTo,
            UUID candidateId,
            UUID instructorId,
            DrivingSessionStatus status,
            UUID circuitId,
            UUID instructorUserId,
            UUID candidateUserId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("sessionDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("sessionDate"), dateTo));
            }
            if (candidateId != null) {
                predicates.add(criteriaBuilder.equal(root.get("candidate").get("id"), candidateId));
            }
            if (instructorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("instructor").get("id"), instructorId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (circuitId != null) {
                predicates.add(criteriaBuilder.equal(root.get("circuit").get("id"), circuitId));
            }
            if (instructorUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("instructor").get("user").get("id"), instructorUserId));
            }
            if (candidateUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("candidate").get("user").get("id"), candidateUserId));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<DrivingSession> overlappingResourceOnDate(
            String resource,
            UUID resourceId,
            LocalDate date,
            UUID excludedSessionId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("sessionDate"), date));
            predicates.add(criteriaBuilder.notEqual(root.get("status"), DrivingSessionStatus.CANCELLED));
            predicates.add(criteriaBuilder.equal(root.get(resource).get("id"), resourceId));
            if (excludedSessionId != null) {
                predicates.add(criteriaBuilder.notEqual(root.get("id"), excludedSessionId));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<DrivingSession> activeSessionsOnDate(LocalDate date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("sessionDate"), date),
                criteriaBuilder.notEqual(root.get("status"), DrivingSessionStatus.CANCELLED)
        );
    }
}
