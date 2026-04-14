package com.agent.service.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 语义分块器
 * 使用 embedding 相似度判断分块边界：相邻句子语义差异大于阈值时切分
 * 原理：将文本按句子切分 → 计算相邻句子的 embedding 余弦相似度 → 低于阈值处切断
 */
public class SemanticSplitter {

    private final EmbeddingModel embeddingModel;
    /** 语义差异阈值：相邻句子相似度低于此值则切分 */
    private final double similarityThreshold;
    /** 单个 chunk 最大字符数（防止合并后超大） */
    private final int maxChunkSize;

    public SemanticSplitter(EmbeddingModel embeddingModel, double similarityThreshold, int maxChunkSize) {
        this.embeddingModel = embeddingModel;
        this.similarityThreshold = similarityThreshold;
        this.maxChunkSize = maxChunkSize;
    }

    /**
     * 对文档列表执行语义分块
     */
    public List<Document> apply(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            List<String> chunks = splitBySemantic(doc.getText());
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new java.util.HashMap<>(doc.getMetadata());
                metadata.put("chunk_index", i);
                metadata.put("chunk_strategy", "semantic");
                result.add(new Document(chunks.get(i), metadata));
            }
        }
        return result;
    }

    /**
     * 基于语义相似度的分块逻辑
     */
    private List<String> splitBySemantic(String text) {
        // 1. 按句子粒度切分
        List<String> sentences = splitToSentences(text);
        if (sentences.size() <= 1) {
            return List.of(text);
        }

        // 2. 计算所有句子的 embedding
        List<float[]> embeddings = new ArrayList<>();
        for (String sentence : sentences) {
            float[] embedding = embeddingModel.embed(sentence);
            embeddings.add(embedding);
        }

        // 3. 计算相邻句子的余弦相似度，找到切分点
        List<Integer> splitPoints = new ArrayList<>();
        for (int i = 0; i < embeddings.size() - 1; i++) {
            double similarity = cosineSimilarity(embeddings.get(i), embeddings.get(i + 1));
            if (similarity < similarityThreshold) {
                splitPoints.add(i + 1); // 在第 i+1 个句子前切分
            }
        }

        // 4. 按切分点合并句子为 chunk
        List<String> chunks = new ArrayList<>();
        int start = 0;
        for (int splitPoint : splitPoints) {
            String chunk = joinSentences(sentences, start, splitPoint);
            // 如果合并后的 chunk 太长，强制按最大长度再切
            if (chunk.length() > maxChunkSize) {
                chunks.addAll(forceSplitChunk(chunk));
            } else if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            start = splitPoint;
        }

        // 最后一段
        if (start < sentences.size()) {
            String lastChunk = joinSentences(sentences, start, sentences.size());
            if (!lastChunk.isBlank()) {
                if (lastChunk.length() > maxChunkSize) {
                    chunks.addAll(forceSplitChunk(lastChunk));
                } else {
                    chunks.add(lastChunk);
                }
            }
        }

        return chunks;
    }

    /**
     * 按中文/英文句号切分为句子列表
     */
    private List<String> splitToSentences(String text) {
        // 按中英文句号、问号、感叹号切分
        String[] parts = text.split("(?<=[。！？.!?\\n])");
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    /**
     * 将句子列表 [start, end) 合并为一个字符串
     */
    private String joinSentences(List<String> sentences, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(sentences.get(i));
            if (i < end - 1) sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * 强制按最大长度切分超长 chunk
     */
    private List<String> forceSplitChunk(String chunk) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < chunk.length()) {
            int end = Math.min(start + maxChunkSize, chunk.length());
            result.add(chunk.substring(start, end).strip());
            start = end;
        }
        return result;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
