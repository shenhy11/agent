## ADDED Requirements

### Requirement: 双模式意图识别
系统 SHALL 同时提供 LLM-based 和规则引擎两种意图识别方式，支持对比。

#### Scenario: LLM 意图识别
- **WHEN** 使用 LLM 模式且用户输入"Sport X2 Pro 和 Elite E2 哪个好？"
- **THEN** 返回 {intent: "COMPARE", confidence: 0.95, method: "llm"}

#### Scenario: 规则引擎意图识别
- **WHEN** 使用规则引擎模式且用户输入包含"对比""哪个好""比较"关键词
- **THEN** 返回 {intent: "COMPARE", confidence: 0.80, method: "rule"}

#### Scenario: 双模式对比
- **WHEN** 前端请求对比模式
- **THEN** 系统同时执行两种方式并返回并排结果

### Requirement: 意图分类体系
系统 SHALL 支持 6 种意图类别：CONSULT、COMPARE、COMPLAINT、PURCHASE、SUPPORT、OTHER。

#### Scenario: 完整分类
- **WHEN** 任意用户输入
- **THEN** 系统返回最匹配的意图类别和置信度分数
