## Why

ChronoTech 项目目前是一个手表品牌官网 + 基础 AI 客服，但前后端之间存在"断裂带"（前端硬编码、后端 API 缺失），AI 能力仅停留在单轮对话层面。需要将项目升级为 **AI 能力展示平台**，以手表业务为实战载体，集中展示大模型、NLP、多模态、RAG、智能体等 AI 技术栈的深度应用能力。

目标受众为**面试官/甲方**，需要兼顾"技术深度透明"和"视觉冲击力"。

## What Changes

### 后端新增
- 新增 `ProductController` + `ContactController`，打通前后端数据链路
- **激活 RAG 管道**：挂载 `QuestionAnswerAdvisor`、预置产品文档、检索结果可视化
- **新增 NLP 接口**：文本摘要、情感分析、关键词提取、多语翻译（统一通过 DashScope qwen-plus）
- **新增多模态接口**：图片+文字联合问答（通过 DashScope qwen-vl-max）
- **升级 Agent**：工具调用过程可视化、多步推理链暴露给前端、产品查询/对比工具
- **新增产品工具集**：`ProductTools`（查询规格、对比产品、个性化推荐）
- 改造 `system-prompt` 适配手表场景，注入产品知识

### 前端新增
- 新增 **AI Lab 入口页** (`ai-lab.html`)：6 大 Demo 卡片导航
- 新增 **智能客服 Demo 页**：对话面板 + Agent 思考链可视化
- 新增 **多模态 Demo 页**：图片上传 + 图文问答交互
- 新增 **NLP 工具箱 Demo 页**：多 Tab 切换的文本分析工具
- 新增 **RAG 知识库 Demo 页**：文档管理 + 对话 + 检索来源可视化
- 新增 **Agent 工作流 Demo 页**：多步任务执行过程的流程图可视化
- 导航栏新增 "🧪 AI Lab" 入口

### 数据预置
- 手动编写 ChronoTech 产品文档 (Markdown/PDF)，预置到 RAG 知识库

## Capabilities

### New Capabilities
- `rag-knowledge-base`: RAG 知识库管道的激活与展示 — 文档预置、向量化、检索增强生成、来源追溯可视化
- `nlp-text-analysis`: NLP 文本分析工具箱 — 摘要、情感分析、关键词提取、翻译等能力的统一接口与展示
- `multimodal-vision`: 多模态图文理解 — 图片上传 + 文字提问的联合推理能力（qwen-vl）
- `agent-orchestration`: 智能体编排升级 — 多步推理、工具调用可视化、Agent 思考链暴露
- `product-api`: 产品数据 API — 产品列表/详情/对比的后端接口，打通前后端
- `ai-lab-frontend`: AI Lab 前端展厅 — 6 大 Demo 页面的统一入口和交互界面

### Modified Capabilities
（无已有 spec 需要修改）

## Impact

- **后端 (`agent-server`)**：新增 4 个 Controller、3 个 Service、2 个 Tools 类；`AiConfig` 需改造支持多模型；`pom.xml` 可能需加入 qwen-vl 相关依赖
- **前端 (`agent-web`)**：新增 6+ 个 HTML 页面、对应的 CSS 和 JS 模块；导航栏改造
- **配置 (`application.yml`)**：新增 qwen-vl-max 模型配置、RAG advisor 参数
- **数据**：新增 `docs/knowledge/` 目录存放预置产品文档
- **依赖**：DashScope API Key 需要支持多模态调用权限
