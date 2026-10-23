package com.survey.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attachment_ref")
public class AttachmentRef {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private String objectKey;
    private String mimeType;
    private Long size;
    private Long uploaderId;
    private LocalDateTime createdAt;
}