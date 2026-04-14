package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 * 组合 VectorStore(密集检索) 和 BM25SearchService(稀疏检索) 两路召回
 * 并使用 RRF(Reciprocal Rank Fusion) 算法进行融合排序
 */
@Service
public class HybridSearchService {
    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);


    private final VectorStore vectorStore;
    private final BM25SearchService bm25SearchService;

    @Value("${agent.rag.hybrid-search-enabled:true}")
    private boolean hybridSearchEnabled;

    @Value("${agent.rag.top-k:5}")
    private int topK;

    @Value("${agent.rag.similarity-threshold:0.6}")
    private double similarityThreshold;

    @Value("${agent.rag.rrf-k:60}")
    private int rrfK;

    public HybridSearchService(VectorStore vectorStore, BM25SearchService bm25SearchService) {
        this.vectorStore = vectorStore;
        this.bm25SearchService = bm25SearchService;
    }

    /**
     * 执行混合检索
     *
     * @param query 查询内容
     * @return 融合排序后的文档列表（只取前 topK 个）
     */
    public List<Document> search(String query) {
        log.info("执行检索，query='{}', hybridEnabled={}", query, hybridSearchEnabled);

        // 1. 向量检索通路
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        List<Document> vectorResults = vectorStore.similaritySearch(searchRequest);
        log.info("向量检索召回 {} 条记录", vectorResults.size());
        
        // 给向量结果标记来源
        vectorResults.forEach(doc -> doc.getMetadata().put("search_type", "vector"));

        // 如果未开启混合检索，直接返回向量结果
        if (!hybridSearchEnabled) {
            return vectorResults;
        }

        // 2. BM25 检索通路
        List<Document> bm25Results = bm25SearchService.search(query, topK);
        log.info("BM25检索召回 {} 条记录", bm25Results.size());

        // 3. RRF 融合排序
        return rrfFusion(vectorResults, bm25Results);
    }

    /**
     * RRF (Reciprocal Rank Fusion) 倒数排名融合算法
     * 公式: Score = 1 / (k + rank)
     *
     * @param listA 结果列表 A（如向量检索）
     * @param listB 结果列表 B（如 BM25 检索）
     * @return 融合排序后的新列表
     */
    private List<Document> rrfFusion(List<Document> listA, List<Document> listB) {
        Map<String, Document> docMap = new HashMap<>();
        Map<String, Double> rrfScores = new HashMap<>();

        // 处理列表 A
        for (int i = 0; i < listA.size(); i++) {
            Document doc = listA.get(i);
            String id = doc.getId();
            docMap.put(id, doc);
            rrfScores.put(id, 1.0 / (rrfK + i + 1));
        }

        // 处理列表 B
        for (int i = 0; i < listB.size(); i++) {
            Document doc = listB.get(i);
            String id = doc.getId();
            
            // 如果两个列表都包含该文档，合并其 metadata (比如把 bm25_score 也塞进去)
            if (docMap.containsKey(id)) {
                Document existingDoc = docMap.get(id);
                Map<String, Object> mergedMeta = new HashMap<>(existingDoc.getMetadata());
                mergedMeta.put("search_type", "hybrid");
                if (doc.getMetadata().containsKey("bm25_score")) {
                    mergedMeta.put("bm25_score", doc.getMetadata().get("bm25_score"));
                }
                Document newDoc = new Document(id, existingDoc.getText(), mergedMeta);
                docMap.put(id, newDoc);
            } else {
                docMap.put(id, doc);
            }

            // 累加 RRF 分数
            rrfScores.put(id, rrfScores.getOrDefault(id, 0.0) + (1.0 / (rrfK + i + 1)));
        }

        // 按 RRF 分数降序排序，并取 topK
        List<Document> fusedResults = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    Document doc = docMap.get(entry.getKey());
                    // 将 RRF 分数存入 metadata 便于之后观察
                    doc.getMetadata().put("rrf_score", entry.getValue());
                    return doc;
                })
                .collect(Collectors.toList());

        log.info("RRF 融合完成，合并后结果数: {}，最终返回 Top {}", docMap.size(), fusedResults.size());
        return fusedResults;
    }
}
