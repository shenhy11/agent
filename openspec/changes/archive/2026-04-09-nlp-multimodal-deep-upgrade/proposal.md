## Why

ChronoTech AI Lab 的 NLP 和多模态模块目前处于"prompt-only"浅层状态：NLP 4 个接口全靠 prompt 调大模型（无传统 NLP 技术栈对比、无 pipeline 编排）；多模态仅有单图单问的简单问答（复用主 chatClient 而非独立视觉模型、无 OCR/图像检索等深度能力）。需要将这两个方向升级为可"讲清原理+展示技术深度"的生产级水平，补充传统 NLP vs LLM 对比、NLP pipeline 编排、多图对话、OCR+LLM 串联、以图搜图等深度能力。

## What Changes

### 后端 — NLP 深化
- **新增 NLP Pipeline 引擎**：支持多步 NLP 任务编排（如：分词→NER→情感分析→摘要的串联流水线）
- **新增 NER 命名实体识别**：提取手表型号、价格、品牌等结构化实体
- **新增文本相似度计算**：基于 embedding 的文本语义相似度接口
- **新增 Intent 识别**：用户意图分类（咨询/投诉/对比/购买），支持 prompt-based 和规则引擎对比
- **升级现有 NLP 接口**：支持批量处理、流式输出、结构化 JSON Schema 校验
- **新增长文档智能问答**：对超长文本自动分段→逐段处理→合并结果（Map-Reduce 模式）

### 后端 — 多模态深化
- **激活独立视觉模型**：配置真正的 `visionChatClient`（qwen-vl-max），不再复用主 ChatClient
- **新增多图对话**：支持单次上传多张图片进行对比分析
- **新增 OCR + LLM 串联**：图片 OCR 提取文字 → LLM 理解分析（手表表盘/说明书识别）
- **新增以图搜图**：上传图片 → 提取视觉特征描述 → 在产品库中语义匹配
- **新增图片描述生成**：输入图片生成结构化描述（物体、颜色、场景等）
- **升级图文对话**：支持多轮图文对话（保持图片上下文）

### 前端升级
- **升级 NLP Demo 页**：新增 NER 可视化（实体高亮标注）、Pipeline 编排可视化（拖拽组合步骤）、Intent 识别展示
- **升级多模态 Demo 页**：多图上传区域、OCR 结果对照面板、以图搜图结果展示
- **新增长文档问答 Demo**：上传长文 → 分段可视化 → 逐段处理进度 → 合并答案

## Capabilities

### New Capabilities
- `nlp-pipeline-engine`: NLP Pipeline 编排引擎 — 多步 NLP 任务的串联执行、可视化和自定义组合
- `nlp-ner-extraction`: 命名实体识别 — 从文本中提取产品型号、价格、品牌等结构化实体
- `nlp-intent-classification`: 意图识别 — 用户输入的意图分类，支持 prompt-based 和规则引擎对比
- `nlp-long-document-qa`: 长文档智能问答 — Map-Reduce 模式处理超长文本
- `multimodal-vision-upgrade`: 多模态视觉升级 — 独立视觉模型配置、多图对话、多轮图文上下文
- `multimodal-ocr-pipeline`: OCR + LLM 串联管道 — 图片文字提取与智能分析
- `multimodal-image-search`: 以图搜图 — 基于视觉特征描述的语义匹配搜索

### Modified Capabilities
（无已有 spec 需要修改）

## Impact

- **后端 (`agent-server`)**：新增 ~6 个 Service、~3 个 Controller；`AiConfig` 需新增 `visionChatClient` Bean；可能需新增 Tess4J/PaddleOCR 依赖
- **前端 (`agent-web`)**：3 个 Demo 页面大幅升级；NLP 新增实体标注和 pipeline 编排 UI 组件
- **配置**：`application.yml` 新增 qwen-vl-max 独立模型配置、OCR 引擎配置
- **依赖**：可能新增 OCR 相关依赖（优先用 LLM 视觉能力替代传统 OCR，降低依赖复杂度）
