## Context

ChronoTech AI Lab 平台已完成 V1（49 tasks 全部完成），当前技术栈：
- **后端**：Spring Boot 3.4 + Spring AI 1.0 + DashScope (qwen-plus/max/vl-max)
- **RAG**：SimpleVectorStore（内存）+ TokenTextSplitter（800/200）+ TikaDocumentReader，但 `QuestionAnswerAdvisor` 被注释，RAG 管道未激活
- **Agent**：ChatClient 内置 tool calling（ProductTools + CustomerServiceTools），单轮调用无显式推理循环
- **记忆**：MessageWindowChatMemory（固定窗口 20 轮），无摘要/压缩/持久化
- **前端**：纯 HTML/CSS/JS，6 个 Demo 页面已完成

**约束**：
- 无本地 GPU，LoRA 微调必须走云端（阿里云 PAI / ModelScope）
- 保持纯前端 + Spring Boot 单体架构（不引入微服务）
- 模型供应商锁定 DashScope

## Goals / Non-Goals

**Goals:**
- RAG 管道做到 Hybrid Search + Rerank + 评估 的生产级深度
- Agent 做到 ReAct 多步推理 + 对话摘要记忆 + 多 Agent 协作
- 完成一次完整的 LoRA 微调流程（数据→训练→评测→对比）
- 所有 AI 过程保持"可视化"（前端能看到完整管道）
- 每个模块都能"讲清原理 + show the code"

**Non-Goals:**
- 不做自建向量数据库（PGVector/Milvus 等，用 SimpleVectorStore 足够学习）
- 不做模型预训练或全量微调
- 不做 Kubernetes/微服务部署
- 不做用户认证系统
- 不做语音/视频模态

## Decisions

### D1: RAG 检索管道架构 — 四阶管道

```
用户问题
   │
   ▼
┌──────────────┐
│ Query 改写   │  用 LLM 对原始问题做扩展/纠正
└──────┬───────┘
       │
   ┌───┴────┐
   ▼        ▼
┌──────┐ ┌──────┐
│BM25  │ │向量  │  双路召回
│关键词│ │语义  │
└──┬───┘ └──┬───┘
   │        │
   ▼        ▼
┌──────────────┐
│ RRF 融合排序 │  Reciprocal Rank Fusion
└──────┬───────┘
       ▼
┌──────────────┐
│ LLM Rerank   │  用 LLM 对 top-N 做二级排序
└──────┬───────┘
       ▼
┌──────────────┐
│ 生成 + 溯源  │  带 source 引用的回答
└──────────────┘
```

**实现方式**：自定义 Spring AI `Advisor` 链，每个阶段封装为独立 Advisor，可配置开关。

**替代方案**：用 LangChain4j（功能完整但引入新框架太重）。选择自建管道是因为学习价值更高，且与现有 Spring AI 架构一致。

### D2: BM25 实现 — 内存版 Lucene

**选择**：用 Apache Lucene 在内存中建 BM25 索引，与 SimpleVectorStore 并行。

**替代方案**：
- Elasticsearch（太重）
- 手写 BM25（实现成本高、精度低）

**理由**：Lucene 是 BM25 的工业实现，嵌入式使用无需额外服务，`lucene-core` + `lucene-analysis-smartcn`（中文分词）即可。

### D3: Agent ReAct 引擎 — 自建循环

**选择**：在 `ChatService` 之上构建 `ReActEngine`，手写 while 循环实现 思考→行动→观察：

```java
while (!finished && step < maxSteps) {
    // 1. 思考：LLM 决定下一步
    ThinkResult think = llm.think(context);
    emit(SSE_THINKING, think);

    if (think.isFinished()) break;

    // 2. 行动：执行工具
    ToolResult result = toolExecutor.execute(think.getToolCall());
    emit(SSE_TOOL_RESULT, result);

    // 3. 观察：追加到上下文
    context.addObservation(result);
    step++;
}
```

**替代方案**：Spring AI 的 ChatClient 内置 tool calling 循环（现有方案）。选择自建是因为需要暴露每一步给前端可视化，且需要自定义终止条件和重试逻辑。

### D4: 对话记忆 — 三级记忆架构

```
┌─────────────────────────────────────────────────┐
│               三级记忆架构                       │
├─────────────────────────────────────────────────┤
│                                                 │
│  L1: 工作记忆（短期）                           │
│      MessageWindow — 最近 10 轮原始对话         │
│      存储：内存                                  │
│                                                 │
│  L2: 摘要记忆（中期）                           │
│      超出窗口的对话自动压缩为摘要               │
│      存储：Redis（key=conversationId）          │
│                                                 │
│  L3: 用户画像（长期）                           │
│      从多次对话中提取用户偏好/需求模式          │
│      存储：Redis Hash                           │
│                                                 │
└─────────────────────────────────────────────────┘
```

**实现**：自定义 `SummarizingChatMemory` 实现 `ChatMemory` 接口，内部协调三级存储。

### D5: 多 Agent 协作 — Supervisor 模式

```
                    ┌──────────────┐
                    │  Supervisor  │  路由决策（用 LLM 判断）
                    │    Agent     │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  客服     │ │  分析    │ │  检索    │
        │  Agent   │ │  Agent   │ │  Agent   │
        └──────────┘ └──────────┘ └──────────┘
        产品咨询      数据分析      知识库检索
        工单创建      竞品对比      文档查找
```

**实现**：每个子 Agent 是一个独立的 `ChatClient` Bean，Supervisor 通过 LLM 分类用户意图后路由。

### D6: LoRA 微调 — 阿里云 ModelScope 路线

**选择**：使用 ModelScope 平台的免费训练资源微调 qwen-7b-chat

**流程**：
1. 构造 500 条 ChronoTech 客服场景的指令对（JSON Lines 格式）
2. 上传到 ModelScope → 选择 LoRA 微调 → 训练
3. 部署微调模型为 API 端点
4. 前端对比面板：base model vs 微调模型 并排回答

**替代方案**：阿里云 PAI（更专业但需付费）。选择 ModelScope 是因为有免费额度且流程更简单。

### D7: RAG 评估框架

**实现**：
- 手工构造 30-50 个 QA 对（问题 + 标准答案 + 相关文档标注）
- 自动化评估脚本计算：Recall@K、Answer Relevancy、Faithfulness
- 对比不同配置的效果：不同分块大小、有无 Rerank、有无 Query 改写

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|------|------|------|
| Lucene 中文分词质量 | BM25 检索效果差 | 用 SmartCN 分析器 + 自定义停用词 |
| ReAct 循环死循环 | Agent 无限调用工具 | 设最大步数（10）+ 超时终止 |
| Redis 增加部署复杂度 | 新手部署困难 | docker-compose 一键启动 + 无 Redis 降级为内存模式 |
| ModelScope 免费额度不足 | 微调不完整 | 缩小数据集 + 用 1.8b 模型替代 |
| 多 Agent 路由判断不准 | 用户体验差 | 设置 fallback 默认路由到客服 Agent |
| Spring AI Advisor 链的性能 | 多级 Advisor 串联延迟 | 每级 Advisor 做耗时追踪，异步化可选环节 |

## Open Questions

- Spring AI 1.0 中自定义 Advisor 的最佳实践是什么？需要 spike 验证 Advisor 链的执行顺序和上下文传递
- ModelScope 的 LoRA 微调流程是否稳定？需要实际跑一遍确认
- Lucene SmartCN 对手表领域专业术语的分词效果如何？可能需要自定义词典
