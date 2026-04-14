## 1. 基础设施 — 视觉模型 Bean + 配置

- [x] 1.1 在 `AiConfig` 中新增 `visionChatClient` Bean，配置 qwen-vl-max 模型（通过 DashScope），使用 `@Qualifier("visionChatClient")` 区分
- [x] 1.2 修改 `VisionService` 构造函数，通过 `@Qualifier("visionChatClient")` 注入独立视觉 ChatClient
- [x] 1.3 `application.yml` 新增视觉模型独立配置（模型名、温度、超时）
- [x] 1.4 验证：调用 `/api/multimodal/chat` 确认走 qwen-vl-max 模型

## 2. NLP 深化 — Pipeline 编排引擎

- [x] 2.1 定义 `NlpStep` 接口：`execute(String input, Map<String, Object> context) -> NlpStepResult`
- [x] 2.2 将现有 4 个 NLP 功能重构为 `NlpStep` 实现（`SummarizeStep`、`SentimentStep`、`KeywordsStep`、`TranslateStep`）
- [x] 2.3 创建 `NlpPipeline.java`，支持按指定顺序串联执行多个 Step，每步输出作为下一步的上下文增强
- [x] 2.4 创建 `PipelineController.java`，提供 `POST /api/nlp/pipeline` 端点（接收步骤列表 + 文本）
- [x] 2.5 实现 SSE 流式推送：每完成一步发送 `event: nlp_step` 事件
- [x] 2.6 验证：执行三步 pipeline（NER→意图→摘要），前端实时渲染进度

## 3. NLP 深化 — NER 命名实体识别

- [x] 3.1 创建 `NerStep.java` 实现 `NlpStep` 接口，通过 few-shot prompt 让 LLM 返回带位置的实体
- [x] 3.2 设计 NER prompt 模板，支持 6 种实体类型：PRODUCT、BRAND、PRICE、SPEC、TIME、PERSON
- [x] 3.3 实现 JSON Schema 校验，确保返回格式包含 text/type/start/end 字段
- [x] 3.4 创建 `POST /api/nlp/ner` 独立端点
- [x] 3.5 验证：输入手表相关文本，返回正确标注的实体列表

## 4. NLP 深化 — 意图识别（双模式）

- [x] 4.1 创建 `IntentStep.java` 实现 `NlpStep`，LLM 模式通过 prompt 分类 6 种意图
- [x] 4.2 创建 `RuleBasedIntentService.java`，基于关键词+正则匹配的规则引擎意图识别
- [x] 4.3 创建 `IntentController.java`，提供 `POST /api/nlp/intent` 端点，支持 `mode=llm|rule|both` 参数
- [x] 4.4 实现双模式对比：mode=both 时同时返回两种方式的结果
- [x] 4.5 验证：6 种意图分类的准确性，对比两种方式的差异

## 5. NLP 深化 — 长文档 Map-Reduce

- [x] 5.1 创建 `LongDocumentService.java`，实现自动分段逻辑（>4000 字触发，2000 字/段，200 重叠）
- [x] 5.2 实现 Map 阶段：各段并行/串行执行 NLP 任务（摘要/QA）
- [x] 5.3 实现 Reduce 阶段：LLM 合并各段结果为最终答案
- [x] 5.4 实现 SSE 进度推送：每段完成发送进度事件（1/5、2/5...）
- [x] 5.5 创建 `POST /api/nlp/long-doc` 端点，支持长文档摘要和问答
- [x] 5.6 验证：输入 >4000 字文本，观察分段→Map→Reduce 完整流程

## 6. NLP 深化 — 升级现有接口

- [x] 6.1 升级 4 个现有 NLP 端点，支持批量处理（`POST /api/nlp/batch`，接收文本数组）
- [x] 6.2 新增文本相似度接口 `POST /api/nlp/similarity`，基于 embedding 计算两段文本的语义相似度
- [x] 6.3 为所有 NLP 接口添加输出 JSON Schema 校验，异常时返回友好错误
- [x] 6.4 验证：批量提交 5 段文本做摘要；相似度接口返回正确的 0-1 分数

## 7. 多模态深化 — 多图对话

- [x] 7.1 改造 `VisionService`，支持 `List<byte[]>` 多图输入（每个图片作为独立 Media 附件）
- [x] 7.2 改造 `MultimodalController`，`/api/multimodal/multi-chat` 接受多个 image 参数（最多 3 张）
- [x] 7.3 超限校验：超过 3 张返回 400 错误
- [x] 7.4 验证：上传 2 张手表图片 + 提问"对比这两款"，返回对比分析

## 8. 多模态深化 — 多轮图文对话

- [x] 8.1 为 `VisionService` 添加会话管理，使用独立的 `ChatMemory` 保持图文上下文
- [x] 8.2 实现图片引用存储：首轮图片 base64 缓存到内存/Redis，后续轮次可引用
- [x] 8.3 改造 `MultimodalController`，支持 `conversationId` 参数用于多轮延续
- [x] 8.4 验证：第 1 轮上传图片问"这是什么"，第 2 轮追问"它的价格呢"（不上传图片），能正确引用

## 9. 多模态深化 — OCR + LLM 串联

- [x] 9.1 创建 `OcrService.java`（内置于 `VisionService`），用 qwen-vl-max + 专用 prompt 从图片提取文字
- [x] 9.2 实现 OCR 结果结构化：text_blocks（逐行文字列表）+ structured_data（表格/KV 对）
- [x] 9.3 实现 OCR+LLM 串联：图片→OCR 提取→LLM 理解分析的两步管道
- [x] 9.4 创建 `POST /api/multimodal/ocr` 端点（纯 OCR）和 `POST /api/multimodal/ocr-analyze` 端点（OCR+分析）
- [x] 9.5 验证：上传手表表盘/说明书图片，返回提取的文字 + 分析结果

## 10. 多模态深化 — 以图搜图

- [x] 10.1 创建 `ImageSearchService.java`，实现图片→视觉描述→向量匹配→产品推荐 流程
- [x] 10.2 编写 6 款产品的结构化视觉描述文本，启动时加载到 VectorStore
- [x] 10.3 实现图片描述生成：调用 qwen-vl-max 生成结构化描述
- [x] 10.4 实现描述向量匹配：描述文本 embedding → VectorStore 相似度搜索 → 返回 top-3
- [x] 10.5 创建 `POST /api/multimodal/search-by-image` 端点
- [x] 10.6 验证：上传一张运动手表图片，返回匹配的 ChronoTech 产品列表

## 11. 前端 — NLP Demo 页升级

- [x] 11.1 升级 `demo-nlp.html`：新增 NER Tab，输入文本后实体高亮标注展示（每种类型不同颜色）
- [x] 11.2 新增 Intent Tab：双模式切换（LLM/规则/对比），展示分类结果和置信度
- [x] 11.3 新增 Pipeline Tab：可勾选组合 NLP 步骤，执行后实时展示每步进度和结果
- [x] 11.4 升级现有 Tab：加入批量处理入口和文本相似度对比功能
- [x] 11.5 新增长文档 Tab：上传/粘贴长文本，展示分段过程 + Map 进度 + Reduce 合并

## 12. 前端 — 多模态 Demo 页升级

- [x] 12.1 升级 `demo-vision.html`：支持多图上传区域（最多 3 张并排预览）
- [x] 12.2 新增 OCR 面板：左侧图片 + 右侧提取文字的对照展示
- [x] 12.3 新增以图搜图功能：上传图片 → 展示匹配产品卡片列表（含相似度分数）
- [x] 12.4 实现多轮图文对话 UI：对话列表中图片消息的展示和追问交互
- [x] 12.5 AI Lab 入口页更新 NLP 和多模态卡片的功能描述

## 13. 整体联调与验证

- [x] 13.1 NLP Pipeline + NER + Intent + 长文档全链路联调
- [x] 13.2 多模态：多图对话 + OCR + 以图搜图 全链路联调
- [x] 13.3 所有新增 Demo 页面的响应式适配
- [x] 13.4 错误降级验证：视觉模型不可用时的友好提示
- [x] 13.5 更新 `application.yml` 整理新增配置

## 14. 多模态深化 — 图片描述生成（独立端点）

- [x] 14.1 创建 `ImageDescriptionService.java`（内置于 `VisionService`），用 qwen-vl-max + 结构化 prompt 生成图片描述（颜色/材质/外观/场景/风格）
- [x] 14.2 设计 JSON Schema 返回格式：`{ "summary": "...", "attributes": { "color": [], "material": "...", "style": "..." } }`
- [x] 14.3 创建 `POST /api/multimodal/describe` 独立端点
- [x] 14.4 前端 `demo-vision.html` 新增"图片描述"Tab，展示结构化属性标签卡片
- [x] 14.5 验证：上传手表图片，返回包含表盘颜色、表带材质、风格系列的结构化描述

## 15. 安全限制与成本防护

- [x] 15.1 长文档接口（`/api/nlp/long-doc`）设置最大字数上限（50000 字），超出返回 400 提示
- [x] 15.2 NLP Pipeline 接口限制最大步骤数（5 步），防止 token 暴涨
- [x] 15.3 多图上传 token 消耗预估，超出阈值拒绝并提示用户压缩图片
- [x] 15.4 图片 base64 Redis 缓存设计：统一 TTL 配置（默认 30 分钟），大图（>500KB）转存策略
- [x] 15.5 为所有视觉模型调用添加超时保护（默认 30s），超时返回友好降级响应

## 16. 前端可用性与引导体验

- [x] 16.1 NLP Pipeline Tab 新增步骤说明引导卡（每种 Step 配图标 + 一句话介绍）
- [x] 16.2 NER 实体高亮结果面板做移动端 overflow 处理，防止长实体截断
- [x] 16.3 多轮图文对话 UI 增加"会话历史"侧栏，支持切换和清空历史会话
- [x] 16.4 以图搜图结果卡片新增"相似度分数"进度条可视化（0-100%）
- [x] 16.5 所有新增 Tab 入口在 AI Lab 主页（`ai-lab.html`）更新功能描述文案

