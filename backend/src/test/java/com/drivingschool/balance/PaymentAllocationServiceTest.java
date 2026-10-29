package com.drivingschool.balance;

import com.drivingschool.drivingsession.DrivingSessionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAllocationServiceTest {

    private final PaymentAllocationService allocator = new PaymentAllocationService();

    @Test
    void allocatesPartialPaymentChronologicallyAndIgnoresNonBillableSessions() {
        var first = session(LocalDate.of(2026, 1, 1), LocalTime.of(9, 0), "100", DrivingSessionStatus.DONE);
        var second = session(LocalDate.of(2026, 1, 2), LocalTime.of(9, 0), "80", DrivingSessionStatus.ABSENT);
        var planned = session(LocalDate.of(2026, 1, 3), LocalTime.of(9, 0), "90", DrivingSessionStatus.PLANNED);
        var cancelled = session(LocalDate.of(2026, 1, 4), LocalTime.of(9, 0), "50", DrivingSessionStatus.CANCELLED);

        var allocation = allocator.allocate(List.of(cancelled, planned, second, first), new BigDecimal("125"));

        assertThat(allocation.get(first.sessionId())).isEqualByComparingTo("100.000");
        assertThat(allocation.get(second.sessionId())).isEqualByComparingTo("25.000");
        assertThat(allocation.get(planned.sessionId())).isEqualByComparingTo("0.000");
        assertThat(allocation.get(cancelled.sessionId())).isEqualByComparingTo("0.000");
    }

    @Test
    void exactPaymentCoversNWholeSessionsAndExcessPaymentCreatesCreditWithoutOverAllocating() {
        var first = session(LocalDate.of(2026, 1, 1), LocalTime.NOON, "56", DrivingSessionStatus.DONE);
        var second = session(LocalDate.of(2026, 1, 2), LocalTime.NOON, "56", DrivingSessionStatus.DONE);
        var absent = session(LocalDate.of(2026, 1, 3), LocalTime.NOON, "56", DrivingSessionStatus.ABSENT);

        var exact = allocator.allocate(List.of(absent, second, first), new BigDecimal("112"));
        assertThat(exact.get(first.sessionId())).isEqualByComparingTo("56.000");
        assertThat(exact.get(second.sessionId())).isEqualByComparingTo("56.000");
        assertThat(exact.get(absent.sessionId())).isEqualByComparingTo("0.000");

        var overpaid = allocator.allocate(List.of(first, second, absent), new BigDecimal("200"));
        assertThat(overpaid.values()).containsExactlyInAnyOrder(
                new BigDecimal("56.000"), new BigDecimal("56.000"), new BigDecimal("56.000")
        );
    }

    private static DrivingSessionAllocation session(
            LocalDate date,
            LocalTime time,
            String price,
            DrivingSessionStatus status
    ) {
        return new DrivingSessionAllocation(
                UUID.randomUUID(),
                date,
                time,
                time.plusHours(1),
                new BigDecimal(price),
                status
        );
    }
}
