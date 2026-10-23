package com.survey.response.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question")
public class QuestionRef {
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
}