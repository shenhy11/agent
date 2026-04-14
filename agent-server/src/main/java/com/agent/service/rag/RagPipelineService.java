package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Builder;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 四阶高级检索管道编排服务
 * 步骤：Query 改写 -> 混合检索(双路召回 + RRF融合) -> LLM Rerank
 */
@Service
public class RagPipelineService {
    public RagPipelineService(QueryRewriteService queryRewriteService, HybridSearchService hybridSearchService, LlmRerankService llmRerankService) {
        this.queryRewriteService = queryRewriteService;
        this.hybridSearchService = hybridSearchService;
        this.llmRerankService = llmRerankService;
    }

    private static final Logger log = LoggerFactory.getLogger(RagPipelineService.class);


    private final QueryRewriteService queryRewriteService;
    private final HybridSearchService hybridSearchService;
    private final LlmRerankService llmRerankService;

    /**
     * 运行检索管道并记录每步耗时
     * @param originalQuery 原始用户查询
     * @return 检索到的文档列表与诊断信息
     */
    public RagPipelineResult runPipeline(String originalQuery) {
        long pipelineStart = System.currentTimeMillis();
        RagPipelineResult result = new RagPipelineResult();
        
        // 1. Query 改写
        long start1 = System.currentTimeMillis();
        String rewrittenQuery = queryRewriteService.rewrite(originalQuery);
        long time1 = System.currentTimeMillis() - start1;
        result.setQueryRewriteMs(time1);
        result.setRewrittenQuery(rewrittenQuery);

        // 2. 混合检索（双路召回 + RRF 融合）
        long start2 = System.currentTimeMillis();
        List<Document> candidates = hybridSearchService.search(rewrittenQuery);
        long time2 = System.currentTimeMillis() - start2;
        result.setRetrievalMs(time2);
        
        if (candidates.isEmpty()) {
            result.setDocuments(List.of());
            result.setTotalMs(System.currentTimeMillis() - pipelineStart);
            return result;
        }

        // 3. LLM Rerank
        long start3 = System.currentTimeMillis();
        List<Document> finalDocuments = llmRerankService.rerank(rewrittenQuery, candidates);
        long time3 = System.currentTimeMillis() - start3;
        result.setRerankMs(time3);
        
        result.setDocuments(finalDocuments);
        result.setTotalMs(System.currentTimeMillis() - pipelineStart);
        
        log.info("RAG Pipeline 完成. 总耗时 {}ms (改写:{}ms, 检索:{}ms, 重排:{}ms)", 
                result.getTotalMs(), time1, time2, time3);
        
        return result;
    }
    public static class RagPipelineResult {
        private String rewrittenQuery;
        private List<Document> documents;
        private long queryRewriteMs;
        private long retrievalMs;
        private long rerankMs;
        private long totalMs;

        public String getRewrittenQuery() { return rewrittenQuery; }
        public void setRewrittenQuery(String rewrittenQuery) { this.rewrittenQuery = rewrittenQuery; }

        public List<Document> getDocuments() { return documents; }
        public void setDocuments(List<Document> documents) { this.documents = documents; }

        public long getQueryRewriteMs() { return queryRewriteMs; }
        public void setQueryRewriteMs(long queryRewriteMs) { this.queryRewriteMs = queryRewriteMs; }

        public long getRetrievalMs() { return retrievalMs; }
        public void setRetrievalMs(long retrievalMs) { this.retrievalMs = retrievalMs; }

        public long getRerankMs() { return rerankMs; }
        public void setRerankMs(long rerankMs) { this.rerankMs = rerankMs; }

        public long getTotalMs() { return totalMs; }
        public void setTotalMs(long totalMs) { this.totalMs = totalMs; }
    }
}
