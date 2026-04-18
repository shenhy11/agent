-- ============================================================
-- V2: 启用 pgvector 扩展，用于向量存储与检索
-- Spring AI PgVectorStore 依赖此扩展
-- ============================================================

-- 启用 pgvector 扩展（需要 PostgreSQL superuser 权限，或提前由 DBA 执行）
CREATE EXTENSION IF NOT EXISTS vector;

-- Spring AI PgVectorStore 会自动建 vector_store 表，此处无需手动建
-- 若需手动确认，表结构如下（注释供参考）：
-- CREATE TABLE IF NOT EXISTS vector_store (
--     id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
--     content     TEXT,
--     metadata    JSON,
--     embedding   VECTOR(1536)
-- );
-- CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
--     ON vector_store USING hnsw (embedding vector_cosine_ops);
