## Why

ChronoTech AI Lab 平台已完成横向 6 大 Demo 搭建（49/49 tasks），但每个方向深度不足：RAG 只有基础 top-k 向量检索（且 Advisor 被注释）、Agent 仅单轮 tool calling 无显式推理循环、缺乏模型微调经验。需要在 **RAG** 和 **Agent** 两个最具市场价值的方向做到生产级深度，同时补充轻量级 **LoRA 微调**经验，实现从"展示型 Demo"到"可讲清原理+show the code"的学习目标。

## What Changes

### 后端 — RAG 深化（L2→L4）
- **修复 RAG Advisor**：取消 `AiConfig` 中 `QuestionAnswerAdvisor` 的注释，激活 RAG 检索增强管道
- **新增多分块策略**：在 `DocumentService` 中支持 3 种分块方式（Token / 递归字符 / 语义），可对比切换
- **新增 Hybrid Search**：关键词 BM25 + 向量语义双路召回，RRF 融合排序
- **新增 LLM Reranking**：对召回结果进行 LLM 二级排序，去噪筛选
- **新增 RAG 评估模块**：构造 QA 测试集，自动计算 Recall@K、命中率等指标
- **新增 Query 改写**：对用户问题进行扩展/改写，提升检索质量
- **RAG 结果溯源**：返回每条引用来源的文件名、chunk 位置、相似度分数

### 后端 — Agent 深化（L2→L4）
- **新增 ReAct 循环引擎**：显式的 思考→行动→观察 循环，Agent 自主决定是否继续
- **新增对话摘要记忆**：超出窗口时自动压缩历史为摘要，Redis 持久化
- **新增多 Agent 协作**：Supervisor Agent 路由，分配给客服 Agent、分析 Agent、检索 Agent
- **新增 Agent 可观测性**：token 消耗/延迟/工具调用成功率追踪与统计
- **升级工具链**：新增 Web 搜索工具、计算工具，丰富 Agent 可用能力

### 独立模块 — LoRA 微调
- **数据集构造**：从客服场景构造 SFT 训练数据（~500 条指令对）
- **云端微调**：使用阿里云 PAI / ModelScope 平台对 qwen-7b-chat 做 LoRA 微调
- **效果对比**：base model vs 微调模型的 A/B 对比评测
- **前端展示**：新增微调效果对比 Demo 页

### 前端升级
- **升级 RAG Demo 页**：新增分块策略切换、检索管道可视化（Query改写→双路召回→Rerank→生成）
- **升级 Agent Demo 页**：ReAct 循环步骤可视化、多 Agent 协作流程图
- **升级智能客服 Demo 页**：接入真实 RAG + ReAct Agent
- **新增微调对比 Demo 页**：双模型并排对话对比

## Capabilities

### New Capabilities
- `rag-advanced-retrieval`: RAG 高级检索管道 — Hybrid Search、RRF 融合、LLM Reranking、Query 改写
- `rag-evaluation`: RAG 质量评估框架 — QA 测试集、Recall@K、命中率、端到端答案质量评分
- `rag-chunking-strategies`: 多分块策略引擎 — Token/递归字符/语义分块的切换与对比
- `agent-react-loop`: Agent ReAct 推理引擎 — 显式的思考→行动→观察循环、自主终止判断
- `agent-memory-system`: Agent 高级记忆系统 — 对话摘要压缩、Redis 持久化、用户画像
- `agent-multi-coordination`: 多 Agent 协作框架 — Supervisor 路由、专家 Agent 分工、消息传递
- `agent-observability`: Agent 可观测性 — token/延迟/成功率追踪、执行轨迹记录
- `lora-finetuning`: 轻量级 LoRA 微调 — 数据集构造、云端训练、效果对比评测

### Modified Capabilities
（无已有 spec 需要修改，全部为新增）

## Impact

- **后端 (`agent-server`)**：新增 ~8 个 Service 类、~3 个 Controller、ReAct 引擎核心；`AiConfig` 大幅改造支持多模型路由 + 高级 Advisor 链；新增 Redis 依赖
- **前端 (`agent-web`)**：4 个 Demo 页面大幅升级，新增 1 个微调对比页面；JS 模块需新增 pipeline 可视化组件
- **配置 (`application.yml`)**：新增 Redis 连接、Rerank 参数、BM25 配置、Agent 策略配置
- **依赖**：新增 `spring-boot-starter-data-redis`、可能需 Lucene（BM25）；微调依赖阿里云 PAI/ModelScope 账号
- **基础设施**：`docker-compose.yml` 新增 Redis 容器；微调需阿里云账号和 API 额度
