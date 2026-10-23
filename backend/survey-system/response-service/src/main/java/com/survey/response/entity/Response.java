package com.survey.response.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("response")
public class Response {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long surveyId;
    private Integer surveyVersion;
    private String respondentKey;
    private String receiptCode;
    private String ipHash;
    private String userAgent;
    private LocalDateTime submittedAt;
}