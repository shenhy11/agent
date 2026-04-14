package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 查询改写服务
 * 在检索前对用户原始问题进行改写，补充上下文并修正歧义，提升检索召回质量
 */
@Service
public class QueryRewriteService {
    private static final Logger log = LoggerFactory.getLogger(QueryRewriteService.class);


    private final ChatClient chatClient;

    @Value("${agent.rag.query-rewrite-enabled:true}")
    private boolean enabled;

    public QueryRewriteService(ChatClient chatClient) {
        // 这里复用主聊天客户端
        this.chatClient = chatClient;
    }

    /**
     * 重写查询
     * @param originalQuery 原始查询
     * @return 改写后的查询。若未开启，返回原查询。
     */
    public String rewrite(String originalQuery) {
        if (!enabled || originalQuery.length() < 2) {
            return originalQuery;
        }

        long start = System.currentTimeMillis();
        try {
            String prompt = """
                    你是一个智能手表领域的搜索词优化专家。
                    请将用户的简短或模糊查询改写为一个具体的、适合进行文档检索的查询语句。
                    如果用户查询已经非常具体，请直接照原样返回。
                    不要返回任何解释，仅返回改写后的查询语句。
                    
                    原始查询：%s
                    """.formatted(originalQuery);

            String rewritten = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 简单清理下换行和多余空字符
            if (rewritten != null && !rewritten.isBlank()) {
                String cleaned = rewritten.replace("\n", "").trim();
                log.info("Query改写耗时 {}ms: '{}' -> '{}'", (System.currentTimeMillis() - start), originalQuery, cleaned);
                return cleaned;
            }
        } catch (Exception e) {
            log.warn("Query改写失败，降级使用原查询: {}", e.getMessage());
        }

        return originalQuery;
    }
}
