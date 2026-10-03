package com.drivingschool.candidate.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record CandidatePageResponse(
        List<CandidateResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static CandidatePageResponse from(Page<CandidateResponse> page) {
        return new CandidatePageResponse(
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
