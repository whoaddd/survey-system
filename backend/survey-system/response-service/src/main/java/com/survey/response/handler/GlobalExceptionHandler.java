package com.survey.response.handler;

import com.survey.common.ApiResponse;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception e) {
        // 关键：把完整堆栈打出来，后面所有 500 都靠这条日志定位
        log.error("未捕获异常", e);
        return ApiResponse.fail(ErrorCode.SYSTEM_ERROR);
    }
}