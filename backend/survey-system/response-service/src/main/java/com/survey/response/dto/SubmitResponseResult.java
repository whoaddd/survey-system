package com.survey.response.dto;

import java.time.LocalDateTime;

public record SubmitResponseResult(String receiptCode, LocalDateTime submittedAt) {}