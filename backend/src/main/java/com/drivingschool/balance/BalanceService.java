package com.drivingschool.balance;

import com.drivingschool.balance.dto.BalanceResponse;
import com.drivingschool.balance.dto.StatementLineResponse;
import com.drivingschool.balance.dto.StatementResponse;
import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateNotFoundException;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.drivingsession.BillableSessionProjection;
import com.drivingschool.drivingsession.DrivingSession;
import com.drivingschool.drivingsession.DrivingSessionRepository;
import com.drivingschool.drivingsession.DrivingSessionStatus;
import com.drivingschool.payment.InvalidPaymentException;
import com.drivingschool.payment.PaymentRepository;
import com.drivingschool.payment.PaymentSpecifications;
import com.drivingschool.payment.dto.PaymentResponse;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BalanceService {

    public static final Set<DrivingSessionStatus> BILLABLE_STATUSES =
            Set.of(DrivingSessionStatus.DONE, DrivingSessionStatus.ABSENT);

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);

    private final CandidateRepository candidateRepository;
    private final DrivingSessionRepository drivingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationService paymentAllocationService;

    public BalanceService(
            CandidateRepository candidateRepository,
            DrivingSessionRepository drivingSessionRepository,
            PaymentRepository paymentRepository,
            PaymentAllocationService paymentAllocationService
    ) {
        this.candidateRepository = candidateRepository;
        this.drivingSessionRepository = drivingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAllocationService = paymentAllocationService;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID candidateId, AppUserDetails actor) {
        requireCandidateAccess(candidateId, actor);
        return calculateBalance(candidateId);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getMyBalance(AppUserDetails actor) {
        return calculateBalance(findCandidateForUser(actor).getId());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPositiveRemaining() {
        List<String> billableStatuses = BILLABLE_STATUSES.stream()
                .map(DrivingSessionStatus::name)
                .toList();
        return money(drivingSessionRepository.sumPositiveRemainingAcrossCandidates(billableStatuses));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(UUID candidateId, Pageable pageable, AppUserDetails actor) {
        requireCandidateAccess(candidateId, actor);
        return paymentRepository.findAll(PaymentSpecifications.search(candidateId, null, null, null), pageable)
                .map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Pageable pageable, AppUserDetails actor) {
        Candidate candidate = findCandidateForUser(actor);
        return paymentRepository.findAll(
                        PaymentSpecifications.search(candidate.getId(), null, null, null),
                        pageable
                )
                .map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public StatementResponse getStatement(
            UUID candidateId,
            LocalDate dateFrom,
            LocalDate dateTo,
            DrivingSessionStatus status,
            UUID instructorId,
            UUID circuitId,
            Pageable pageable,
            AppUserDetails actor
    ) {
        requireCandidateAccess(candidateId, actor);
        return statement(candidateId, dateFrom, dateTo, status, instructorId, circuitId, pageable);
    }

    @Transactional(readOnly = true)
    public StatementResponse getMyStatement(
            LocalDate dateFrom,
            LocalDate dateTo,
            DrivingSessionStatus status,
            UUID instructorId,
            UUID circuitId,
            Pageable pageable,
            AppUserDetails actor
    ) {
        Candidate candidate = findCandidateForUser(actor);
        return statement(candidate.getId(), dateFrom, dateTo, status, instructorId, circuitId, pageable);
    }

    private StatementResponse statement(
            UUID candidateId,
            LocalDate dateFrom,
            LocalDate dateTo,
            DrivingSessionStatus status,
            UUID instructorId,
            UUID circuitId,
            Pageable pageable
    ) {
        validateDateRange(dateFrom, dateTo);
        BalanceResponse balance = calculateBalance(candidateId);
        List<DrivingSessionAllocation> billableSessions = drivingSessionRepository
                .findBillableSessionProjections(candidateId, BILLABLE_STATUSES)
                .stream()
                .map(BalanceService::toAllocation)
                .toList();
        var allocatedAmounts = paymentAllocationService.allocate(billableSessions, balance.totalPaid());

        Page<StatementLineResponse> lines = drivingSessionRepository.findAll(
                        statementSpecification(candidateId, dateFrom, dateTo, status, instructorId, circuitId),
                        pageable
                )
                .map(session -> toStatementLine(session, allocatedAmounts, balance.remaining()));
        return StatementResponse.from(candidateId, lines, balance);
    }

    private static Specification<DrivingSession> statementSpecification(
            UUID candidateId,
            LocalDate dateFrom,
            LocalDate dateTo,
            DrivingSessionStatus status,
            UUID instructorId,
            UUID circuitId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("candidate").get("id"), candidateId));
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("sessionDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("sessionDate"), dateTo));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (instructorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("instructor").get("id"), instructorId));
            }
            if (circuitId != null) {
                predicates.add(criteriaBuilder.equal(root.get("circuit").get("id"), circuitId));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private BalanceResponse calculateBalance(UUID candidateId) {
        BigDecimal totalSessions = money(
                drivingSessionRepository.sumBillablePricesByCandidateId(candidateId, BILLABLE_STATUSES)
        );
        BigDecimal totalPaid = money(paymentRepository.sumAmountsByCandidateId(candidateId));
        BigDecimal remaining = totalSessions.subtract(totalPaid).setScale(3, RoundingMode.HALF_UP);
        return new BalanceResponse(candidateId, totalSessions, totalPaid, remaining, remaining.signum() < 0);
    }

    private void requireCandidateAccess(UUID candidateId, AppUserDetails actor) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new CandidateNotFoundException(candidateId));
        if (hasRole(actor, Role.ADMIN)) {
            return;
        }
        if (hasRole(actor, Role.INSTRUCTOR)
                && drivingSessionRepository.existsByCandidate_IdAndInstructor_User_Id(candidate.getId(), actor.id())) {
            return;
        }
        throw new CandidateFinancialAccessDeniedException();
    }

    private Candidate findCandidateForUser(AppUserDetails actor) {
        if (!hasRole(actor, Role.CANDIDATE)) {
            throw new CandidateFinancialAccessDeniedException();
        }
        return candidateRepository.findByUser_Id(actor.id())
                .orElseThrow(() -> new CandidateNotFoundException(actor.id()));
    }

    private static StatementLineResponse toStatementLine(
            DrivingSession session,
            java.util.Map<UUID, BigDecimal> allocations,
            BigDecimal remaining
    ) {
        BigDecimal hours = BigDecimal.valueOf(Duration.between(session.getStartTime(), session.getEndTime()).toSeconds())
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
        return new StatementLineResponse(
                session.getId(),
                session.getSessionDate().getDayOfWeek(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                hours,
                money(session.getPrice()),
                session.getStatus(),
                allocations.getOrDefault(session.getId(), ZERO),
                remaining
        );
    }

    private static DrivingSessionAllocation toAllocation(BillableSessionProjection session) {
        return new DrivingSessionAllocation(
                session.getSessionId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getPrice(),
                session.getStatus()
        );
    }

    private static BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(3, RoundingMode.HALF_UP);
    }

    private static boolean hasRole(AppUserDetails user, Role role) {
        return user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    private static void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateTo.isBefore(dateFrom)) {
            throw new InvalidPaymentException("dateTo must not be before dateFrom");
        }
    }
}
