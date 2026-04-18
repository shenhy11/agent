package com.agent.controller;

import com.agent.model.dto.ApiResult;
import com.agent.model.dto.ContactRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 联系我们 API 控制器
 * 接收并处理用户提交的联系表单
 */
@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    /**
     * 提交联系表单
     * POST /api/contact
     */
    @PostMapping
    public ApiResult<Map<String, Object>> submitContact(
            @Valid @RequestBody ContactRequest request) {
        log.info("收到联系表单: name={}, email={}, subject={}",
                request.getName(), request.getEmail(), request.getSubject());

        // 模拟处理（Demo 不落库，仅返回成功）
        String ticketId = "CT-" + System.currentTimeMillis();
        String receivedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return ApiResult.success(Map.of(
                "ticketId", ticketId,
                "receivedAt", receivedAt,
                "message", "感谢您的留言！我们会在 24 小时内回复您。"
        ));
    }
}
