package com.agent.interceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 限流拦截器
 * 基于 Bucket4j 令牌桶算法，对高频 AI 接口进行保护：
 * - NLP / Vision 接口：每分钟 30 次
 * - Chat 接口：每分钟 60 次
 * - 其他接口不限流
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 按 IP + 接口类型 维护独立令牌桶 */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI();
        String ip = extractIp(request);

        // 仅对 AI 核心接口限流
        if (!isRateLimited(path)) return true;

        String bucketKey = ip + ":" + resolveGroup(path);
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(path));

        if (bucket.tryConsume(1)) {
            return true;
        }

        // 超限：返回 429
        log.warn("限流触发: IP={}, path={}", ip, path);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"RATE_LIMIT\",\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
        return false;
    }

    /** 判断路径是否需要限流 */
    private boolean isRateLimited(String path) {
        return path.startsWith("/api/v1/nlp/")
                || path.startsWith("/api/v1/ner/")
                || path.startsWith("/api/v1/vision/")
                || path.startsWith("/api/v1/multimodal/")
                || path.startsWith("/api/v1/image-search/")
                || path.startsWith("/api/v1/chat/");
    }

    /** 推断路径分组（同组共享限额） */
    private String resolveGroup(String path) {
        if (path.startsWith("/api/v1/chat/")) return "chat";
        return "ai";
    }

    /** 根据分组创建令牌桶 */
    private Bucket createBucket(String path) {
        String group = resolveGroup(path);
        // chat 接口：每分钟 60 次
        if ("chat".equals(group)) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1))))
                    .build();
        }
        // NLP / Vision 接口：每分钟 30 次
        return Bucket.builder()
                .addLimit(Bandwidth.classic(30, Refill.greedy(30, Duration.ofMinutes(1))))
                .build();
    }

    /** 提取客户端 IP（支持反向代理） */
    private String extractIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
