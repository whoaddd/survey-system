package com.survey.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.survey.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}