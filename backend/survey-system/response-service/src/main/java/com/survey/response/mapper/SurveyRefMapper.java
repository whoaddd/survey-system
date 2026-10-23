package com.survey.response.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.survey.response.entity.SurveyRef;
import org.apache.ibatis.annotations.Mapper;

/**
 * survey_db 只读。
 * 注意：继承 BaseMapper 会暴露 insert/update/delete 方法，
 * 但本服务约定不调用它们。这是动态数据源的既有能力，
 * 靠代码约定而不是技术手段限制。
 */
@Mapper
@DS("survey")
public interface SurveyRefMapper extends BaseMapper<SurveyRef> {
}