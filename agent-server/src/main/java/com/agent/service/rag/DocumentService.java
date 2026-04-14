package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档服务
 * 负责文档的解析、分块和向量化入库
 * 通过 ChunkingStrategyFactory 支持多种分块策略
 */
@Service
public class DocumentService {
    public DocumentService(VectorStore vectorStore, ChunkingStrategyFactory chunkingStrategyFactory, BM25SearchService bm25SearchService) {
        this.vectorStore = vectorStore;
        this.chunkingStrategyFactory = chunkingStrategyFactory;
        this.bm25SearchService = bm25SearchService;
    }

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);


    private final VectorStore vectorStore;
    private final ChunkingStrategyFactory chunkingStrategyFactory;
    private final BM25SearchService bm25SearchService;

    @Value("${agent.rag.chunking-strategy:token}")
    private String chunkingStrategy;

    /**
     * 上传并处理文档
     * 流程：解析文档 → 分块（根据配置策略）→ 向量化 → 存入 VectorStore
     *
     * @param resource    文件资源
     * @param knowledgeId 知识库 ID
     * @return 处理的文档块数量
     */
    public int ingestDocument(Resource resource, String knowledgeId) {
        return ingestDocument(resource, knowledgeId, this.chunkingStrategy);
    }

    /**
     * 上传并处理文档（指定分块策略）
     *
     * @param resource    文件资源
     * @param knowledgeId 知识库 ID
     * @param strategy    分块策略：token / recursive / semantic
     * @return 处理的文档块数量
     */
    public int ingestDocument(Resource resource, String knowledgeId, String strategy) {
        log.info("开始处理文档: {}, 知识库: {}, 分块策略: {}", resource.getFilename(), knowledgeId, strategy);

        // 1. 使用 Tika 解析文档（支持 PDF/Word/HTML 等多种格式）
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();

        // 2. 为每个文档添加元数据
        documents.forEach(doc -> {
            doc.getMetadata().putAll(Map.of(
                    "knowledge_id", knowledgeId,
                    "source", resource.getFilename(),
                    "ingested_at", System.currentTimeMillis()
            ));
        });

        // 3. 使用策略工厂进行分块
        List<Document> chunks = chunkingStrategyFactory.split(documents, strategy);

        // 4. 向量化并存入 VectorStore，同时也存入 BM25 索引
        vectorStore.add(chunks);
        bm25SearchService.addDocuments(chunks);

        log.info("文档处理完成: {}, 策略: {}, 生成 {} 个文档块", resource.getFilename(), strategy, chunks.size());
        return chunks.size();
    }

    /**
     * 删除指定知识库的所有文档
     *
     * @param knowledgeId 知识库 ID
     */
    public void deleteByKnowledgeId(String knowledgeId) {
        log.info("删除知识库文档: {}", knowledgeId);
        vectorStore.delete(
                List.of("knowledge_id == '" + knowledgeId + "'")
        );
    }
}

