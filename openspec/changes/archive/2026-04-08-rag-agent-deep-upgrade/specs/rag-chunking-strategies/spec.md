## ADDED Requirements

### Requirement: 多分块策略支持
系统 SHALL 支持至少 3 种文档分块策略，可通过配置切换。

#### Scenario: Token 分块
- **WHEN** 配置 `agent.rag.chunking-strategy=token`
- **THEN** 系统使用 TokenTextSplitter 按固定 token 数分块

#### Scenario: 递归字符分块
- **WHEN** 配置 `agent.rag.chunking-strategy=recursive`
- **THEN** 系统按段落→句子→字符逐级切分，优先保持语义完整

#### Scenario: 语义分块
- **WHEN** 配置 `agent.rag.chunking-strategy=semantic`
- **THEN** 系统使用 embedding 相似度判断分块边界，相邻句子语义差异大于阈值时切分

### Requirement: 分块参数可配置
系统 SHALL 支持通过配置文件调整分块参数。

#### Scenario: 调整块大小
- **WHEN** 配置 `agent.rag.chunk-size=500` 和 `agent.rag.chunk-overlap=100`
- **THEN** 系统使用 500 token 的块大小和 100 token 的重叠
