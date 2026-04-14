package com.agent.service.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.dto.ChatRequest;
import com.agent.model.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import com.agent.service.agent.RouterAgent;
import com.agent.service.rag.RagPipelineService;
import org.springframework.ai.document.Document;
import java.util.stream.Collectors;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import cn.hutool.json.JSONUtil;

/**
 * 对话服务
 * 负责处理用户对话请求，支持同步和流式响应
 */
@Service
public class ChatService {
    public ChatService(RouterAgent routerAgent, com.agent.service.agent.UserProfileService userProfileService, RagPipelineService ragPipelineService) {
        this.routerAgent = routerAgent;
        this.userProfileService = userProfileService;
        this.ragPipelineService = ragPipelineService;
    }

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);


    private final RouterAgent routerAgent;
    private final com.agent.service.agent.UserProfileService userProfileService;
    private final RagPipelineService ragPipelineService;

    /**
     * 同步对话
     * 等待完整响应后一次性返回
     */
        public ChatResponse chat(ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        log.debug("同步对话 - 会话ID: {}, 消息: {}", conversationId, request.getMessage());

        String finalMessage = request.getMessage();
        if (request.isEnableRag()) {
            RagPipelineService.RagPipelineResult pipelineResult = ragPipelineService.runPipeline(request.getMessage());
            if (pipelineResult.getDocuments() != null && !pipelineResult.getDocuments().isEmpty()) {
                String context = pipelineResult.getDocuments().stream().map(Document::getText).collect(Collectors.joining("\\n---\\n"));
                finalMessage = request.getMessage() + "\\n\\nContext information is below.\\n---------------------\\n" + context + "\\n---------------------\\nGiven the context information and not prior knowledge, answer the query.";
            }
        }

        ChatClient chatClient = routerAgent.routeChatClient(request.getMessage());
        String response = chatClient.prompt()
                .user(finalMessage)
                .advisors(advisorSpec -> advisorSpec
                        .param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return new ChatResponse(conversationId, response, "text", null);
    }

    /**
     * 流式对话
     * 通过 SSE 逐步返回响应内容
     */
        public Flux<ServerSentEvent<String>> streamChat(ChatRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        log.debug("流式对话 - 会话ID: {}, 消息: {}", conversationId, request.getMessage());

        String finalMessage = request.getMessage();
        Flux<ServerSentEvent<String>> retrievalFlux = Flux.empty();
        
        if (request.isEnableRag()) {
            try {
                RagPipelineService.RagPipelineResult pipelineResult = ragPipelineService.runPipeline(request.getMessage());
                if (pipelineResult.getDocuments() != null && !pipelineResult.getDocuments().isEmpty()) {
                    String context = pipelineResult.getDocuments().stream().map(Document::getText).collect(Collectors.joining("\\n---\\n"));
                    finalMessage = request.getMessage() + "\\n\\nContext information is below.\\n---------------------\\n" + context + "\\n---------------------\\nGiven the context information and not prior knowledge, answer the query.";
                    
                    String docsJson = JSONUtil.toJsonStr(pipelineResult.getDocuments());
                    retrievalFlux = Flux.just(ServerSentEvent.<String>builder()
                            .event("retrieval")
                            .data(docsJson)
                            .build());
                }
            } catch(Exception e) {
                log.error("RAG pipeline failed", e);
            }
        }

        ChatClient chatClient = routerAgent.routeChatClient(request.getMessage());

        Flux<org.springframework.ai.chat.model.ChatResponse> responseFlux = chatClient.prompt()
                .user(finalMessage)
                .advisors(advisorSpec -> advisorSpec
                        .param(org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .chatResponse();

        Flux<ServerSentEvent<String>> llmFlux = responseFlux.map(response -> {
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String content = response.getResult().getOutput().getText();
                return ServerSentEvent.<String>builder()
                        .event("message")
                        .data(content != null ? content : "")
                        .build();
            }
            return ServerSentEvent.<String>builder().event("message").data("").build();
        });
        
        return Flux.concat(retrievalFlux, llmFlux).doOnComplete(() -> {
            new Thread(() -> {
                userProfileService.extractAndUpdateProfile(conversationId, request.getMessage());
            }).start();
        });
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
