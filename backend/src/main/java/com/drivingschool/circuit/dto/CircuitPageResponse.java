package com.drivingschool.circuit.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record CircuitPageResponse(
        List<CircuitResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static CircuitPageResponse from(Page<CircuitResponse> page) {
        return new CircuitPageResponse(
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
