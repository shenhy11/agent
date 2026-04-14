package com.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.dto.ApiResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return ApiResult.error(400, message);
    }

    /**
     * AI 调用异常
     */
    @ExceptionHandler(org.springframework.ai.retry.NonTransientAiException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResult<Void> handleAiException(Exception e) {
        log.error("AI 服务调用失败: {}", e.getMessage(), e);
        return ApiResult.error(503, "AI 服务暂时不可用，请稍后重试");
    }

    /**
     * 通用异常兜底
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统内部错误: {}", e.getMessage(), e);
        return ApiResult.error(500, "系统内部错误: " + e.getMessage());
    }
}
