-- ============================================================
-- V3: 创建 Spring AI PgVectorStore 所需的 vector_store 表
-- 使用 1536 维向量（通义千问 text-embedding-v1/v2 默认维度）
-- ============================================================

-- 确保 pgvector 扩展启用（与 V2 幂等）
CREATE EXTENSION IF NOT EXISTS vector;

-- 创建向量存储表
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(1536)
);

-- 创建 HNSW 索引，加速余弦相似度向量检索
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
    ON vector_store
    USING hnsw (embedding vector_cosine_ops);
