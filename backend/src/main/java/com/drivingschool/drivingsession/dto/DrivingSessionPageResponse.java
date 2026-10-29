package com.drivingschool.drivingsession.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record DrivingSessionPageResponse(
        List<DrivingSessionResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static DrivingSessionPageResponse from(Page<DrivingSessionResponse> page) {
        return new DrivingSessionPageResponse(
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
