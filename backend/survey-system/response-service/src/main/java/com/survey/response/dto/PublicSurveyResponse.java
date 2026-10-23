package com.survey.response.dto;

import java.util.List;

public record PublicSurveyResponse(
        String shareCode,
        String title,
        String description,
        Integer version,
        List<QuestionView> questions
) {
    public record QuestionView(
            Long id,
            String type,
            String title,
            String contentHtml,
            String imageKey,
            Boolean required,
            Boolean scoreEnabled,
            Integer scoreMin,
            Integer scoreMax,
            Integer sortOrder,
            List<OptionView> options
    ) {}

    public record OptionView(Long id, String label, String value, Integer sortOrder) {}
}