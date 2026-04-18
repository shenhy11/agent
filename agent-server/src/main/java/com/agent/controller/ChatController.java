package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.dto.ApiResult;
import com.agent.model.dto.ChatRequest;
import com.agent.model.dto.ChatResponse;
import com.agent.service.chat.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 对话 API 控制器
 * 提供同步对话和 SSE 流式对话接口
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);


    private final ChatService chatService;

    /**
     * 同步对话接口
     * POST /api/chat
     */
    @PostMapping
    public ApiResult<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到对话请求: {}", request.getMessage());
        ChatResponse response = chatService.chat(request);
        return ApiResult.success(response);
    }

    /**
     * SSE 流式对话接口（POST 方式）
     * POST /api/chat/stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("收到流式对话请求(POST): {}", request.getMessage());
        return chatService.streamChat(request);
    }

    /**
     * SSE 流式对话接口（GET 方式）
     * GET /api/chat/stream?message=xxx&conversationId=xxx
     * 兼容前端 Demo 页面的 fetch GET 请求
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChatGet(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        log.info("收到流式对话请求(GET): message={}, conversationId={}", message, conversationId);
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        request.setConversationId(conversationId);
        return chatService.streamChat(request);
    }
}

