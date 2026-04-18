package com.agent.advisor;

import com.agent.model.AuditLog;
import com.agent.model.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计切面：自动拦截 AI 接口调用，写入 audit_log 表
 * 切入点为所有 /api/v1/ 下非认证/管理的 Controller 方法
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    /** 切入所有 Service 层的 AI 调用（nlp / vision / rag / agent / chat 子包） */
    @Around("execution(* com.agent.service..*(..))" +
            " && !execution(* com.agent.service.UserService.*(..))")
    public Object auditAiCall(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long elapsed = System.currentTimeMillis() - start;

        try {
            // 从 SecurityContext 获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";

            // 推断模块名（取 service 包名最后一段）
            String className = pjp.getTarget().getClass().getSimpleName();
            String module = inferModule(className);
            String action = className + "." + pjp.getSignature().getName();

            // 获取请求 IP
            String ip = extractIp();

            // 异步写入审计日志（不阻塞业务）
            AuditLog log = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .module(module)
                    .detail("耗时: " + elapsed + "ms")
                    .ip(ip)
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // 审计失败不影响业务
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
        return result;
    }

    /** 根据类名推断模块 */
    private String inferModule(String className) {
        String lower = className.toLowerCase();
        if (lower.contains("chat"))   return "chat";
        if (lower.contains("nlp") || lower.contains("text")) return "nlp";
        if (lower.contains("vision") || lower.contains("image") || lower.contains("multimodal")) return "vision";
        if (lower.contains("rag") || lower.contains("knowledge") || lower.contains("document")) return "rag";
        if (lower.contains("agent") || lower.contains("react")) return "agent";
        return "other";
    }

    /** 从 HttpServletRequest 提取客户端 IP */
    private String extractIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
