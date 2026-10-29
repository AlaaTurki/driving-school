package com.drivingschool.payment.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaymentPageResponse(
        List<PaymentResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static PaymentPageResponse from(Page<PaymentResponse> page) {
        return new PaymentPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
