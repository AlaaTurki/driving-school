package com.drivingschool.vehicle.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record VehiclePageResponse(
        List<VehicleResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static VehiclePageResponse from(Page<VehicleResponse> page) {
        return new VehiclePageResponse(
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
