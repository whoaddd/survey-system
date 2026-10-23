package com.survey.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_evaluation")
public class AiEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long responseId;
    private Long questionId;
    private String modelName;
    private String promptVersion;
    private Integer rating;
    private String reason;
    private String suggestion;
    private BigDecimal confidence;
    private String status;
    private LocalDateTime createdAt;
}