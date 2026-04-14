# 📝 关键决策记录 (ADR)

> 本文档记录项目中所有重要的技术选型和架构决策，避免遗忘"当初为什么这样做"。
> 格式遵循轻量级 ADR (Architecture Decision Record)。

---

## ADR-001：技术栈选择 Spring AI + Spring AI Alibaba

- **日期**: 2026-03-25
- **状态**: ✅ 已执行
- **背景**: 需要选择 AI Agent 框架来对接大模型，候选方案包括 LangChain4j、Spring AI、直接调用 DashScope HTTP API。
- **决策**: 选择 **Spring AI 1.0 GA + Spring AI Alibaba 1.0**
- **理由**:
  1. 与现有 Java / Spring Boot 生态无缝集成，不需引入 Python 技术栈
  2. 内置 RAG、ChatMemory、Tool Calling、Advisor 链等 Agent 核心能力
  3. Spring AI Alibaba 原生支持通义千问 (DashScope)，无需自研适配器
  4. 2025 年底 Spring AI 正式 GA，API 稳定性有保障
- **代价**: Spring AI 生态相比 LangChain 社区资源更少，遇到问题排查成本高
- **参考**: `docs/Overall-Solution.md`

---

## ADR-002：使用 SimpleVectorStore 作为临时向量库

- **日期**: 2026-03-25
- **状态**: 🟡 临时方案，后续需迁移
- **背景**: 生产架构设计使用 PostgreSQL + pgvector，但本地开发环境搭建 pgvector 有额外成本。
- **决策**: Phase 1 阶段使用 `SimpleVectorStore`（基于内存的向量库）
- **理由**:
  1. 零外部依赖，可快速验证 RAG 链路
  2. Spring AI 提供了统一的 `VectorStore` 接口，切换到 PgVectorStore 只需改配置
- **代价**: 数据不持久化，每次重启丢失；不适合大数据量
- **后续**: Phase 2 迁移到 PgVectorStore

---

## ADR-003：对话记忆使用内存存储（MessageWindowChatMemory）

- **日期**: 2026-03-25
- **状态**: 🟡 临时方案，后续需迁移
- **背景**: 生产方案需要 Redis 存储会话历史，但 Phase 1 优先级是跑通链路。
- **决策**: 使用 `MessageWindowChatMemory`（默认内存存储），窗口大小 20 条消息
- **理由**:
  1. 无需 Redis 依赖即可运行
  2. API 与 Redis 版本完全兼容，后续切换无代码改动
- **代价**: 单节点限制，重启丢失
- **后续**: Phase 2 切换为 Redis 持久化实现

---

## ADR-004：Tool Calling 使用 Mock 实现

- **日期**: 2026-03-25
- **状态**: 🟡 临时方案
- **背景**: 订单查询、工单创建、余额查询等工具需要对接内部业务系统 API，但 Phase 1 阶段这些系统不可用。
- **决策**: `CustomerServiceTools` 中所有方法返回 Mock 数据
- **理由**:
  1. 可验证 LLM → Tool Calling → 结果回传的完整链路
  2. Mock 数据格式与生产接口返回一致，后续替换无需改上层代码
- **代价**: 无法验证真实业务逻辑的正确性
- **后续**: Phase 3 逐一对接真实 API

---

## ADR-005：采用四阶段递进式开发策略

- **日期**: 2026-03-26
- **状态**: ✅ 已执行
- **背景**: 单人/小团队开发企业级 AI Agent 系统，需要控制复杂度和风险。
- **决策**: 分四阶段推进：底座 → 权限后台 → 业务核心 → 工程化稳定性
- **理由**:
  1. 每阶段有明确可验证的交付物，避免"做了很多但都没做完"
  2. 先保证能跑起来，再加安全、再加业务、最后做质量
  3. 前期快速试错，后期逐步加固
- **代价**: 前期可能遗留一些技术债务（如临时 Mock、内存存储）
- **参考**: `NEXT_ACTIONS.md`

---

## ADR-006：主力模型选择通义千问，OpenAI 作为兜底

- **日期**: 2026-03-25
- **状态**: ✅ 已执行
- **背景**: 需要选择核心推理模型，同时需要考虑国内合规和成本因素。
- **决策**: 主力用通义千问 `qwen-plus`，预留 OpenAI 配置作为降级方案
- **理由**:
  1. 国产模型，数据合规无忧
  2. 通义千问性价比高，适合大规模客服场景
  3. 通过 Spring AI 的抽象层，切换模型只需改配置
- **代价**: 通义系列模型在部分英文场景表现弱于 GPT-4o
- **配置位置**: `application.yml` → `spring.ai.dashscope` / `spring.ai.openai`

---

*新决策请追加在文档末尾，保持编号连续。*
