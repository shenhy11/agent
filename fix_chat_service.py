import os
import re

filepath = r"d:\code\agent\agent-server\src\main\java\com\agent\service\chat\ChatService.java"
with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Add imports if not exist
if "import com.agent.service.rag.RagPipelineService;" not in content:
    content = content.replace("import com.agent.service.agent.RouterAgent;", 
                              "import com.agent.service.agent.RouterAgent;\nimport com.agent.service.rag.RagPipelineService;\nimport org.springframework.ai.document.Document;\nimport java.util.stream.Collectors;")

# Add RagPipelineService field and update constructor
if "private final RagPipelineService ragPipelineService;" not in content:
    # Constructor
    content = content.replace(
        "public ChatService(RouterAgent routerAgent, com.agent.service.agent.UserProfileService userProfileService) {",
        "public ChatService(RouterAgent routerAgent, com.agent.service.agent.UserProfileService userProfileService, RagPipelineService ragPipelineService) {"
    )
    content = content.replace(
        "this.userProfileService = userProfileService;\n    }",
        "this.userProfileService = userProfileService;\n        this.ragPipelineService = ragPipelineService;\n    }"
    )
    # Field
    content = content.replace(
        "private final com.agent.service.agent.UserProfileService userProfileService;",
        "private final com.agent.service.agent.UserProfileService userProfileService;\n    private final RagPipelineService ragPipelineService;"
    )

# Fix chat method (sync)
sync_chat = """    public ChatResponse chat(ChatRequest request) {
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
    }"""
content = re.sub(r'public ChatResponse chat\(ChatRequest request\) \{.*?\n    \}', sync_chat, content, flags=re.DOTALL)

# Fix streamChat method
stream_chat = """    public Flux<ServerSentEvent<String>> streamChat(ChatRequest request) {
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
                String content = response.getResult().getOutput().getContent();
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
    }"""
content = re.sub(r'public Flux<ServerSentEvent<String>> streamChat\(ChatRequest request\) \{.*?\n    \}', stream_chat, content, flags=re.DOTALL)

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)
