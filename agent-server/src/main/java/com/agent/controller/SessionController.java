package com.agent.controller;

import com.agent.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 对话会话控制器：查询历史会话和消息
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SysUserRepository userRepository;

    /**
     * 获取当前用户的历史会话列表（按更新时间倒序）
     */
    @GetMapping
    public ApiResult<List<Map<String, Object>>> listSessions(Authentication auth) {
        String username = auth.getName();
        // 通过用户名查 userId
        return userRepository.findByUsername(username).map(user -> {
            List<Map<String, Object>> sessions = sessionRepository
                    .findByUserIdOrderByUpdatedAtDesc(user.getId())
                    .stream()
                    .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "title", s.getTitle() != null ? s.getTitle() : "新会话",
                        "module", s.getModule() != null ? s.getModule() : "chat",
                        "createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : "",
                        "updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : ""
                    ))
                    .toList();
            return ApiResult.success(sessions);
        }).orElse(ApiResult.success(List.of()));
    }

    /**
     * 获取指定会话的消息列表（按时间正序）
     */
    @GetMapping("/{sessionId}/messages")
    public ApiResult<List<Map<String, Object>>> listMessages(
            @PathVariable Long sessionId,
            Authentication auth) {
        List<Map<String, Object>> messages = messageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> Map.<String, Object>of(
                    "id", m.getId(),
                    "role", m.getRole(),
                    "content", m.getContent(),
                    "createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : ""
                ))
                .toList();
        return ApiResult.success(messages);
    }
}
