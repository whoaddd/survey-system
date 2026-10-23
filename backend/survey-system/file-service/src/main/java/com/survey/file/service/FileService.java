package com.survey.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.file.dto.*;
import com.survey.file.entity.AttachmentRef;
import com.survey.file.mapper.AttachmentRefMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_BIZ = Set.of("QUESTION", "RESPONSE", "SURVEY");

    private final MinioClient minioClient;
    private final AttachmentRefMapper attachmentRefMapper;
    private final SecureRandom random = new SecureRandom();

    @Value("${minio.bucket}")
    private String bucket;
    @Value("${minio.presign-expire-seconds}")
    private long expireSeconds;
    @Value("${minio.max-file-size}")
    private long maxFileSize;

    /** 1. 生成预签名上传 URL */
    public PresignResponse presign(PresignRequest req) {
        validateMime(req.mimeType());
        validateSize(req.size());
        validateBizType(req.bizType());

        String objectKey = buildObjectKey(req.bizType(), req.filename());
        try {
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry((int) expireSeconds, TimeUnit.SECONDS)
                            .build());
            return new PresignResponse(objectKey, uploadUrl, expireSeconds);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "预签名生成失败");
        }
    }

    /** 2. 上传成功后登记 attachment_ref */
    @Transactional(rollbackFor = Exception.class)
    public ConfirmResponse confirm(ConfirmRequest req, Long userId) {
        if (req.objectKey() == null || req.objectKey().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "objectKey 不能为空");
        }
        validateMime(req.mimeType());
        validateSize(req.size());
        validateBizType(req.bizType());

        // 幂等：同一 objectKey 重复 confirm 直接返回已有记录
        AttachmentRef exists = attachmentRefMapper.selectOne(
                new LambdaQueryWrapper<AttachmentRef>()
                        .eq(AttachmentRef::getObjectKey, req.objectKey()));
        if (exists != null) {
            return new ConfirmResponse(exists.getId(), exists.getObjectKey());
        }

        AttachmentRef ref = new AttachmentRef();
        ref.setBizType(req.bizType());
        ref.setBizId(req.bizId());
        ref.setObjectKey(req.objectKey());
        ref.setMimeType(req.mimeType());
        ref.setSize(req.size());
        ref.setUploaderId(userId);
        ref.setCreatedAt(LocalDateTime.now());
        attachmentRefMapper.insert(ref);

        return new ConfirmResponse(ref.getId(), ref.getObjectKey());
    }

    /** 3. 换取下载/展示用的预签名 GET URL */
    public FileUrlResponse getUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "objectKey 不能为空");
        }
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry((int) expireSeconds, TimeUnit.SECONDS)
                            .build());
            return new FileUrlResponse(url, expireSeconds);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "签名下载 URL 生成失败");
        }
    }

    // ===== 私有工具 =====

    private String buildObjectKey(String bizType, String filename) {
        // 结构：bizType/yyyy/MM/dd/uuid.ext
        String ext = "";
        if (filename != null) {
            int dot = filename.lastIndexOf('.');
            if (dot > 0) ext = filename.substring(dot).toLowerCase();
        }
        String uuid = String.format("%08x%08x", random.nextInt(), random.nextInt());
        LocalDateTime now = LocalDateTime.now();
        return String.format("%s/%04d/%02d/%02d/%s%s",
                bizType.toLowerCase(), now.getYear(), now.getMonthValue(),
                now.getDayOfMonth(), uuid, ext);
    }

    private void validateMime(String mime) {
        if (mime == null || !ALLOWED_MIME.contains(mime)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "仅支持 " + ALLOWED_MIME + " 类型");
        }
    }

    private void validateSize(Long size) {
        if (size == null || size <= 0 || size > maxFileSize) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "文件大小非法，最大 " + (maxFileSize / 1024 / 1024) + "MB");
        }
    }

    private void validateBizType(String bizType) {
        if (bizType == null || !ALLOWED_BIZ.contains(bizType)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "bizType 非法：" + bizType);
        }
    }
}