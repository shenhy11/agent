package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BM25 关键词检索服务
 * 使用 Lucene 内存索引，集成 SmartCN 中文分析器
 */
@Service
public class BM25SearchService {
    private static final Logger log = LoggerFactory.getLogger(BM25SearchService.class);


    private final Directory directory;
    private final Analyzer analyzer;
    private IndexWriter indexWriter;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;

    public BM25SearchService() {
        this.directory = new ByteBuffersDirectory();
        this.analyzer = new SmartChineseAnalyzer();
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            this.indexWriter = new IndexWriter(directory, config);
        } catch (IOException e) {
            log.error("初始化 Lucene IndexWriter 失败", e);
        }
    }

    /**
     * 将文档加入 BM25 索引
     *
     * @param documents Spring AI 的文档列表
     */
    public void addDocuments(List<org.springframework.ai.document.Document> documents) {
        try {
            for (org.springframework.ai.document.Document doc : documents) {
                Document luceneDoc = new Document();
                // 索引内容
                luceneDoc.add(new TextField("content", doc.getText(), Field.Store.YES));
                // 索引 id 并存储
                luceneDoc.add(new StringField("id", doc.getId(), Field.Store.YES));

                // 存储其他有用的元数据，方便检索后组装回 Spring AI Document
                String source = (String) doc.getMetadata().getOrDefault("source", "");
                luceneDoc.add(new StringField("source", source, Field.Store.YES));

                // 如果有 chunk_index
                Object chunkIndexObj = doc.getMetadata().get("chunk_index");
                if (chunkIndexObj != null) {
                    luceneDoc.add(new StringField("chunk_index", chunkIndexObj.toString(), Field.Store.YES));
                }

                indexWriter.addDocument(luceneDoc);
            }
            indexWriter.commit();
            refreshSearcher();
            log.info("BM25 索引已添加 {} 个文档", documents.size());
        } catch (IOException e) {
            log.error("构建 BM25 索引失败", e);
        }
    }

    /**
     * 执行 BM25 检索
     *
     * @param queryString 查询字符串
     * @param topK        返回前 K 个
     * @return 匹配的 Spring AI Document 列表（包含相似度分数在 metadata 中）
     */
    public List<org.springframework.ai.document.Document> search(String queryString, int topK) {
        List<org.springframework.ai.document.Document> results = new ArrayList<>();
        if (indexSearcher == null) {
            log.warn("BM25 IndexSearcher 未初始化或索引为空");
            return results;
        }

        try {
            // 转义 Lucene 关键字，或者对于简单场景处理异常即可
            // 这里为了安全，可以自己做简单的转义或者使用默认 Parser
            QueryParser parser = new QueryParser("content", analyzer);
            // 简单处理无法解析的问题，比如空查询或者特殊符号
            String escapedQuery = QueryParser.escape(queryString);
            if (escapedQuery.isBlank()) {
                return results;
            }

            Query query = parser.parse(escapedQuery);
            TopDocs topDocs = indexSearcher.search(query, topK);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document luceneDoc = indexSearcher.doc(scoreDoc.doc);
                String id = luceneDoc.get("id");
                String content = luceneDoc.get("content");
                String source = luceneDoc.get("source");
                String chunkIndex = luceneDoc.get("chunk_index");

                org.springframework.ai.document.Document aiDoc = new org.springframework.ai.document.Document(
                        id,
                        content,
                        Map.of(
                                "source", source != null ? source : "",
                                "chunk_index", chunkIndex != null ? chunkIndex : "",
                                "bm25_score", scoreDoc.score,
                                "search_type", "bm25"
                        )
                );
                results.add(aiDoc);
            }

            log.info("BM25 查询 '{}' 返回 {} 条结果", queryString, results.size());

        } catch (ParseException | IOException e) {
            log.error("BM25 查询失败: {}", queryString, e);
        }

        return results;
    }

    /**
     * 刷新搜索器以包含最新提交的文档
     */
    private synchronized void refreshSearcher() {
        try {
            if (indexReader == null) {
                indexReader = DirectoryReader.open(directory);
            } else {
                IndexReader newReader = DirectoryReader.openIfChanged((DirectoryReader) indexReader);
                if (newReader != null) {
                    indexReader.close();
                    indexReader = newReader;
                }
            }
            indexSearcher = new IndexSearcher(indexReader);
        } catch (IOException e) {
            log.error("刷新 BM25 IndexSearcher 失败", e);
        }
    }
}
