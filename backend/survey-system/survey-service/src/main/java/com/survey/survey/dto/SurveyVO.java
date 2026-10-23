package com.survey.survey.dto;

import com.survey.survey.entity.Survey;

import java.time.LocalDateTime;

public record SurveyVO(
        Long id,
        String title,
        String description,
        String status,
        Integer version,
        String shareCode,
        LocalDateTime createdAt
) {
    public static SurveyVO from(Survey s) {
        return new SurveyVO(
                s.getId(),
                s.getTitle(),
                s.getDescription(),
                s.getStatus(),
                s.getVersion(),
                s.getShareCode(),
                s.getCreatedAt()
        );
    }
}