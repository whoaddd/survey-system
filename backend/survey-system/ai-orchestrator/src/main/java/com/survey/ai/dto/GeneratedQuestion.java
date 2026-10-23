package com.survey.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record GeneratedQuestion(
        @JsonPropertyDescription("题型，只能是 TEXT / SINGLE / MULTI / RATING 之一")
        String type,

        @JsonPropertyDescription("题干内容，简洁明确，不超过 200 字")
        String title,

        @JsonPropertyDescription("选项列表，仅当题型为 SINGLE 或 MULTI 时提供，至少 2 个；其他题型传空数组")
        List<String> options
) {}