## Context

ChronoTech AI Lab 当前 NLP 和多模态模块状态：
- **NLP**：4 个端点全靠 prompt engineering（summarize/sentiment/keywords/translate），返回非结构化 JSON 字符串，无类型校验，无批量处理，无 pipeline 编排
- **多模态**：`VisionService` 复用主 `chatClient`（qwen-plus），未接入真正的视觉模型（qwen-vl-max）；仅支持单图单问同步/流式；无 OCR、无多图对比、无图片检索
- **前端**：`demo-nlp.html` 四 Tab 基础布局；`demo-vision.html` 单图拖拽上传

**约束**：无本地 GPU；OCR 优先利用 qwen-vl-max 的视觉理解能力替代传统 OCR 引擎，减少依赖。

## Goals / Non-Goals

**Goals:**
- NLP 做到 pipeline 编排 + NER + 意图识别 + 长文档处理 的深度
- 多模态做到独立视觉模型 + 多图对话 + OCR+LLM + 以图搜图 的深度
- 所有 NLP 过程可视化（pipeline 执行流程、NER 实体标注）
- 每个模块能"讲清楚 LLM-based NLP vs 传统 NLP 的 tradeoff"

**Non-Goals:**
- 不做传统 ML 模型训练（如 CRF/BiLSTM 做 NER）
- 不做语音识别/合成
- 不做视频分析
- 不做本地 OCR 引擎部署（Tess4J/PaddleOCR），全用 LLM 视觉能力

## Decisions

### D1: NLP Pipeline 架构 — 责任链模式

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│  分词    │───▶│  NER    │───▶│ 意图    │───▶│  摘要   │
│ Tokenize │    │ Extract │    │ Intent  │    │ Summary │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
     │              │              │              │
     ▼              ▼              ▼              ▼
  分词结果       实体列表       意图分类       摘要文本
```

**实现**：定义 `NlpStep` 接口，每个步骤实现该接口。`NlpPipeline` 按配置顺序串联执行。每步的输出作为下一步的上下文增强输入。

**替代方案**：每个 NLP 功能独立调用（现有方案）。选择 pipeline 是因为展示编排能力 + 让步骤间可以共享上下文。

### D2: NER 实现 — LLM Structured Output

**选择**：通过精心设计的 prompt + JSON Schema 校验让 LLM 返回结构化实体：

```json
{
  "entities": [
    {"text": "Sport X2 Pro", "type": "PRODUCT", "start": 12, "end": 24},
    {"text": "¥1999", "type": "PRICE", "start": 30, "end": 35},
    {"text": "防水50米", "type": "SPEC", "start": 42, "end": 48}
  ]
}
```

**实体类型**：PRODUCT（产品）、BRAND（品牌）、PRICE（价格）、SPEC（规格）、TIME（时间）、PERSON（人名）

**替代方案**：用 Spacy/HanLP 做传统 NER（更精确但需额外依赖 Python 服务）。选择 LLM-based 是因为无需额外基础设施，且在开放领域效果更好。

### D3: 意图识别 — 双模式对比

**选择**：同时实现两种方式，前端可切换对比：

| 模式 | 实现 | 优缺点 |
|------|------|--------|
| Prompt-based | LLM 直接分类 | 灵活但慢、费 token |
| 规则引擎 | 关键词+正则匹配 | 快但不灵活 |

**意图类别**：CONSULT（产品咨询）、COMPARE（对比）、COMPLAINT（投诉）、PURCHASE（购买）、SUPPORT（售后）、OTHER（其他）

### D4: 长文档 Map-Reduce

```
长文档（>4000字）
       │
       ▼
┌────────────────┐
│  分段            │  按段落或固定窗口切分
│  (Map 阶段)     │
└──────┬─────────┘
       │
  ┌────┼────┐
  ▼    ▼    ▼
┌───┐┌───┐┌───┐
│ S1││ S2││ S3│  各段独立处理（摘要/QA）
└─┬─┘└─┬─┘└─┬─┘
  │    │    │
  └────┼────┘
       ▼
┌────────────────┐
│  合并            │  LLM 合并各段结果
│  (Reduce 阶段)  │
└────────────────┘
       ▼
   最终答案
```

**实现**：对超过 4000 字的文本自动走 Map-Reduce 路径，前端展示分段进度。

### D5: 多模态视觉模型 — 独立 Bean

**选择**：在 `AiConfig` 中新增 `visionChatClient` Bean，通过 DashScope 配置 `qwen-vl-max`。

**实现细节**：
- 使用 `@Qualifier("visionChatClient")` 注入到 `VisionService`
- 支持多图输入：`UserMessage` 中附加多个 `Media` 对象
- 保持多轮图文上下文：用独立的 `ChatMemory` 存储图片引用

### D6: OCR 策略 — LLM 视觉理解替代传统 OCR

**选择**：不部署传统 OCR 引擎（Tess4J/PaddleOCR），直接用 qwen-vl-max 的视觉理解能力做"OCR"。

**Prompt 模板**：
```
请仔细观察图片中的所有文字内容，原样提取：
1. 逐行列出所有可见文字
2. 对表格/表盘数据保持结构化
3. 以 JSON 返回: {"text_blocks": [...], "structured_data": {...}}
```

**替代方案**：Tess4J（需本地安装 Tesseract，中文识别率不高）。选择 LLM 是因为无需额外依赖，且能"理解"而不只是"识别"文字。

### D7: 以图搜图 — 视觉描述 + 语义匹配

```
上传图片
    │
    ▼
┌──────────────────┐
│ qwen-vl-max      │  生成结构化描述
│ 图片描述生成      │  (颜色/材质/表盘/系列)
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ 描述文本 embedding │  向量化描述
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ 产品描述库匹配    │  在预置产品描述向量中搜索
└────────┬─────────┘
         ▼
   匹配产品列表
```

**实现**：预置 6 款产品的结构化描述，上传图片后生成描述并做向量匹配。

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|------|------|------|
| qwen-vl-max API 延迟高（2-5s） | 多模态 Demo 体验差 | 前端加 loading 动画 + streaming |
| LLM-based NER 精度不如传统模型 | 实体提取遗漏 | 设计 few-shot prompt 提升精度 |
| LLM-based OCR 对小字/模糊图效果差 | OCR 结果不完整 | 前端提示推荐清晰图片 |
| 多图对话 token 消耗高 | API 成本上升 | 限制单次最多 3 张图 |
| Pipeline 步骤串联增加延迟 | NLP 响应慢 | 每步独立 SSE 推送，实时展示进度 |

## Open Questions

- qwen-vl-max 的多图输入在 Spring AI Alibaba 中是否支持？需 spike 验证
- LLM-based NER 在中文环境的 few-shot prompt 最佳实践？
