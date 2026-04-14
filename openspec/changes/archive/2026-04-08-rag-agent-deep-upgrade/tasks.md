## 1. 基础设施 — Redis + 依赖 + 配置

- [x] 1.1 `docker-compose.yml` 新增 Redis 容器（redis:7-alpine，端口 6379）——已有
- [x] 1.2 `agent-server/pom.xml` 新增依赖：`spring-boot-starter-data-redis`、`lucene-core`（9.x）、`lucene-analysis-smartcn`
- [x] 1.3 `application.yml` 新增配置块：Redis 连接、RAG 高级参数（query-rewrite-enabled、rerank-enabled、chunking-strategy）、Agent 策略参数
- [x] 1.4 创建 `RedisConfig.java`，配置 `RedisTemplate<String, Object>` 和 JSON 序列化

## 2. RAG 深化 — 多分块策略

- [x] 2.1 创建 `ChunkingStrategyFactory.java`，支持 3 种策略（token/recursive/semantic）的创建
- [x] 2.2 实现 `RecursiveCharacterSplitter.java`，按段落→句子→字符逐级切分
- [x] 2.3 实现 `SemanticSplitter.java`，使用 embedding 相似度判断分块边界
- [x] 2.4 改造 `KnowledgeInitializer` 和 `DocumentService`，通过配置切换分块策略
- [x] 2.5 验证：同一文档使用 3 种策略分块，对比块数和内容质量

## 3. RAG 深化 — BM25 关键词检索

- [x] 3.1 创建 `BM25SearchService.java`，封装 Lucene 内存索引的创建和查询
- [x] 3.2 集成 SmartCN 中文分析器，实现中文分词检索
- [x] 3.3 在 `KnowledgeInitializer` 中同步构建 BM25 索引（文档加载时同时写入 VectorStore 和 Lucene）
- [x] 3.4 实现 `BM25SearchService.search(query, topK)` 返回关键词匹配文档列表
- [x] 3.5 验证：对中文查询返回正确的 BM25 检索结果

## 4. RAG 深化 — Hybrid Search + RRF 融合

- [x] 4.1 创建 `HybridSearchService.java`，组合 VectorStore 和 BM25 双路检索
- [x] 4.2 实现 RRF（Reciprocal Rank Fusion）融合算法，K=60
- [x] 4.3 创建 `HybridSearchAdvisor.java`（实现 Spring AI Advisor 接口），替代默认向量检索
- [x] 4.4 验证：对同一问题分别展示向量结果、BM25 结果和 RRF 融合结果

## 5. RAG 深化 — Query 改写 + LLM Reranking

- [x] 5.1 创建 `QueryRewriteService.java`，用 LLM 对原始问题做扩展/改写
- [x] 5.2 创建 `LlmRerankService.java`，对候选文档使用 LLM 打分排序（1-10），返回 top-N
- [x] 5.3 创建 `RagPipelineService.java`，编排完整管道：Query改写→Hybrid Search→RRF→Rerank→生成
- [x] 5.4 每个阶段添加开关配置和耗时记录
- [x] 5.5 验证管道端到端：问题 → 改写 → 双路召回 → 融合 → Rerank → 带溯源回答

## 6. RAG 深化 — 评估框架

- [x] 6.1 在 `docs/eval/` 下编写 QA 测试集（JSON 格式，30-50 条 question/expected_answer/relevant_doc_ids）
- [x] 6.2 创建 `RagEvaluationService.java`，实现 Recall@K 计算和答案质量 LLM 打分
- [x] 6.3 创建 `EvaluationController.java`，提供 `POST /api/eval/run` 端点，返回评估报告
- [x] 6.4 实现配置对比功能：不同 RAG 配置的 A/B 评估结果对比
- [x] 6.5 验证：运行评估获取 Recall@1/3/5 和答案质量分数

## 7. RAG 深化 — 修复 RAG Advisor + 来源溯源

- [x] 7.1 取消 `AiConfig` 中 `QuestionAnswerAdvisor` 的注释，接入 `RagPipelineService` 作为数据源
- [x] 7.2 修改 SSE 响应格式，新增 `event: retrieval` 事件推送检索来源（文件名、chunk 序号、相似度分数、内容摘要）
- [x] 7.3 验证：对话时前端能展示引用来源列表

## 8. Agent 深化 — ReAct 推理引擎

- [x] 8.1 创建 `ReActEngine.java`，实现 while 循环：思考→行动→观察，最大 10 步
- [x] 8.2 定义 `ThinkResult` / `ToolCallEvent` / `ObservationEvent` 数据模型
- [x] 8.3 在 ReAct 引擎中集成 SSE 事件发送（thinking/tool_call/tool_result 事件）
- [x] 8.4 实现工具调用错误恢复：超时重试、异常捕获后让 LLM 自主选择替代方案
- [x] 8.5 创建 `ReActController.java`，提供 `GET /api/agent/react?message=xxx` 端点
- [x] 8.6 验证：提问复杂问题 → 观察多步推理过程 → 最终综合回答

## 9. Agent 深化 — 三级记忆系统

- [x] 9.1 创建 `SummarizingChatMemory.java` 实现 `ChatMemory` 接口，内部管理工作记忆 + 摘要记忆
- [x] 9.2 实现对话摘要压缩逻辑：超过窗口时用 LLM 压缩最早 N 轮为摘要文本
- [x] 9.3 实现 Redis 持久化：每轮对话后保存到 Redis（key=conversationId，TTL=24h）
- [x] 9.4 实现无 Redis 降级：连接不可用时自动切换为纯内存模式
- [x] 9.5 创建 `UserProfileService.java`，从对话中提取偏好标签写入 Redis Hash
- [x] 9.6 在 `AiConfig` 中替换 `MessageWindowChatMemory` 为 `SummarizingChatMemory`
- [x] 9.7 验证：长对话场景下摘要压缩 + 记忆恢复 + 用户画像注入

## 10. Agent 深化 — 多 Agent 协作

- [x] 10.1 修改 `AiConfig`，并在其中配置出独立的 `customerServiceChatClient` 和 `afterSalesChatClient`
- [x] 10.2 创建 `RouterAgent.java`（负责意图识别与请求路由）
- [x] 10.3 实现路由逻辑：基于 LLM 判断意图类别（客服 vs 售后）并分发
- [x] 10.4 改造 `ChatController` / `ChatService`，接入 `RouterAgent` 作为入口
- [x] 10.5 创建 `MultiAgentController.java`，提供 `GET /api/agent/multi?message=xxx` 端点
- [x] 10.6 验证：不同领域问题能被正确路由到对应的 Agent 处理

## 11. Agent 深化 — 可观测性

- [x] 11.1 创建 `AiMetricsService.java`，追踪 token 消耗、调用延迟、工具调用成功/失败
- [x] 11.2 为 RagPipelineService 和 ReActEngine 的每个阶段添加耗时埋点
- [x] 11.3 创建 `MetricsController.java`，提供 `GET /api/metrics/summary` 端点
- [x] 11.4 验证：前端能获取并展示 token 消耗和管道耗时数据

## 12. LoRA 微调 — 数据集 + 训练 + 对比

- [x] 12.1 在 `docs/finetune/` 下编写 SFT 数据集生成脚本（Python），生成 ~500 条 ChronoTech 客服指令对
- [x] 12.2 编写微调配置文档 `docs/finetune/README.md`：ModelScope 操作步骤、LoRA 参数（rank=8, alpha=32, lr=2e-4, epochs=3）
- [x] 12.3 在 ModelScope 上完成 qwen-7b-chat 的 LoRA 微调并部署为 API（仅配置和接口层接入模拟）
- [x] 12.4 在 `AiConfig` 中新增 `finetunedChatClient` Bean，配置微调模型 API 端点
- [x] 12.5 创建 `FinetuneCompareController.java`，实现 `/api/finetune/xxxx`，分别调用 base+微调模型
- [x] 12.6 验证：对比页面能并排展示两个模型的回答

## 13. 前端升级 — RAG Demo 页

- [x] 13.1 升级 `demo-rag.html`：新增管道可视化区域（Query改写→双路召回→RRF→Rerank→生成的流程图）
- [x] 13.2 升级 `js/demo-rag.js`：解析 SSE retrieval 事件，渲染检索来源卡片（文件名+相似度+摘要）
- [x] 13.3 新增分块策略切换下拉框（token/recursive/semantic），切换后重建索引
- [x] 13.4 新增管道阶段耗时瀑布图

## 14. 前端升级 — Agent Demo 页

- [x] 14.1 升级 `demo-agent.html`：新增 ReAct 循环步骤可视化（思考→行动→观察的流程节点）
- [x] 14.2 升级 `demo-chat.html`：接入 ReAct 引擎和多 Agent 协作，支持路由指示器（显示当前由哪个 Agent 处理）
- [x] 14.3 新增 Agent 统计面板：token 消耗、工具调用次数、管道耗时

## 15. 前端升级 — 微调对比 Demo 页

- [x] 15.1 创建 `demo-finetune.html`：双栏对话面板（左 base model / 右微调模型），同一问题并排展示
- [x] 15.2 创建 `js/demo-finetune.js`：同时发送请求到两个模型端点，流式渲染回答
- [x] 15.3 AI Lab 入口页新增微调对比卡片

## 16. 整体联调与验证

- [x] 16.1 端到端联调：RAG 管道→ReAct Agent→多 Agent→前端全链路验证
- [x] 16.2 所有新 Demo 页响应式适配
- [x] 16.3 错误降级验证：无 Redis、Rerank 超时、工具调用失败等场景
- [x] 16.4 更新 `application.yml` 整理所有新增配置
- [x] 16.5 更新 `PROJECT_STATUS.md` 和 `README.md` 反映新架构
