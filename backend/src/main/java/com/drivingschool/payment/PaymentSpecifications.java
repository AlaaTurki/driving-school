package com.drivingschool.payment;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PaymentSpecifications {

    private PaymentSpecifications() {
    }

    public static Specification<Payment> search(
            UUID candidateId,
            LocalDate dateFrom,
            LocalDate dateTo,
            PaymentMethod paymentMethod
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (candidateId != null) {
                predicates.add(criteriaBuilder.equal(root.get("candidate").get("id"), candidateId));
            }
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("paymentDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("paymentDate"), dateTo));
            }
            if (paymentMethod != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
