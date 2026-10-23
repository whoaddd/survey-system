package com.survey.response.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.survey.response.entity.Response;
import org.apache.ibatis.annotations.Mapper;

/**
 * response_db 的读写。
 * 注意 @DS("response") 对应 application.yml 中 dynamic.datasource.datasource.response。
 */
@Mapper
@DS("response")
public interface ResponseMapper extends BaseMapper<Response> {
}