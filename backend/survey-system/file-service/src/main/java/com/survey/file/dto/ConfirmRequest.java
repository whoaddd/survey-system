package com.survey.file.dto;

public record ConfirmRequest(
        String objectKey,
        String bizType,
        Long bizId,
        String mimeType,
        Long size
) {}