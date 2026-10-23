package com.survey.response.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_option")
public class OptionRef {
    private Long id;
    private Long questionId;
    private String label;
    private String value;
    private Integer sortOrder;
}