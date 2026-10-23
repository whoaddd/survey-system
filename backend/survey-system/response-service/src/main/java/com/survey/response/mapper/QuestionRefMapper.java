package com.survey.response.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.survey.response.entity.QuestionRef;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("survey")
public interface QuestionRefMapper extends BaseMapper<QuestionRef> {
}