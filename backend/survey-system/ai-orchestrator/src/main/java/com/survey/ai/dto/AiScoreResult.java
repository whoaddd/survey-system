package com.survey.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AiScoreResult(
        @JsonPropertyDescription("评分，1-5 的整数，1 为最差，5 为最好")
        int rating,
        @JsonPropertyDescription("评分理由，简明扼要，不超过 100 字")
        String reason,
        @JsonPropertyDescription("置信度，0.0-1.0 之间，模型对该评分的把握程度")
        double confidence
) {}