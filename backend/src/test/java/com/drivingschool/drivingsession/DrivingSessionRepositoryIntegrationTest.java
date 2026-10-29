package com.drivingschool.drivingsession;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.candidate.CandidateStatus;
import com.drivingschool.balance.BalanceService;
import com.drivingschool.circuit.Circuit;
import com.drivingschool.circuit.CircuitRepository;
import com.drivingschool.instructor.Instructor;
import com.drivingschool.instructor.InstructorRepository;
import com.drivingschool.instructor.InstructorStatus;
import com.drivingschool.payment.Payment;
import com.drivingschool.payment.PaymentMethod;
import com.drivingschool.payment.PaymentRepository;
import com.drivingschool.payment.PaymentSpecifications;
import com.drivingschool.security.AppUserDetails;
import com.drivingschool.drivingsession.dto.CreateDrivingSessionRequest;
import com.drivingschool.drivingsession.dto.ChangeDrivingSessionStatusRequest;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import com.drivingschool.vehicle.Vehicle;
import com.drivingschool.vehicle.VehicleRepository;
import com.drivingschool.vehicle.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DrivingSessionRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("app.jwt.secret", () -> java.util.Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Autowired
    private DrivingSessionRepository sessionRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CircuitRepository circuitRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private DrivingSessionService drivingSessionService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    void appliesAllSearchFiltersAndPagination() {
        Fixture fixture = fixture();
        DrivingSession planned = saveSession(
                fixture,
                LocalDate.now().plusDays(2),
                DrivingSessionStatus.PLANNED,
                fixture.firstCandidate(),
                fixture.firstInstructor(),
                fixture.circuit()
        );
        saveSession(
                fixture,
                LocalDate.now().minusDays(2),
                DrivingSessionStatus.CANCELLED,
                fixture.secondCandidate(),
                fixture.secondInstructor(),
                null
        );

        var all = sessionRepository.search(null, null, null, null, null, null, null, null,
                PageRequest.of(0, 1, Sort.by("sessionDate").descending()));
        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(all.getContent()).hasSize(1);
        assertThat(all.getContent().getFirst().getId()).isEqualTo(planned.getId());

        assertThat(search(LocalDate.now().plusDays(2), LocalDate.now().plusDays(2), null, null, null, null)
                .getTotalElements()).isEqualTo(1);
        assertThat(search(null, null, fixture.firstCandidate().getId(), null, null, null)
                .getTotalElements()).isEqualTo(1);
        assertThat(search(null, null, null, fixture.firstInstructor().getId(), null, null)
                .getTotalElements()).isEqualTo(1);
        assertThat(search(null, null, null, null, DrivingSessionStatus.CANCELLED, null)
                .getTotalElements()).isEqualTo(1);
        assertThat(search(null, null, null, null, null, fixture.circuit().getId())
                .getTotalElements()).isEqualTo(1);
    }

    @Test
    @Transactional
    void databaseRejectsEndTimeNotAfterStartTime() {
        Fixture fixture = fixture();
        DrivingSession invalidSession = new DrivingSession(
                fixture.firstCandidate(),
                fixture.firstInstructor(),
                null,
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(10, 0),
                BigDecimal.TEN,
                null
        );

        assertThatThrownBy(() -> sessionRepository.saveAndFlush(invalidSession))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsOverlappingInstructorSessionsWithoutServiceValidation() {
        Fixture fixture = fixture();
        LocalDate date = LocalDate.now().plusDays(40);
        saveSessionAt(fixture.firstCandidate(), fixture.firstInstructor(), null, date,
                LocalTime.of(10, 0), LocalTime.of(12, 0), DrivingSessionStatus.PLANNED);

        assertThatThrownBy(() -> sessionRepository.saveAndFlush(new DrivingSession(
                fixture.secondCandidate(),
                fixture.firstInstructor(),
                null,
                null,
                date,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                BigDecimal.TEN,
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsOverlappingCandidateSessionsWithoutServiceValidation() {
        Fixture fixture = fixture();
        LocalDate date = LocalDate.now().plusDays(41);
        saveSessionAt(fixture.firstCandidate(), fixture.firstInstructor(), null, date,
                LocalTime.of(10, 0), LocalTime.of(12, 0), DrivingSessionStatus.PLANNED);

        assertThatThrownBy(() -> sessionRepository.saveAndFlush(new DrivingSession(
                fixture.firstCandidate(),
                fixture.secondInstructor(),
                null,
                null,
                date,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                BigDecimal.TEN,
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsOverlappingVehicleSessionsWithoutServiceValidation() {
        Fixture fixture = fixture();
        LocalDate date = LocalDate.now().plusDays(42);
        saveSessionAt(fixture.firstCandidate(), fixture.firstInstructor(), fixture.vehicle(), date,
                LocalTime.of(10, 0), LocalTime.of(12, 0), DrivingSessionStatus.PLANNED);

        assertThatThrownBy(() -> sessionRepository.saveAndFlush(new DrivingSession(
                fixture.secondCandidate(),
                fixture.secondInstructor(),
                fixture.vehicle(),
                null,
                date,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                BigDecimal.TEN,
                null
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseAllowsTouchingSessionsAndReusesCancelledSlots() {
        Fixture fixture = fixture();
        LocalDate date = LocalDate.now().plusDays(43);
        DrivingSession original = saveSessionAt(
                fixture.firstCandidate(), fixture.firstInstructor(), fixture.vehicle(), date,
                LocalTime.of(10, 0), LocalTime.of(12, 0), DrivingSessionStatus.PLANNED
        );

        saveSessionAt(fixture.secondCandidate(), fixture.secondInstructor(), fixture.vehicle(), date,
                LocalTime.of(12, 0), LocalTime.of(14, 0), DrivingSessionStatus.PLANNED);

        drivingSessionService.changeStatus(
                original.getId(),
                new ChangeDrivingSessionStatusRequest(DrivingSessionStatus.CANCELLED, "Cancelled"),
                adminActor()
        );
        saveSessionAt(fixture.firstCandidate(), fixture.firstInstructor(), fixture.vehicle(), date,
                LocalTime.of(10, 0), LocalTime.of(12, 0), DrivingSessionStatus.PLANNED);
    }

    @Test
    void simultaneousServiceCreationsProduceOneSuccessAndOneConflict() throws Exception {
        Fixture fixture = fixture();
        LocalDate date = LocalDate.now().plusDays(44);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        try {
            Future<ConcurrentCreateResult> first = executor.submit(() -> createConcurrently(
                    transactions, ready, start, fixture.firstCandidate(), fixture.firstInstructor(), date
            ));
            Future<ConcurrentCreateResult> second = executor.submit(() -> createConcurrently(
                    transactions, ready, start, fixture.secondCandidate(), fixture.firstInstructor(), date
            ));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .satisfiesExactlyInAnyOrder(
                            success -> assertThat(success.succeeded()).isTrue(),
                            conflict -> {
                                assertThat(conflict.succeeded()).isFalse();
                                assertThat(conflict.conflictingSession()).isNotNull();
                            }
                    );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional
    void recalculatesBalanceAfterPaymentAndSessionChangesWithoutStoredTotals() {
        Fixture fixture = fixture();
        DrivingSession session = saveSession(
                fixture,
                LocalDate.now().minusDays(1),
                DrivingSessionStatus.DONE,
                fixture.firstCandidate(),
                fixture.firstInstructor(),
                null
        );
        AppUserDetails admin = new AppUserDetails(
                java.util.UUID.randomUUID(),
                "admin@example.com",
                "test-hash",
                true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        assertThat(balanceService.getBalance(fixture.firstCandidate().getId(), admin).remaining())
                .isEqualByComparingTo("35.000");

        Payment payment = paymentRepository.saveAndFlush(new Payment(
                fixture.firstCandidate(), new BigDecimal("10.000"), LocalDate.now(), PaymentMethod.CASH, null
        ));
        assertThat(balanceService.getBalance(fixture.firstCandidate().getId(), admin).remaining())
                .isEqualByComparingTo("25.000");

        payment.update(new BigDecimal("20.000"), LocalDate.now(), PaymentMethod.CARD, null);
        paymentRepository.saveAndFlush(payment);
        assertThat(balanceService.getBalance(fixture.firstCandidate().getId(), admin).remaining())
                .isEqualByComparingTo("15.000");

        paymentRepository.delete(payment);
        paymentRepository.flush();
        assertThat(balanceService.getBalance(fixture.firstCandidate().getId(), admin).remaining())
                .isEqualByComparingTo("35.000");

        session.changeStatus(DrivingSessionStatus.PLANNED);
        sessionRepository.saveAndFlush(session);
        assertThat(balanceService.getBalance(fixture.firstCandidate().getId(), admin).totalSessions())
                .isEqualByComparingTo("0.000");

        session.changeStatus(DrivingSessionStatus.ABSENT);
        sessionRepository.saveAndFlush(session);
        assertThat(balanceService.getBalance(fixture.firstCandidate().getId(), admin).totalSessions())
                .isEqualByComparingTo("35.000");
    }

    @Test
    @Transactional
    void statementSearchWorksWhenOptionalFiltersAreOmitted() {
        Fixture fixture = fixture();
        saveSession(
                fixture,
                LocalDate.now().minusDays(2),
                DrivingSessionStatus.DONE,
                fixture.firstCandidate(),
                fixture.firstInstructor(),
                fixture.circuit()
        );
        saveSession(
                fixture,
                LocalDate.now().minusDays(1),
                DrivingSessionStatus.PLANNED,
                fixture.firstCandidate(),
                fixture.firstInstructor(),
                null
        );
        AppUserDetails admin = new AppUserDetails(
                java.util.UUID.randomUUID(),
                "admin@example.com",
                "test-hash",
                true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        var statement = balanceService.getStatement(
                fixture.firstCandidate().getId(),
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10),
                admin
        );

        assertThat(statement.totalElements()).isEqualTo(2);
        assertThat(statement.content()).hasSize(2);
        assertThat(statement.totalSessions()).isEqualByComparingTo("35.000");
    }

    @Test
    @Transactional
    void searchesPaymentsByCandidateDateMethodAndPagination() {
        Fixture fixture = fixture();
        paymentRepository.saveAndFlush(new Payment(
                fixture.firstCandidate(), new BigDecimal("10.000"),
                LocalDate.of(2026, 1, 1), PaymentMethod.CASH, null
        ));
        paymentRepository.saveAndFlush(new Payment(
                fixture.firstCandidate(), new BigDecimal("20.000"),
                LocalDate.of(2026, 1, 2), PaymentMethod.CARD, null
        ));
        paymentRepository.saveAndFlush(new Payment(
                fixture.secondCandidate(), new BigDecimal("30.000"),
                LocalDate.of(2026, 1, 2), PaymentMethod.CARD, null
        ));

        var page = paymentRepository.findAll(
                PaymentSpecifications.search(
                        fixture.firstCandidate().getId(),
                        LocalDate.of(2026, 1, 2),
                        LocalDate.of(2026, 1, 2),
                        PaymentMethod.CARD
                ),
                PageRequest.of(0, 1)
        );
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getAmount()).isEqualByComparingTo("20.000");
    }

    @Test
    @Transactional
    void loadsCandidatePaymentHistoryWithoutOptionalFilters() {
        Fixture fixture = fixture();
        paymentRepository.saveAndFlush(new Payment(
                fixture.firstCandidate(), new BigDecimal("10.000"),
                LocalDate.now(), PaymentMethod.CASH, null
        ));
        AppUserDetails admin = new AppUserDetails(
                java.util.UUID.randomUUID(),
                "admin@example.com",
                "test-hash",
                true,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        var page = balanceService.getPayments(
                fixture.firstCandidate().getId(),
                PageRequest.of(0, 10),
                admin
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
    }

    private org.springframework.data.domain.Page<DrivingSession> search(
            LocalDate dateFrom,
            LocalDate dateTo,
            java.util.UUID candidateId,
            java.util.UUID instructorId,
            DrivingSessionStatus status,
            java.util.UUID circuitId
    ) {
        return sessionRepository.search(dateFrom, dateTo, candidateId, instructorId, status, circuitId,
                null, null, PageRequest.of(0, 10));
    }

    private DrivingSession saveSession(
            Fixture fixture,
            LocalDate date,
            DrivingSessionStatus status,
            Candidate candidate,
            Instructor instructor,
            Circuit circuit
    ) {
        DrivingSession session = new DrivingSession(
                candidate,
                instructor,
                fixture.vehicle(),
                circuit,
                date,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                new BigDecimal("35.000"),
                null
        );
        session.changeStatus(status);
        return sessionRepository.saveAndFlush(session);
    }

    private DrivingSession saveSessionAt(
            Candidate candidate,
            Instructor instructor,
            Vehicle vehicle,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            DrivingSessionStatus status
    ) {
        DrivingSession session = new DrivingSession(
                candidate,
                instructor,
                vehicle,
                null,
                date,
                start,
                end,
                BigDecimal.TEN,
                null
        );
        session.changeStatus(status);
        return sessionRepository.saveAndFlush(session);
    }

    private ConcurrentCreateResult createConcurrently(
            TransactionTemplate transactions,
            CountDownLatch ready,
            CountDownLatch start,
            Candidate candidate,
            Instructor instructor,
            LocalDate date
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent session creation");
        }
        try {
            transactions.execute(transaction -> drivingSessionService.create(
                    new CreateDrivingSessionRequest(
                            candidate.getId(),
                            instructor.getId(),
                            null,
                            null,
                            date,
                            LocalTime.of(10, 0),
                            LocalTime.of(12, 0),
                            BigDecimal.TEN,
                            null
                    ),
                    new AppUserDetails(
                            UUID.randomUUID(),
                            "admin@example.com",
                            "test-hash",
                            true,
                            java.util.List.of(
                                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")
                            )
                    )
            ));
            return new ConcurrentCreateResult(true, null);
        } catch (ScheduleConflictException exception) {
            return new ConcurrentCreateResult(false, exception.getConflictingSession());
        }
    }

    private record ConcurrentCreateResult(boolean succeeded, ScheduleConflictDetails conflictingSession) {
    }

    private Fixture fixture() {
        String suffix = java.util.UUID.randomUUID().toString();
        Candidate firstCandidate = candidateRepository.saveAndFlush(new Candidate(
                null, "Ada", "Lovelace", "111", "ada-" + suffix + "@example.com", null,
                null, LocalDate.now(), CandidateStatus.ACTIVE, null
        ));
        Candidate secondCandidate = candidateRepository.saveAndFlush(new Candidate(
                null, "Alan", "Turing", "222", "alan-" + suffix + "@example.com", null,
                null, LocalDate.now(), CandidateStatus.ACTIVE, null
        ));
        Instructor firstInstructor = instructorRepository.saveAndFlush(instructor("one-" + suffix));
        Instructor secondInstructor = instructorRepository.saveAndFlush(instructor("two-" + suffix));
        Circuit circuit = circuitRepository.saveAndFlush(new Circuit(
                "Circuit " + suffix, null, null, new BigDecimal("35.000"), true
        ));
        Vehicle vehicle = vehicleRepository.saveAndFlush(new Vehicle(
                "REG-" + suffix.substring(0, 8), "Brand", "Model", VehicleType.MANUAL, true
        ));
        return new Fixture(firstCandidate, secondCandidate, firstInstructor, secondInstructor, circuit, vehicle);
    }

    private Instructor instructor(String emailPrefix) {
        String email = emailPrefix + "@example.com";
        User user = userRepository.saveAndFlush(new User(email, "test-hash", Set.of(Role.INSTRUCTOR)));
        return new Instructor(user, "First", "Last", "123", email,
                "LIC-" + java.util.UUID.randomUUID(), InstructorStatus.ACTIVE);
    }

    private static AppUserDetails adminActor() {
        return new AppUserDetails(
                UUID.randomUUID(),
                "admin@example.com",
                "test-hash",
                true,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private record Fixture(
            Candidate firstCandidate,
            Candidate secondCandidate,
            Instructor firstInstructor,
            Instructor secondInstructor,
            Circuit circuit,
            Vehicle vehicle
    ) {
    }
}
