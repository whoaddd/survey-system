package com.survey.common;

import java.io.Serializable;

public record ApiResponse<T>(int code, String message, T data, String traceId)
        implements Serializable {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "success", null, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }

    public boolean isSuccess() {
        return code == 0;
    }
}