package com.survey.survey.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long surveyId;
    private Integer version;
    private String type;
    private String title;
    private String contentHtml;
    private String imageKey;
    private Integer required;
    private Integer scoreEnabled;
    private Integer scoreMin;
    private Integer scoreMax;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}