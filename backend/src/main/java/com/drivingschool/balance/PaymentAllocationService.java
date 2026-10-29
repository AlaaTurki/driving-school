package com.drivingschool.balance;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentAllocationService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);

    public Map<UUID, BigDecimal> allocate(
            List<DrivingSessionAllocation> sessions,
            BigDecimal totalPaid
    ) {
        BigDecimal available = money(totalPaid);
        Map<UUID, BigDecimal> allocations = new LinkedHashMap<>();
        List<DrivingSessionAllocation> ordered = sessions.stream()
                .sorted(Comparator.comparing(DrivingSessionAllocation::sessionDate)
                        .thenComparing(DrivingSessionAllocation::startTime)
                        .thenComparing(DrivingSessionAllocation::sessionId))
                .toList();
        for (DrivingSessionAllocation session : ordered) {
            if (!BalanceService.BILLABLE_STATUSES.contains(session.status())) {
                allocations.put(session.sessionId(), ZERO);
                continue;
            }
            BigDecimal price = money(session.price()).max(ZERO);
            BigDecimal allocated = available.min(price);
            allocations.put(session.sessionId(), allocated);
            available = available.subtract(allocated);
        }
        return Map.copyOf(allocations);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(3, RoundingMode.HALF_UP);
    }
}
