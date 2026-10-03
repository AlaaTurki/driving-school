package com.drivingschool.instructor.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record InstructorPageResponse(
        List<InstructorResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static InstructorPageResponse from(Page<InstructorResponse> page) {
        return new InstructorPageResponse(
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
