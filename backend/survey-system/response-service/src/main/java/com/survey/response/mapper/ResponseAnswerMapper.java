package com.survey.response.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.survey.response.entity.ResponseAnswer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("response")
public interface ResponseAnswerMapper extends BaseMapper<ResponseAnswer> {
}