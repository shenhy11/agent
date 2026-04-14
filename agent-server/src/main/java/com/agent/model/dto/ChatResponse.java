package com.agent.model.dto;

/**
 * 对话响应 DTO
 */
public class ChatResponse {

    private String conversationId;
    private String content;
    private String type = "text";
    private TokenUsage tokenUsage;

    public ChatResponse() {}

    public ChatResponse(String conversationId, String content, String type, TokenUsage tokenUsage) {
        this.conversationId = conversationId;
        this.content = content;
        this.type = type;
        this.tokenUsage = tokenUsage;
    }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public TokenUsage getTokenUsage() { return tokenUsage; }
    public void setTokenUsage(TokenUsage tokenUsage) { this.tokenUsage = tokenUsage; }

    public static class TokenUsage {
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;

        public TokenUsage() {}
        public TokenUsage(long promptTokens, long completionTokens, long totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public long getPromptTokens() { return promptTokens; }
        public void setPromptTokens(long promptTokens) { this.promptTokens = promptTokens; }
        public long getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(long completionTokens) { this.completionTokens = completionTokens; }
        public long getTotalTokens() { return totalTokens; }
        public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }
    }
}
