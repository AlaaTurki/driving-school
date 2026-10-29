package com.drivingschool.dashboard;

import com.drivingschool.candidate.Candidate;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.candidate.CandidateStatus;
import com.drivingschool.dashboard.dto.DashboardChartPoint;
import com.drivingschool.drivingsession.DrivingSession;
import com.drivingschool.drivingsession.DrivingSessionRepository;
import com.drivingschool.drivingsession.DrivingSessionStatus;
import com.drivingschool.instructor.Instructor;
import com.drivingschool.instructor.InstructorRepository;
import com.drivingschool.instructor.InstructorStatus;
import com.drivingschool.payment.Payment;
import com.drivingschool.payment.PaymentMethod;
import com.drivingschool.payment.PaymentRepository;
import com.drivingschool.user.Role;
import com.drivingschool.user.User;
import com.drivingschool.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DashboardIntegrationTest {

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
    private DashboardService dashboardService;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private DrivingSessionRepository drivingSessionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void aggregatesKnownAdminAndInstructorDashboardDataInPostgres() {
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Tunis"));
        String marker = UUID.randomUUID().toString();
        Candidate firstCandidate = createCandidate(marker, "first");
        Candidate secondCandidate = createCandidate(marker, "second");
        Instructor instructor = createInstructor(marker);

        saveSession(firstCandidate, instructor, today, LocalTime.of(8, 0), LocalTime.of(9, 0),
                "20.000", DrivingSessionStatus.DONE);
        saveSession(firstCandidate, instructor, today.minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                "10.000", DrivingSessionStatus.ABSENT);
        saveSession(secondCandidate, instructor, today.minusDays(20), LocalTime.of(10, 0), LocalTime.of(11, 0),
                "40.000", DrivingSessionStatus.CANCELLED);

        for (int day = 1; day <= 11; day++) {
            saveSession(day % 2 == 0 ? firstCandidate : secondCandidate, instructor, today.plusDays(day),
                    LocalTime.of(8, 0), LocalTime.of(9, 0), "15.000", DrivingSessionStatus.PLANNED);
        }
        paymentRepository.save(new Payment(
                firstCandidate, new BigDecimal("12.000"), today, PaymentMethod.CASH, null
        ));
        paymentRepository.save(new Payment(
                secondCandidate, new BigDecimal("50.000"), today.minusMonths(1), PaymentMethod.CARD, null
        ));

        var admin = dashboardService.getAdminDashboard();

        int yesterdayInWeek = today.getDayOfWeek().getValue() == 1 ? 0 : 1;
        assertThat(admin.candidatesCount()).isEqualTo(2);
        assertThat(admin.instructorsCount()).isEqualTo(1);
        assertThat(admin.sessionsToday()).isEqualTo(1);
        assertThat(admin.sessionsThisWeek())
                .isEqualTo(1 + yesterdayInWeek + Math.min(11, 7 - today.getDayOfWeek().getValue()));
        assertThat(admin.sessionsDone()).isEqualTo(1);
        assertThat(admin.sessionsPlanned()).isEqualTo(11);
        assertThat(admin.totalPaid()).isEqualByComparingTo("62.000");
        assertThat(admin.totalRemaining()).isEqualByComparingTo("18.000");
        assertThat(admin.sessionsPerDay()).hasSize(14);
        assertThat(admin.sessionsPerDay().getLast().value()).isEqualByComparingTo("1");
        assertThat(admin.sessionsPerDay().stream()
                .map(DashboardChartPoint::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("2");
        assertThat(admin.paymentsPerMonth()).hasSize(6);
        assertThat(admin.paymentsPerMonth().stream()
                .map(DashboardChartPoint::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("62.000");
        assertThat(admin.sessionsByStatus().stream()
                .map(com.drivingschool.dashboard.dto.DashboardStatusCount::value)
                .toList())
                .containsExactly(11L, 1L, 1L, 1L);

        var instructorDashboard = dashboardService.getInstructorDashboard(instructor.getUser().getId());
        assertThat(instructorDashboard.todaySessions()).hasSize(1);
        assertThat(instructorDashboard.upcomingSessions()).hasSize(10);
        assertThat(instructorDashboard.candidatesCount()).isEqualTo(2);
        assertThat(instructorDashboard.hoursTaught().total()).isEqualByComparingTo("1.00");
        assertThat(instructorDashboard.hoursTaught().currentMonth()).isEqualByComparingTo("1.00");
    }

    private Candidate createCandidate(String marker, String suffix) {
        User user = userRepository.save(new User(
                marker + "-candidate-" + suffix + "@example.com",
                "encoded-password",
                Set.of(Role.CANDIDATE)
        ));
        return candidateRepository.save(new Candidate(
                user,
                suffix,
                "Candidate",
                "+21612345678",
                marker + "-candidate-profile-" + suffix + "@example.com",
                null,
                null,
                LocalDate.now(),
                CandidateStatus.ACTIVE,
                null
        ));
    }

    private Instructor createInstructor(String marker) {
        User user = userRepository.save(new User(
                marker + "-instructor@example.com",
                "encoded-password",
                Set.of(Role.INSTRUCTOR)
        ));
        return instructorRepository.save(new Instructor(
                user,
                "Test",
                "Instructor",
                "+21612345678",
                marker + "-instructor-profile@example.com",
                marker,
                InstructorStatus.ACTIVE
        ));
    }

    private void saveSession(
            Candidate candidate,
            Instructor instructor,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String price,
            DrivingSessionStatus status
    ) {
        DrivingSession session = new DrivingSession(
                candidate,
                instructor,
                null,
                null,
                date,
                start,
                end,
                new BigDecimal(price),
                null
        );
        session.changeStatus(status);
        drivingSessionRepository.save(session);
    }
}
