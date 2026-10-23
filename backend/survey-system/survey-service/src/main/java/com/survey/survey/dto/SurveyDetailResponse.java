package com.survey.survey.dto;

import java.time.LocalDateTime;

public record SurveyDetailResponse(
        Long id,
        String title,
        String description,
        String status,
        String shareCode,
        String shareUrl,
        Integer version,
        Integer publishedVersion,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
) {
}