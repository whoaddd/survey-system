package com.survey.response.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("response_answer")
public class ResponseAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long responseId;
    private Long questionId;
    private String answerText;
    private String selectedOptionIds;  // JSON 字符串，存 [1,2,3]
    private Integer ratingValue;
    private String attachmentKey;
    private LocalDateTime createdAt;
}