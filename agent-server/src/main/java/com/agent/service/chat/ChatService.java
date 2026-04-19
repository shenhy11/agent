package com.agent.service.chat;

import cn.hutool.json.JSONUtil;
import com.agent.model.*;
import com.agent.model.dto.ChatRequest;
import com.agent.model.dto.ChatResponse;
import com.agent.service.agent.RouterAgent;
import com.agent.service.agent.UserProfileService;
import com.agent.service.rag.RagPipelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 对话服务：处理同步/流式对话，并将消息持久化至 PostgreSQL
 */
@Slf4j
@Service
public class ChatService {

    private final RouterAgent routerAgent;
    private final UserProfileService userProfileService;
    private final RagPipelineService ragPipelineService;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final SysUserRepository userRepository;

    public ChatService(RouterAgent routerAgent,
                       UserProfileService userProfileService,
                       RagPipelineService ragPipelineService,
                       ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       SysUserRepository userRepository) {
        this.routerAgent = routerAgent;
        this.userProfileService = userProfileService;
        this.ragPipelineService = ragPipelineService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /**
     * 同步对话：完整响应后持久化用户消息和 AI 回复
     */
    public ChatResponse chat(ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        log.debug("同步对话 - 会话ID: {}, 消息: {}", conversationId, request.getMessage());

        String finalMessage = request.getMessage();
        if (request.isEnableRag()) {
            RagPipelineService.RagPipelineResult result = ragPipelineService.runPipeline(request.getMessage());
            if (result.getDocuments() != null && !result.getDocuments().isEmpty()) {
                String context = result.getDocuments().stream()
                        .map(Document::getText).collect(Collectors.joining("\\n---\\n"));
                finalMessage = request.getMessage()
                        + "\\n\\nContext information is below.\\n---------------------\\n"
                        + context + "\\n---------------------\\nGiven the context information and not prior knowledge, answer the query.";
            }
        }

        ChatClient chatClient = routerAgent.routeChatClient(request.getMessage());
        String responseText = chatClient.prompt()
                .user(finalMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        // 持久化消息
        persistMessages(conversationId, request.getMessage(), responseText);

        return new ChatResponse(conversationId, responseText, "text", null);
    }

    /**
     * 流式对话：SSE 逐步返回，完成后持久化消息
     */
    public Flux<ServerSentEvent<String>> streamChat(ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        log.debug("流式对话 - 会话ID: {}, 消息: {}", conversationId, request.getMessage());

        String finalMessage = request.getMessage();
        Flux<ServerSentEvent<String>> retrievalFlux = Flux.empty();

        if (request.isEnableRag()) {
            try {
                RagPipelineService.RagPipelineResult r = ragPipelineService.runPipeline(request.getMessage());
                if (r.getDocuments() != null && !r.getDocuments().isEmpty()) {
                    String context = r.getDocuments().stream()
                            .map(Document::getText).collect(Collectors.joining("\\n---\\n"));
                    finalMessage = request.getMessage()
                            + "\\n\\nContext information is below.\\n---------------------\\n"
                            + context + "\\n---------------------\\nGiven the context information and not prior knowledge, answer the query.";
                    retrievalFlux = Flux.just(ServerSentEvent.<String>builder()
                            .event("retrieval").data(JSONUtil.toJsonStr(r.getDocuments())).build());
                }
            } catch (Exception e) {
                log.error("RAG pipeline 失败", e);
            }
        }

        StringBuilder fullResponse = new StringBuilder();
        String userMessage = request.getMessage();

        ChatClient chatClient = routerAgent.routeChatClient(request.getMessage());
        Flux<ServerSentEvent<String>> llmFlux = chatClient.prompt()
                .user(finalMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream().chatResponse()
                .map(resp -> {
                    String content = "";
                    if (resp.getResult() != null && resp.getResult().getOutput() != null) {
                        content = resp.getResult().getOutput().getText();
                        if (content != null) fullResponse.append(content);
                    }
                    return ServerSentEvent.<String>builder()
                            .event("message").data(content != null ? content : "").build();
                });

        return Flux.concat(retrievalFlux, llmFlux).doOnComplete(() -> {
            // 流式完成后持久化消息
            try {
                persistMessages(conversationId, userMessage, fullResponse.toString());
            } catch (Exception e) {
                log.warn("流式对话消息持久化失败: {}", e.getMessage());
            }
            // 异步更新用户画像
            new Thread(() -> userProfileService.extractAndUpdateProfile(conversationId, userMessage)).start();
        });
    }

    /**
     * 将用户消息和 AI 回复持久化至数据库
     */
    private void persistMessages(String conversationId, String userText, String assistantText) {
        try {
            // 获取当前登录用户 ID（未登录则跳过持久化）
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return;
            }
            Long userId = userRepository.findByUsername(auth.getName())
                    .map(SysUser::getId).orElse(null);

            // 查找或创建会话
            ChatSession session = sessionRepository.findById(parseSessionId(conversationId))
                    .orElseGet(() -> {
                        String title = userText.length() > 30 ? userText.substring(0, 30) + "…" : userText;
                        ChatSession s = ChatSession.builder()
                                .userId(userId)
                                .title(title)
                                .module("chat")
                                .build();
                        return sessionRepository.save(s);
                    });

            // 保存用户消息
            messageRepository.save(ChatMessage.builder()
                    .session(session).role("user").content(userText).build());
            // 保存 AI 回复
            messageRepository.save(ChatMessage.builder()
                    .session(session).role("assistant").content(assistantText).build());
        } catch (Exception e) {
            log.warn("消息持久化失败（不影响业务）: {}", e.getMessage());
        }
    }

    /**
     * 尝试将 conversationId 解析为 Long，否则返回 -1（触发新建会话）
     */
    private Long parseSessionId(String conversationId) {
        try {
            return Long.parseLong(conversationId);
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * 解析或生成会话 ID
     */
    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return conversationId;
    }
}
