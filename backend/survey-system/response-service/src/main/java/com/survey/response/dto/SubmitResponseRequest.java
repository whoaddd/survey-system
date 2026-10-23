package com.survey.response.dto;

import java.util.List;

public record SubmitResponseRequest(
        String shareCode,
        String respondentKey,
        List<AnswerItem> answers
) {
    public record AnswerItem(
            Long questionId,
            String answerText,
            List<Long> selectedOptionIds,
            Integer ratingValue,
            String attachmentKey
    ) {}
}