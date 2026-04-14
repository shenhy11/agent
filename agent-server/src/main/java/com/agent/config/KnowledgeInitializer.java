package com.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.service.rag.ChunkingStrategyFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库初始化器
 * 启动时自动加载 docs/knowledge/ 下的预置文档到 VectorStore
 * 支持通过配置切换分块策略（token/recursive/semantic）
 */
@Component
public class KnowledgeInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeInitializer.class);
    public KnowledgeInitializer(VectorStore vectorStore, ChunkingStrategyFactory chunkingStrategyFactory, com.agent.service.rag.BM25SearchService bm25SearchService) {
        this.vectorStore = vectorStore;
        this.chunkingStrategyFactory = chunkingStrategyFactory;
        this.bm25SearchService = bm25SearchService;
    }


    private final VectorStore vectorStore;
    private final ChunkingStrategyFactory chunkingStrategyFactory;
    private final com.agent.service.rag.BM25SearchService bm25SearchService;

    @Value("${agent.rag.chunking-strategy:token}")
    private String chunkingStrategy;

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 开始加载预置知识库文档（分块策略: {}）==========", chunkingStrategy);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;

        try {
            resources = resolver.getResources("classpath:knowledge/*.*");
        } catch (Exception e) {
            // 如果 classpath 下没有，尝试从文件系统加载
            try {
                resources = resolver.getResources("file:docs/knowledge/*.*");
            } catch (Exception ex) {
                log.warn("未找到预置知识库文档，跳过初始化");
                return;
            }
        }

        if (resources.length == 0) {
            log.warn("知识库目录为空，跳过初始化");
            return;
        }

        List<Document> allDocuments = new ArrayList<>();

        for (Resource resource : resources) {
            try {
                String filename = resource.getFilename();
                log.info("正在处理文档: {}", filename);

                // Tika 解析文档
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                List<Document> documents = reader.get();

                // 添加元数据
                documents.forEach(doc -> {
                    doc.getMetadata().putAll(Map.of(
                            "knowledge_id", "preset",
                            "source", filename,
                            "type", "preset_knowledge"
                    ));
                });

                allDocuments.addAll(documents);
                log.info("文档 {} 解析完成", filename);

            } catch (Exception e) {
                log.error("处理文档失败: {}", resource.getFilename(), e);
            }
        }

        if (!allDocuments.isEmpty()) {
            // 使用策略工厂进行分块
            List<Document> allChunks = chunkingStrategyFactory.split(allDocuments, chunkingStrategy);
            // 同步写入向量库和 BM25 索引
            vectorStore.add(allChunks);
            bm25SearchService.addDocuments(allChunks);
            
            log.info("========== 知识库初始化完成: 共加载 {} 个文档块（策略: {}）==========",
                    allChunks.size(), chunkingStrategy);
        }
    }
}

