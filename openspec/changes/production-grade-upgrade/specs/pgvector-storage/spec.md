## ADDED Requirements

### Requirement: pgvector 向量存储
系统 SHALL 使用 PostgreSQL pgvector 扩展替代 SimpleVectorStore 作为向量存储后端。

#### Scenario: 知识库文档持久化
- **WHEN** 上传知识库文档并重启应用
- **THEN** 向量数据仍然存在于 pgvector 表中，无需重新 embedding

#### Scenario: 相似度检索
- **WHEN** 用户发起 RAG 查询
- **THEN** 系统通过 pgvector 进行向量相似度检索，返回 top-K 相关文档

### Requirement: 产品视觉描述持久化
系统 SHALL 将以图搜图的产品视觉描述数据持久化至 pgvector，取代启动时内存初始化。

#### Scenario: 重启后搜图仍可用
- **WHEN** 应用重启后调用以图搜图接口
- **THEN** 无需重新调用 Embedding API，直接从 pgvector 检索匹配产品

### Requirement: Spring AI PgVectorStore 集成
系统 SHALL 通过 Spring AI 官方 `spring-ai-pgvector-store` 依赖集成 pgvector。

#### Scenario: VectorStore Bean 切换
- **WHEN** 应用启动
- **THEN** VectorStore Bean 类型为 PgVectorStore，而非 SimpleVectorStore
