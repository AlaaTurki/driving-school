package com.drivingschool.balance;

import com.drivingschool.balance.dto.BalanceResponse;
import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateNotFoundException;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.candidate.CandidateStatus;
import com.drivingschool.drivingsession.BillableSessionProjection;
import com.drivingschool.drivingsession.DrivingSession;
import com.drivingschool.drivingsession.DrivingSessionRepository;
import com.drivingschool.drivingsession.DrivingSessionStatus;
import com.drivingschool.payment.PaymentRepository;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BalanceServiceTest {

    private final CandidateRepository candidateRepository = mock(CandidateRepository.class);
    private final DrivingSessionRepository sessionRepository = mock(DrivingSessionRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PaymentAllocationService allocationService = new PaymentAllocationService();
    private final BalanceService service =
            new BalanceService(candidateRepository, sessionRepository, paymentRepository, allocationService);
    private final UUID candidateId = UUID.randomUUID();

    @Test
    void computesTheSpecifiedTenSessionBalanceWithDatabaseAggregates() {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate()));
        when(sessionRepository.sumBillablePricesByCandidateId(candidateId, BalanceService.BILLABLE_STATUSES))
                .thenReturn(new BigDecimal("560"));
        when(paymentRepository.sumAmountsByCandidateId(candidateId)).thenReturn(new BigDecimal("250"));

        BalanceResponse balance = service.getBalance(candidateId, admin());

        assertThat(balance.totalSessions()).isEqualByComparingTo("560.000");
        assertThat(balance.totalPaid()).isEqualByComparingTo("250.000");
        assertThat(balance.remaining()).isEqualByComparingTo("310.000");
        assertThat(balance.credit()).isFalse();
        assertThat(BalanceService.BILLABLE_STATUSES)
                .containsExactlyInAnyOrder(DrivingSessionStatus.DONE, DrivingSessionStatus.ABSENT);
    }

    @Test
    void balanceIsRecomputedAfterPaymentOrSessionTotalsChange() {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate()));
        when(sessionRepository.sumBillablePricesByCandidateId(candidateId, BalanceService.BILLABLE_STATUSES))
                .thenReturn(new BigDecimal("560"), new BigDecimal("504"));
        when(paymentRepository.sumAmountsByCandidateId(candidateId))
                .thenReturn(new BigDecimal("250"), new BigDecimal("300"));

        assertThat(service.getBalance(candidateId, admin()).remaining()).isEqualByComparingTo("310");
        assertThat(service.getBalance(candidateId, admin()).remaining()).isEqualByComparingTo("204");
        verify(sessionRepository, org.mockito.Mockito.times(2))
                .sumBillablePricesByCandidateId(candidateId, BalanceService.BILLABLE_STATUSES);
        verify(paymentRepository, org.mockito.Mockito.times(2)).sumAmountsByCandidateId(candidateId);
    }

    @Test
    void exposesNegativeBalanceAsCredit() {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate()));
        when(sessionRepository.sumBillablePricesByCandidateId(candidateId, BalanceService.BILLABLE_STATUSES))
                .thenReturn(new BigDecimal("100"));
        when(paymentRepository.sumAmountsByCandidateId(candidateId)).thenReturn(new BigDecimal("150"));

        BalanceResponse balance = service.getBalance(candidateId, admin());

        assertThat(balance.remaining()).isEqualByComparingTo("-50.000");
        assertThat(balance.credit()).isTrue();
    }

    @Test
    void statementUsesAllHistoricalPaymentsForChronologicalAllocationAndRepeatsOverallBalance() {
        Candidate candidate = candidate();
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(sessionRepository.sumBillablePricesByCandidateId(candidateId, BalanceService.BILLABLE_STATUSES))
                .thenReturn(new BigDecimal("180"));
        when(paymentRepository.sumAmountsByCandidateId(candidateId)).thenReturn(new BigDecimal("125"));
        var billableRows = List.of(
                projection(UUID.randomUUID(), LocalDate.of(2026, 1, 1), "100", DrivingSessionStatus.DONE),
                projection(UUID.randomUUID(), LocalDate.of(2026, 1, 2), "80", DrivingSessionStatus.ABSENT)
        );
        when(sessionRepository.findBillableSessionProjections(candidateId, BalanceService.BILLABLE_STATUSES))
                .thenReturn(billableRows);

        var first = drivingSession(billableRows.get(0), DrivingSessionStatus.DONE);
        var second = drivingSession(billableRows.get(1), DrivingSessionStatus.ABSENT);
        when(sessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<DrivingSession>>any(),
                org.mockito.ArgumentMatchers.<Pageable>any()
        )).thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 10), 2));

        var statement = service.getStatement(
                candidateId, null, null, null, null, null, PageRequest.of(0, 10), admin()
        );

        assertThat(statement.content()).extracting("paidAmount")
                .containsExactly(new BigDecimal("100.000"), new BigDecimal("25.000"));
        assertThat(statement.content()).extracting("remaining")
                .containsExactly(new BigDecimal("55.000"), new BigDecimal("55.000"));
        assertThat(statement.totalSessions()).isEqualByComparingTo("180.000");
        assertThat(statement.totalPaid()).isEqualByComparingTo("125.000");
    }

    @Test
    void instructorCannotReadFinancialDataForUnassignedCandidate() {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate()));
        UUID userId = UUID.randomUUID();
        var instructor = new AppUserDetails(userId, "instructor@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
        when(sessionRepository.existsByCandidate_IdAndInstructor_User_Id(candidateId, userId)).thenReturn(false);

        assertThatThrownBy(() -> service.getBalance(candidateId, instructor))
                .isInstanceOf(CandidateFinancialAccessDeniedException.class);
        verify(sessionRepository, never())
                .sumBillablePricesByCandidateId(any(), any());
    }

    @Test
    void missingCandidateIsReportedAsNotFound() {
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBalance(candidateId, admin()))
                .isInstanceOf(CandidateNotFoundException.class);
    }

    private Candidate candidate() {
        Candidate candidate = new Candidate(null, "Ada", "Lovelace", "123", "ada@example.com", null,
                null, LocalDate.now(), CandidateStatus.ACTIVE, null);
        org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", candidateId);
        return candidate;
    }

    private static DrivingSession drivingSession(BillableSessionProjection projection, DrivingSessionStatus status) {
        Candidate candidate = new Candidate(null, "Ada", "Lovelace", "123", "ada@example.com", null,
                null, LocalDate.now(), CandidateStatus.ACTIVE, null);
        com.drivingschool.instructor.Instructor instructor = new com.drivingschool.instructor.Instructor(
                new User("instructor@example.com", "hash", Set.of(Role.INSTRUCTOR)),
                "Grace", "Hopper", "123", "instructor@example.com", "LIC-1",
                com.drivingschool.instructor.InstructorStatus.ACTIVE
        );
        DrivingSession session = new DrivingSession(
                candidate,
                instructor,
                null,
                null,
                projection.getSessionDate(),
                projection.getStartTime(),
                projection.getEndTime(),
                projection.getPrice(),
                null
        );
        org.springframework.test.util.ReflectionTestUtils.setField(session, "id", projection.getSessionId());
        session.changeStatus(status);
        return session;
    }

    private static BillableSessionProjection projection(
            UUID id,
            LocalDate date,
            String price,
            DrivingSessionStatus status
    ) {
        return new BillableSessionProjection() {
            public UUID getSessionId() {
                return id;
            }

            public LocalDate getSessionDate() {
                return date;
            }

            public LocalTime getStartTime() {
                return LocalTime.of(9, 0);
            }

            public LocalTime getEndTime() {
                return LocalTime.of(10, 0);
            }

            public BigDecimal getPrice() {
                return new BigDecimal(price);
            }

            public DrivingSessionStatus getStatus() {
                return status;
            }
        };
    }

    private static AppUserDetails admin() {
        return new AppUserDetails(UUID.randomUUID(), "admin@example.com", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
