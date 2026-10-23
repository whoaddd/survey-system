package com.survey.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record GeneratedSurvey(
        @JsonPropertyDescription("根据用户需求生成的问卷题目列表")
        List<GeneratedQuestion> questions
) {}