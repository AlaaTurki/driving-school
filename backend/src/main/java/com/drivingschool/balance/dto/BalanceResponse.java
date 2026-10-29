package com.drivingschool.balance.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
        UUID candidateId,
        BigDecimal totalSessions,
        BigDecimal totalPaid,
        BigDecimal remaining,
        boolean credit
) {
}
