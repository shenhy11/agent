## ADDED Requirements

### Requirement: RAG 知识库预置加载
系统 SHALL 在启动时自动从 `docs/knowledge/` 目录加载预置的产品文档（Markdown/PDF），执行分块和向量化，存入 VectorStore。

#### Scenario: 启动自动加载
- **WHEN** Spring Boot 应用启动完成
- **THEN** VectorStore 中已包含预置产品文档的向量数据，可供检索

### Requirement: ChatClient 挂载 RAG Advisor
系统 SHALL 在 ChatClient 配置中挂载 `QuestionAnswerAdvisor`，使 AI 对话自动检索知识库内容作为上下文。

#### Scenario: 用户提问时触发检索增强
- **WHEN** 用户通过 `/api/chat/stream` 问 "Sport X2 Pro 的续航时间是多少天？"
- **THEN** 系统先从 VectorStore 检索相关文档块，拼入 prompt context，再由 LLM 生成回答

### Requirement: 检索结果元数据返回
系统 SHALL 在 RAG 对话响应中返回检索到的文档来源信息（文件名、相似度分数、文档块内容摘要）。

#### Scenario: 前端展示来源信息
- **WHEN** 用户进行 RAG 增强的对话
- **THEN** SSE 流中包含 `retrieval` 类型事件，携带 top-K 检索结果的来源文件名和相似度分数

### Requirement: 知识库文档管理 API
系统 SHALL 保留现有的 `POST /api/knowledge/{knowledgeId}/upload` 接口，支持用户在 Demo 中动态上传文档。

#### Scenario: 用户上传 PDF 文档
- **WHEN** 用户通过 RAG Demo 页面上传一个 PDF 文件
- **THEN** 系统解析、分块、向量化后存入 VectorStore，后续对话可检索到该文档内容

### Requirement: 预置产品文档内容
系统 SHALL 提供至少 3 份预置文档：产品规格手册、常见问题 FAQ、售后服务政策。

#### Scenario: 基于 FAQ 回答售后问题
- **WHEN** 用户问 "手表保修期是多久？"
- **THEN** AI 基于 FAQ 文档回答保修政策，并标注来源
