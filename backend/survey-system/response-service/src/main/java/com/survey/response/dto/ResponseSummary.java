package com.survey.response.dto;

import java.time.LocalDateTime;

public record ResponseSummary(
        Long id,
        String receiptCode,
        String respondentKey,
        LocalDateTime submittedAt
) {}