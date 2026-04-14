## ADDED Requirements

### Requirement: 文本摘要接口
系统 SHALL 提供 `POST /api/nlp/summarize` 接口，将长文本压缩为简短摘要。

#### Scenario: 正常摘要请求
- **WHEN** 前端提交包含 text 字段的 JSON（文本长度 > 100 字符）
- **THEN** 返回 AI 生成的摘要文本（≤ 原文 30% 长度）

#### Scenario: 文本过短
- **WHEN** 前端提交 text 长度 < 20 字符
- **THEN** 返回原文本不做处理，附带提示信息

### Requirement: 情感分析接口
系统 SHALL 提供 `POST /api/nlp/sentiment` 接口，分析文本的情感倾向。

#### Scenario: 正面情感分析
- **WHEN** 前端提交 "这款手表续航特别长，非常满意！"
- **THEN** 返回情感标签 "positive"、置信度分数（0-1）、关键词

#### Scenario: 负面情感分析
- **WHEN** 前端提交 "手表质量太差了，屏幕一周就坏了"
- **THEN** 返回情感标签 "negative"、置信度分数、关键词

### Requirement: 关键词提取接口
系统 SHALL 提供 `POST /api/nlp/keywords` 接口，从文本中提取关键词和短语。

#### Scenario: 提取产品评论关键词
- **WHEN** 前端提交一段产品评论文本
- **THEN** 返回关键词数组（≤ 10 个），每个包含词语和权重

### Requirement: 多语翻译接口
系统 SHALL 提供 `POST /api/nlp/translate` 接口，支持中英双向翻译。

#### Scenario: 中文翻译为英文
- **WHEN** 前端提交 text="智能科技，定义未来" 和 targetLang="en"
- **THEN** 返回英文翻译结果

#### Scenario: 英文翻译为中文
- **WHEN** 前端提交 text="Redefine Time" 和 targetLang="zh"
- **THEN** 返回中文翻译结果

### Requirement: NLP 统一通过 LLM Prompt 实现
所有 NLP 能力 SHALL 通过 prompt engineering 调用 qwen-plus 模型实现，不引入独立的 NLP 模型/库。

#### Scenario: NLP 请求路由到正确模型
- **WHEN** 任意 NLP 接口被调用
- **THEN** 系统通过专门的 prompt 模板调用 qwen-plus，结果以结构化 JSON 返回
