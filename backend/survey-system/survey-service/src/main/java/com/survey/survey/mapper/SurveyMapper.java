package com.survey.survey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.survey.survey.entity.Survey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SurveyMapper extends BaseMapper<Survey> {
}