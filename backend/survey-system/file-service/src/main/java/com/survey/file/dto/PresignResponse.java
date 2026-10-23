package com.survey.file.dto;

public record PresignResponse(
        String objectKey,      // 服务端生成的最终 object_key
        String uploadUrl,      // 预签名 PUT URL
        long expiresInSeconds  // 有效期
) {}