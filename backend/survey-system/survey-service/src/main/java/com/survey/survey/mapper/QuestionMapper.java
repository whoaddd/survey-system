package com.survey.survey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.survey.survey.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}