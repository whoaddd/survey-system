package com.survey.survey.dto;

import java.util.List;

public record SaveSurveyRequest(
        String title,
        String description,
        List<QuestionDto> questions
) {
    public record QuestionDto(
            String type,
            String title,
            String contentHtml,
            String imageKey,
            Boolean required,
            Boolean scoreEnabled,
            Integer scoreMin,
            Integer scoreMax,
            List<OptionDto> options
    ) {}

    public record OptionDto(
            String label,
            String value
    ) {}
}