package com.survey.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.user.dto.LoginRequest;
import com.survey.user.dto.LoginResponse;
import com.survey.user.dto.RegisterRequest;
import com.survey.user.entity.User;
import com.survey.user.mapper.UserMapper;
import com.survey.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
    public LoginResponse register(RegisterRequest request) {
        // 1. 用户名格式校验
        String username = request.username() == null ? "" : request.username().trim();
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "用户名需为 3-20 位字母、数字或下划线");
        }

        // 2. 密码强度校验
        String password = request.password() == null ? "" : request.password();
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "密码长度需为 6-32 位");
        }

        // 3. 用户名重复预检（唯一索引兜底）
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "用户名已存在");
        }

        // 4. 插入用户
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setRole("PUBLISHER");
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发场景下唯一索引兜底
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "用户名已存在");
        }

        // 5. 注册即登录，直接返回 token
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRole());
    }
}