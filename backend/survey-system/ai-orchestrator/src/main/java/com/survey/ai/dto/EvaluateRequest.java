package com.survey.ai.dto;

public record EvaluateRequest(
        Long questionId,
        String questionTitle,
        String answerText
) {}