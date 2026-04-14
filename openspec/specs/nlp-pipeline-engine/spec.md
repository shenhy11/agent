## ADDED Requirements

### Requirement: NLP 步骤接口定义
系统 SHALL 定义统一的 `NlpStep` 接口，每个 NLP 功能实现该接口。

#### Scenario: 步骤执行
- **WHEN** Pipeline 执行到某一步
- **THEN** 该步接收上一步的输出作为上下文增强输入，返回本步结果

### Requirement: Pipeline 串联编排
系统 SHALL 支持按用户配置顺序串联执行多个 NLP 步骤。

#### Scenario: 三步串联
- **WHEN** 用户选择"NER → 意图识别 → 摘要"三步 pipeline
- **THEN** 系统依次执行三步，每步结果传递给下一步，最终返回所有步骤的聚合结果

#### Scenario: 单步执行
- **WHEN** 用户只选择一个步骤（如仅摘要）
- **THEN** 系统直接执行该步骤，无需 pipeline 编排

### Requirement: Pipeline 执行可视化
系统 SHALL 通过 SSE 实时向前端推送 pipeline 每步的执行进度和结果。

#### Scenario: 实时进度
- **WHEN** Pipeline 执行过程中
- **THEN** 每完成一步发送 `event: nlp_step` 事件，包含步骤名、耗时、结果摘要
