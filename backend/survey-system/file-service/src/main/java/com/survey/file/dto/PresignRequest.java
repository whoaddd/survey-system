package com.survey.file.dto;

public record PresignRequest(
        String bizType,        // QUESTION / RESPONSE / SURVEY
        Long bizId,            // 目标业务 id（可为 null，confirm 时再填）
        String filename,       // 原始文件名（用于生成 object_key 后缀）
        String mimeType,       // image/png, image/jpeg
        Long size              // 字节数
) {}