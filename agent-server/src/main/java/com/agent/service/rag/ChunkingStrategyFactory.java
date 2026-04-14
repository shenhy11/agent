package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分块策略工厂
 * 支持 3 种分块策略：token（固定大小）、recursive（递归字符）、semantic（语义）
 */
@Component
public class ChunkingStrategyFactory {
    private static final Logger log = LoggerFactory.getLogger(ChunkingStrategyFactory.class);


    @Value("${agent.rag.chunk-size:800}")
    private int chunkSize;

    @Value("${agent.rag.chunk-overlap:200}")
    private int chunkOverlap;

    @Value("${agent.rag.semantic-threshold:0.5}")
    private double semanticThreshold;

    private final EmbeddingModel embeddingModel;

    public ChunkingStrategyFactory(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 根据策略名称创建分块器并执行分块
     *
     * @param documents 待分块的原始文档
     * @param strategy  策略名称：token / recursive / semantic
     * @return 分块后的文档列表
     */
    public List<Document> split(List<Document> documents, String strategy) {
        log.info("使用 {} 策略分块，chunkSize={}, overlap={}", strategy, chunkSize, chunkOverlap);

        return switch (strategy.toLowerCase()) {
            case "recursive" -> {
                RecursiveCharacterSplitter splitter = new RecursiveCharacterSplitter(chunkSize, chunkOverlap);
                yield splitter.apply(documents);
            }
            case "semantic" -> {
                SemanticSplitter splitter = new SemanticSplitter(embeddingModel, semanticThreshold, chunkSize);
                yield splitter.apply(documents);
            }
            default -> {
                // 默认 token 策略
                TokenTextSplitter splitter = new TokenTextSplitter(
                        chunkSize, chunkOverlap, 5, 10000, true
                );
                yield splitter.apply(documents);
            }
        };
    }
}
