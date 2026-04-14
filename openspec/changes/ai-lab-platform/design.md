## Context

ChronoTech 项目当前状态：
- **前端**：4 个页面已完成（首页/产品/关于/联系），全站响应式适配，AI 客服悬浮窗
- **后端**：Spring Boot 3.4 + Spring AI 1.0 + DashScope (qwen-plus)，已有 ChatService (SSE流式)、DocumentService (Tika+分块)、CustomerServiceTools (基础工具)
- **缺口**：前后端断裂（无产品API、无联系表单API）、RAG 未激活、工具集不匹配手表场景、system-prompt 未适配

**模型供应商**：全部走阿里云通义千问 (DashScope)，不同场景使用不同模型规格。

**目标受众**：面试官/甲方，需要"技术深度可见 + 视觉冲击力"。

## Goals / Non-Goals

**Goals:**
- 构建 6 个可交互的 AI Demo 模块，每个独立可演示
- 所有 AI 过程"可视化"（思考链、工具调用、检索来源、耗时指标）
- 保持纯前端+Spring Boot 架构，无额外基础设施依赖（开箱即用）
- 预置足够的产品数据让 Demo 开箱有内容

**Non-Goals:**
- 不做用户认证/登录系统
- 不做生产级部署（K8s/微服务拆分）
- 不做模型微调/训练
- 不做语音交互（仅文本+图片）
- 不做实时监控和告警系统

## Decisions

### D1: 多模型路由策略

| 场景 | 模型 | 理由 |
|------|------|------|
| 普通对话/NLP 分析 | qwen-plus | 性价比最优 |
| 复杂 Agent 推理 | qwen-max | 推理能力更强 |
| 图片理解 | qwen-vl-max | 唯一支持多模态 |
| 文本 Embedding | text-embedding-v3 | DashScope 默认向量模型 |

**替代方案**：全部用 qwen-max（更简单但成本高）。选择多模型路由是因为 Demo 需要展示"模型路由"本身也是一种 AI 工程能力。

**实现**：配置多个 `ChatClient` Bean（`chatClient`, `visionChatClient`, `agentChatClient`），通过 `@Qualifier` 注入不同 Service。

### D2: RAG 存储方案 — SimpleVectorStore + JSON 持久化

**选择**：继续用 `SimpleVectorStore`（内存向量库），但启动时从 JSON 文件加载预置向量数据。

**替代方案**：
- PGVector（需要 PostgreSQL，增加部署复杂度）
- ChromaDB（额外进程，不利于开箱即用）

**理由**：Demo 场景数据量小（<100 个文档块），内存版足够。启动时自动加载预置文档，JSON 持久化可避免每次重启重新 Embedding。

### D3: Agent 思考链暴露机制

**选择**：通过 SSE 流向前端推送结构化事件，区分 `text`、`tool_call`、`tool_result`、`thinking` 四种事件类型。

```
SSE 事件流示例：
event: thinking
data: {"step": 1, "content": "用户想对比两款手表，需要查询产品规格"}

event: tool_call
data: {"tool": "queryProduct", "args": {"id": "sport-x2-pro"}, "step": 2}

event: tool_result
data: {"tool": "queryProduct", "result": "...", "duration_ms": 120, "step": 2}

event: text
data: {"content": "根据对比分析..."}
```

**替代方案**：只返回最终文本（简单但失去可视化价值）。选择暴露全过程是因为"透明度"本身就是 Demo 的卖点。

### D4: 前端页面组织 — 独立页面 + 共享组件

**选择**：每个 Demo 是独立 HTML 页面（`ai-lab.html`, `demo-chat.html`, `demo-vision.html` 等），共享 CSS 变量和 JS 工具函数。

**理由**：纯前端架构，不引入打包工具；每个 Demo 独立加载、可单独演示；降低复杂度。

### D5: 产品数据 — 后端硬编码 JSON

**选择**：在后端用 `static final` 硬编码 6 款产品数据（JSON 对象），提供 RESTful API 返回。

**替代方案**：数据库存储（过重）、前端硬编码（无法被 Agent 工具调用）。

**理由**：6 款产品数据量极小；硬编码让后端工具可以查询；避免引入数据库依赖。

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|------|------|------|
| DashScope API Key 额度用完 | 所有 AI Demo 不可用 | 前端做降级提示；Agent Demo 可展示录屏/截图 |
| qwen-vl-max 调用延迟高 (2-5s) | 多模态 Demo 体验差 | 前端加 loading 动画+进度提示 |
| SimpleVectorStore 重启丢数据 | RAG 每次冷启动需重新 Embedding | 实现启动时自动加载预置文档 |
| 多模型配置冲突 | Spring AI 自动配置可能冲突 | 手动创建 Bean，禁用 auto-config |
| Demo 数据不够真实 | 面试官质疑说服力 | 预置文档写得专业详细，模拟真实产品手册 |

## Open Questions

- qwen-vl-max 在 Spring AI Alibaba 中的集成方式是否成熟？需要做一个 spike 验证
- Agent 多步推理在 Spring AI 中是否有原生支持（ReAct 模式），还是需要自己实现循环？
