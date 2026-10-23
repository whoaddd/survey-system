package com.survey.common.constant;

/**
 * 通用常量。S2 起 JWT、TraceId 等会用到。
 */
public final class CommonConstants {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";

    private CommonConstants() {
    }
}