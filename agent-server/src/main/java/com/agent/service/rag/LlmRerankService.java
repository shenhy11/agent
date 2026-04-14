package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * LLM Reranking (重排序) 服务
 * 对召回的候选文档使用 LLM 进行二级排序，打分 1-10
 */
@Service
public class LlmRerankService {
    private static final Logger log = LoggerFactory.getLogger(LlmRerankService.class);


    private final ChatClient chatClient;
    private final ExecutorService executorService;

    @Value("${agent.rag.rerank-enabled:true}")
    private boolean enabled;

    @Value("${agent.rag.rerank-top-n:3}")
    private int rerankTopN;

    @Value("${agent.rag.rerank-timeout-ms:5000}")
    private long rerankTimeoutMs;

    public LlmRerankService(ChatClient chatClient) {
        this.chatClient = chatClient;
        // 使用缓存线程池并行给文档打分
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 对检索结果重新排序
     * @param query 原始用户查询
     * @param documents 候选文档列表
     * @return 重新排序并截断后的前 N 个文档
     */
    public List<Document> rerank(String query, List<Document> documents) {
        if (!enabled || documents.isEmpty()) {
            return documents.stream().limit(rerankTopN).toList();
        }

        long start = System.currentTimeMillis();
        List<CompletableFuture<DocumentScore>> futures = new ArrayList<>();

        for (Document doc : documents) {
            CompletableFuture<DocumentScore> future = CompletableFuture.supplyAsync(() -> {
                double score = evaluateScore(query, doc.getText());
                return new DocumentScore(doc, score);
            }, executorService);
            futures.add(future);
        }

        List<Document> rerankedResults;
        try {
            // 等待所有评分完成或超时
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.get(rerankTimeoutMs, TimeUnit.MILLISECONDS);

            rerankedResults = futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparing(DocumentScore::score).reversed())
                    .limit(rerankTopN)
                    .map(ds -> {
                        ds.doc().getMetadata().put("rerank_score", ds.score());
                        return ds.doc();
                    })
                    .toList();

            log.info("LLM Rerank 耗时 {}ms, 返回 Top-{}", (System.currentTimeMillis() - start), rerankTopN);
            return rerankedResults;

        } catch (TimeoutException e) {
            log.warn("LLM Rerank 超时 (>{}), 降级使用传入文档排序", rerankTimeoutMs);
            return documents.stream().limit(rerankTopN).toList();
        } catch (Exception e) {
            log.error("LLM Rerank 失败，降级使用原排序", e);
            return documents.stream().limit(rerankTopN).toList();
        }
    }

    /**
     * 调用 LLM 对单个文档与查询的相关性打分 (1-10)
     */
    private double evaluateScore(String query, String docContent) {
        try {
            String prompt = """
                    请作为一个严格的判卷专家，评估给定文档对用户查询的解决程度，并打分(1到10分)。
                    
                    标准：
                    1-3分：文档完全无关或几乎没有帮助。
                    4-6分：文档有一些相关信息，但不能直接回答问题。
                    7-8分：文档高度相关，可以回答问题的大部分内容。
                    9-10分：文档完美且直接地回答了用户问题。
                    
                    只返回一个数字（例如 8），不要返回任何其他内容。
                    
                    用户查询：%s
                    
                    文档内容：%s
                    """.formatted(query, docContent);

            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (result != null) {
                // 提取数字
                String number = result.replaceAll("[^0-9.]", "").trim();
                return Double.parseDouble(number);
            }
        } catch (Exception e) {
            // 失败时给个默认低分，不影响整体流程
            return 1.0;
        }
        return 1.0;
    }

    private record DocumentScore(Document doc, double score) {}
}
