package com.survey.common;

/**
 * 全局错误码。前 3 位对应 HTTP 语义，后 2 位为业务序号。
 */
public enum ErrorCode {

    PARAM_INVALID(40001, "参数错误"),
    UNAUTHORIZED(40101, "未登录或登录已过期"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "重复提交"),
    TOO_MANY_REQUESTS(42901, "请求过于频繁"),
    SYSTEM_ERROR(50001, "系统错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}