package com.survey.response.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("survey")
public class SurveyRef {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String shareCode;
    private Integer publishedVersion;
    private Long creatorId;
}