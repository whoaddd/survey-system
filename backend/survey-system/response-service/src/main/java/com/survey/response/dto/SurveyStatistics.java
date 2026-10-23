package com.survey.response.dto;

import java.util.List;
import java.util.Map;

public record SurveyStatistics(
        long totalResponses,
        List<QuestionStatistic> questions
) {
    public record QuestionStatistic(
            Long questionId,
            String title,
            String type,
            Double averageScore,
            Map<String, Long> scoreDistribution,
            Map<String, Long> optionDistribution,
            List<String> textAnswers
    ) {}
}