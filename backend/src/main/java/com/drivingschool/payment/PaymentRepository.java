package com.drivingschool.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    @Query("select coalesce(sum(payment.amount), 0) from Payment payment where payment.candidate.id = :candidateId")
    BigDecimal sumAmountsByCandidateId(@Param("candidateId") UUID candidateId);

    @Query("select coalesce(sum(payment.amount), 0) from Payment payment")
    BigDecimal sumAllAmounts();

    @Query(value = """
            select cast(date_trunc('month', payment_date) as date) as "monthStart",
                   sum(amount) as total
            from payments
            where payment_date >= :from and payment_date < :to
            group by date_trunc('month', payment_date)
            """, nativeQuery = true)
    List<PaymentMonthTotal> sumPaymentsByMonth(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    boolean existsByCandidate_Id(UUID candidateId);

    interface PaymentMonthTotal {
        LocalDate getMonthStart();

        BigDecimal getTotal();
    }
}
