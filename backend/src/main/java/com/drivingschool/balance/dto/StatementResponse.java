package com.drivingschool.balance.dto;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StatementResponse(
        UUID candidateId,
        List<StatementLineResponse> content,
        BigDecimal totalSessions,
        BigDecimal totalPaid,
        BigDecimal remaining,
        boolean credit,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static StatementResponse from(
            UUID candidateId,
            Page<StatementLineResponse> page,
            BalanceResponse balance
    ) {
        return new StatementResponse(
                candidateId,
                page.getContent(),
                balance.totalSessions(),
                balance.totalPaid(),
                balance.remaining(),
                balance.credit(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
