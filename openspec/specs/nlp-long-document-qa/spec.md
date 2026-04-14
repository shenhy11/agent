## ADDED Requirements

### Requirement: 长文档自动分段
系统 SHALL 对超过 4000 字的文本自动走 Map-Reduce 处理路径。

#### Scenario: 自动分段
- **WHEN** 输入文本超过 4000 字
- **THEN** 系统按段落或固定窗口（2000 字/段，200 字重叠）切分为多段

#### Scenario: 短文档直接处理
- **WHEN** 输入文本不超过 4000 字
- **THEN** 系统直接处理，不走 Map-Reduce

### Requirement: Map 阶段独立处理
系统 SHALL 对每段文本独立执行 NLP 任务（摘要/QA）。

#### Scenario: 各段独立摘要
- **WHEN** 执行长文档摘要
- **THEN** 每段独立生成摘要，并通过 SSE 实时推送每段的处理进度

### Requirement: Reduce 阶段合并
系统 SHALL 使用 LLM 将各段结果合并为最终答案。

#### Scenario: 摘要合并
- **WHEN** 所有段摘要完成
- **THEN** LLM 对所有段摘要做最终合并，产出一份完整摘要

#### Scenario: 问答合并
- **WHEN** 对长文档提问
- **THEN** 从各段中找到相关段落的答案片段，LLM 合并为完整回答
