package com.drivingschool.dashboard;

import com.drivingschool.balance.BalanceService;
import com.drivingschool.candidate.CandidateRepository;
import com.drivingschool.dashboard.dto.AdminDashboardResponse;
import com.drivingschool.dashboard.dto.DashboardChartPoint;
import com.drivingschool.dashboard.dto.DashboardStatusCount;
import com.drivingschool.dashboard.dto.InstructorDashboardResponse;
import com.drivingschool.drivingsession.DrivingSessionRepository;
import com.drivingschool.drivingsession.DrivingSessionStatus;
import com.drivingschool.instructor.InstructorRepository;
import com.drivingschool.payment.PaymentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.drivingschool.drivingsession.DrivingSessionStatus.PLANNED;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM", Locale.FRENCH);

    private final CandidateRepository candidateRepository;
    private final InstructorRepository instructorRepository;
    private final DrivingSessionRepository drivingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final BalanceService balanceService;
    private final Clock clock;

    public DashboardService(
            CandidateRepository candidateRepository,
            InstructorRepository instructorRepository,
            DrivingSessionRepository drivingSessionRepository,
            PaymentRepository paymentRepository,
            BalanceService balanceService,
            @Qualifier("dashboardClock") Clock clock
    ) {
        this.candidateRepository = candidateRepository;
        this.instructorRepository = instructorRepository;
        this.drivingSessionRepository = drivingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.balanceService = balanceService;
        this.clock = clock;
    }

    public AdminDashboardResponse getAdminDashboard() {
        DashboardPeriods periods = DashboardPeriods.current(clock);
        Map<DrivingSessionStatus, Long> countsByStatus = new EnumMap<>(DrivingSessionStatus.class);
        drivingSessionRepository.countSessionsByStatus().forEach(row ->
                countsByStatus.put(row.getStatus(), row.getSessionCount())
        );

        return new AdminDashboardResponse(
                candidateRepository.count(),
                instructorRepository.count(),
                drivingSessionRepository.countBySessionDate(periods.today()),
                drivingSessionRepository.countBySessionDateBetween(periods.weekStart(), periods.weekEnd()),
                countsByStatus.getOrDefault(DrivingSessionStatus.DONE, 0L),
                countsByStatus.getOrDefault(PLANNED, 0L),
                money(paymentRepository.sumAllAmounts()),
                balanceService.getTotalPositiveRemaining(),
                buildSessionsPerDay(periods),
                buildPaymentsPerMonth(periods),
                buildSessionsByStatus(countsByStatus)
        );
    }

    public InstructorDashboardResponse getInstructorDashboard(UUID userId) {
        DashboardPeriods periods = DashboardPeriods.current(clock);
        LocalDate thisMonthStart = periods.today().withDayOfMonth(1);
        var hours = drivingSessionRepository.findInstructorHours(
                userId,
                thisMonthStart,
                thisMonthStart.plusMonths(1)
        );
        var todaySessions = drivingSessionRepository
                .findAllByInstructor_User_IdAndSessionDateOrderByStartTimeAsc(userId, periods.today())
                .stream()
                .map(com.drivingschool.dashboard.dto.DashboardInstructorSession::from)
                .toList();
        var upcomingSessions = drivingSessionRepository.findUpcomingForInstructor(
                        userId,
                        PLANNED,
                        periods.today(),
                        DashboardPeriods.currentTime(clock),
                        PageRequest.of(0, 10)
                ).stream()
                .map(com.drivingschool.dashboard.dto.DashboardInstructorSession::from)
                .toList();

        return new InstructorDashboardResponse(
                todaySessions,
                upcomingSessions,
                drivingSessionRepository.countDistinctCandidatesForInstructor(userId),
                new InstructorDashboardResponse.HoursTaught(
                        hours(hours.getTotalHours()),
                        hours(hours.getMonthHours())
                )
        );
    }

    private List<DashboardChartPoint> buildSessionsPerDay(DashboardPeriods periods) {
        Map<LocalDate, Long> values = new HashMap<>();
        drivingSessionRepository.countSessionsByDay(periods.daysStart(), periods.today()).forEach(row ->
                values.put(row.getSessionDate(), row.getSessionCount())
        );
        List<LocalDate> days = java.util.stream.IntStream.range(0, DashboardPeriods.DAYS_IN_CHART)
                .mapToObj(periods.daysStart()::plusDays)
                .toList();
        long maximum = days.stream().mapToLong(day -> values.getOrDefault(day, 0L)).max().orElse(0L);
        return days.stream()
                .map(day -> new DashboardChartPoint(
                        day.format(DAY_LABEL),
                        BigDecimal.valueOf(values.getOrDefault(day, 0L)),
                        height(values.getOrDefault(day, 0L), maximum)
                ))
                .toList();
    }

    private List<DashboardChartPoint> buildPaymentsPerMonth(DashboardPeriods periods) {
        Map<LocalDate, BigDecimal> values = paymentRepository
                .sumPaymentsByMonth(periods.monthStart(), periods.nextMonthStart())
                .stream()
                .collect(Collectors.toMap(
                        PaymentRepository.PaymentMonthTotal::getMonthStart,
                        PaymentRepository.PaymentMonthTotal::getTotal
                ));
        List<LocalDate> months = java.util.stream.IntStream.range(0, DashboardPeriods.MONTHS_IN_CHART)
                .mapToObj(periods.monthStart()::plusMonths)
                .toList();
        BigDecimal maximum = months.stream().map(month -> values.getOrDefault(month, BigDecimal.ZERO))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        return months.stream()
                .map(month -> {
                    BigDecimal value = money(values.getOrDefault(month, BigDecimal.ZERO));
                    return new DashboardChartPoint(month.format(MONTH_LABEL), value, height(value, maximum));
                })
                .toList();
    }

    private static List<DashboardStatusCount> buildSessionsByStatus(Map<DrivingSessionStatus, Long> values) {
        long maximum = Arrays.stream(DrivingSessionStatus.values())
                .mapToLong(status -> values.getOrDefault(status, 0L))
                .max()
                .orElse(0L);
        return Arrays.stream(DrivingSessionStatus.values())
                .map(status -> {
                    long value = values.getOrDefault(status, 0L);
                    return new DashboardStatusCount(status, statusLabel(status), value, height(value, maximum));
                })
                .toList();
    }

    private static String statusLabel(DrivingSessionStatus status) {
        return switch (status) {
            case PLANNED -> "Planifiées";
            case DONE -> "Terminées";
            case ABSENT -> "Absents";
            case CANCELLED -> "Annulées";
        };
    }

    private static int height(long value, long maximum) {
        if (maximum == 0) {
            return 5;
        }
        return Math.max(5, BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maximum), 0, RoundingMode.HALF_UP)
                .intValue());
    }

    private static int height(BigDecimal value, BigDecimal maximum) {
        if (maximum.signum() == 0) {
            return 5;
        }
        return Math.max(5, value.multiply(BigDecimal.valueOf(100))
                .divide(maximum, 0, RoundingMode.HALF_UP)
                .intValue());
    }

    private static BigDecimal hours(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(3, RoundingMode.HALF_UP);
    }
}
