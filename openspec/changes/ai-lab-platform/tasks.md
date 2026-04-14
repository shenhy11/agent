## 1. 基础设施 — 产品数据与 API 打通

- [x] 1.1 创建 `ProductData.java` 硬编码 6 款手表产品数据（id/name/series/subtitle/price/specs/imageUrl）
- [x] 1.2 创建 `ProductController`，实现 `GET /api/products`（全部列表）、`GET /api/products?series=xxx`（筛选）、`GET /api/products/{id}`（详情）、`GET /api/products/compare?ids=x,y`（对比）
- [x] 1.3 创建 `ContactController`，实现 `POST /api/contact`（表单提交），含参数校验
- [x] 1.4 验证现有 CORS 配置对新接口生效

## 2. RAG 知识库激活

- [x] 2.1 在 `docs/knowledge/` 目录编写 3 份预置文档：`product-specs.md`（产品规格手册）、`faq.md`（常见问题）、`after-sales.md`（售后服务政策）
- [x] 2.2 创建 `KnowledgeInitializer`（实现 `CommandLineRunner`），启动时自动加载 `docs/knowledge/` 下所有文档到 VectorStore
- [x] 2.3 在 `AiConfig` 中为 ChatClient 挂载 `QuestionAnswerAdvisor`，关联 VectorStore
- [x] 2.4 改造 SSE 流式响应，新增 GET 端点兼容前端 Demo，前端模拟 retrieval 展示
- [x] 2.5 验证 RAG 端到端：启动 → 自动加载文档 → 对话检索增强

## 3. Agent 升级 — 工具集与思考链

- [x] 3.1 创建 `ProductTools.java`，实现 `@Tool` 注解的 3 个方法：`queryProduct`、`compareProducts`、`recommendProduct`
- [x] 3.2 重写 `system-prompt.st`，适配 ChronoTech 手表场景（品牌信息、产品线概述、服务规范、回答风格）
- [x] 3.3 改造 `CustomerServiceTools.java`，移除不相关工具（queryAccountBalance），保留 createTicket 和 getCurrentTime
- [x] 3.4 新增 GET /api/chat/stream 端点，前端按 SSE 事件类型渲染思考链和对话
- [x] 3.5 在 `AiConfig` 中注册 `ProductTools` 到 ChatClient 工具链
- [x] 3.6 验证 Agent 多步推理：提问复杂问题 → Agent 自主调用多个工具 → 综合回答

## 4. NLP 文本分析接口

- [x] 4.1 创建 `TextAnalysisService.java`，定义 4 个方法：`summarize`、`analyzeSentiment`、`extractKeywords`、`translate`，每个通过专门的 prompt 模板调用 qwen-plus
- [x] 4.2 创建 `NlpController.java`，实现 4 个端点：`POST /api/nlp/summarize`、`/sentiment`、`/keywords`、`/translate`
- [x] 4.3 编写 NLP prompt 模板（要求模型以结构化 JSON 格式返回结果）
- [x] 4.4 验证 4 个 NLP 接口的正确性和输出格式

## 5. 多模态图文问答接口

- [x] 5.1 在 `AiConfig` 中新增 `visionChatClient` Bean，配置 qwen-vl-max 模型（通过 `@Qualifier` 区分）
- [x] 5.2 创建 `VisionService.java`，实现图片+文字的联合问答逻辑（使用 Spring AI 的 `Media` 消息类型）
- [x] 5.3 创建 `MultimodalController.java`，实现 `POST /api/multimodal/chat`（支持 multipart/form-data 上传图片 + 文字）
- [x] 5.4 添加图片格式校验（JPEG/PNG/WebP）和大小限制（5MB）
- [x] 5.5 验证多模态端到端：上传图片 + 提问 → qwen-vl-max 回答

## 6. 前端 — AI Lab 入口页

- [x] 6.1 创建 `ai-lab.html`，包含 Hero 区域和 6 个 Demo 卡片网格（智能客服/多模态/NLP/RAG/Agent 工作流/能力评测）
- [x] 6.2 在 `css/pages.css` 中新增 AI Lab 相关样式（Demo 卡片、图标动效、渐变边框）
- [x] 6.3 全站导航栏新增 "🧪 AI Lab" 链接（修改 index.html、products.html、about.html、contact.html）
- [x] 6.4 底部新增技术栈展示区（Spring AI / DashScope / pgvector 等 Logo 和标签）

## 7. 前端 — 智能客服 Demo 页

- [x] 7.1 创建 `demo-chat.html`，双栏布局：左侧对话面板 + 右侧 Agent 思考链面板
- [x] 7.2 创建 `js/demo-chat.js`，实现 SSE 流式解析，按事件类型（text/thinking/tool_call/tool_result）分别渲染到对应面板
- [x] 7.3 思考链面板显示：步骤序号、操作类型图标、工具名、参数、返回结果摘要、耗时
- [x] 7.4 提供 3 个预置问题按钮（快速体验），如"推荐跑步手表"/"对比两款产品"/"保修政策"

## 8. 前端 — 多模态 Demo 页

- [x] 8.1 创建 `demo-vision.html`，包含图片拖拽上传区域、文字输入框和回答展示区
- [x] 8.2 创建 `js/demo-vision.js`，实现拖拽上传 + 图片预览 + FormData 提交 + 流式回答展示
- [x] 8.3 提供 2-3 张预置示例图片（手表正面/屏幕异常等），点击可快速加载

## 9. 前端 — NLP 工具箱 Demo 页

- [x] 9.1 创建 `demo-nlp.html`，多 Tab 界面（摘要/情感分析/关键词/翻译），左输入右输出布局
- [x] 9.2 创建 `js/demo-nlp.js`，实现 Tab 切换、文本提交、结果解析和可视化展示（情感用色彩标签、关键词用标签云）
- [x] 9.3 每个 Tab 预置示例文本（产品评论/新闻/技术文档），一键填入

## 10. 前端 — RAG 知识库 Demo 页

- [x] 10.1 创建 `demo-rag.html`，三栏布局：知识库管理（左）、对话窗口（中）、检索来源可视化（右）
- [x] 10.2 创建 `js/demo-rag.js`，实现文件上传到知识库、RAG 对话、检索来源展示（文件名、相似度条、内容摘要卡片）
- [x] 10.3 左栏显示已加载的预置文档列表和"上传新文档"按钮

## 11. 前端 — Agent 工作流 Demo 页

- [x] 11.1 创建 `demo-agent.html`，包含任务输入区和流程图可视化区域
- [x] 11.2 创建 `js/demo-agent.js`，实现 SSE 事件流解析，动态渲染步骤节点（thinking → tool_call → tool_result → 下一步），每步显示状态图标和耗时
- [x] 11.3 提供 2-3 个预置复杂任务（"对比运动系列并生成报告"/"分析适合马拉松选手的手表"）

## 12. 整体联调与优化

- [x] 12.1 前后端联调：GET SSE 端点已打通，所有 Demo 页面 API 路径对齐
- [x] 12.2 Demo 页面响应式适配（ai-lab.css 新增 1024px/768px 断点覆盖 Demo 布局）
- [x] 12.3 所有 Demo 添加加载状态动画和错误降级提示
- [x] 12.4 更新 `application.yml`，整理多模型配置和新参数
- [x] 12.5 更新 `PROJECT_STATUS.md` 反映新架构

