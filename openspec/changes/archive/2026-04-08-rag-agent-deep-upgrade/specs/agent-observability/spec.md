## ADDED Requirements

### Requirement: Token 消耗追踪
系统 SHALL 追踪每次 AI 调用的 token 消耗（input + output）。

#### Scenario: 单次调用 token 统计
- **WHEN** 完成一次 LLM 调用
- **THEN** 系统记录 prompt_tokens、completion_tokens、total_tokens 和模型名称

#### Scenario: 会话级 token 汇总
- **WHEN** 前端请求会话统计
- **THEN** 系统返回该会话累计的 token 消耗和估算费用

### Requirement: 延迟追踪
系统 SHALL 追踪 AI 管道各阶段的耗时。

#### Scenario: 端到端延迟
- **WHEN** 完成一次完整的请求处理
- **THEN** 系统记录总耗时及各阶段耗时（Query改写、检索、Rerank、生成）

#### Scenario: 前端可视化
- **WHEN** 前端请求延迟数据
- **THEN** 系统返回各阶段耗时的 JSON，前端可渲染为瀑布图

### Requirement: 工具调用成功率
系统 SHALL 统计 Agent 工具调用的成功率和失败原因。

#### Scenario: 成功率统计
- **WHEN** 请求工具调用统计
- **THEN** 系统返回每个工具的调用次数、成功次数、失败次数、平均耗时
