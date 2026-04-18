package com.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /** 响应码 */
    private String code;

    /** 响应消息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 成功响应 */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /** 成功响应（无数据） */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    /** 业务错误响应 */
    public static <T> ApiResult<T> error(ErrorCode errorCode) {
        return new ApiResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 业务错误响应（自定义消息） */
    public static <T> ApiResult<T> error(ErrorCode errorCode, String message) {
        return new ApiResult<>(errorCode.getCode(), message, null);
    }
}
