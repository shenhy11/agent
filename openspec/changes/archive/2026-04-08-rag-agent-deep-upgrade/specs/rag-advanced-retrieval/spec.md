## ADDED Requirements

### Requirement: Query 改写
系统 SHALL 在检索前对用户原始问题进行改写，补充上下文信息、修正表述歧义，提升检索召回质量。

#### Scenario: 简短问题扩展
- **WHEN** 用户输入"X2 Pro 续航"
- **THEN** 系统改写为"ChronoTech Sport X2 Pro 智能手表的电池续航时间是多少？"

#### Scenario: 模糊问题澄清
- **WHEN** 用户输入"防水怎么样"
- **THEN** 系统改写为"ChronoTech 手表各型号的防水等级和适用场景是什么？"

### Requirement: 双路召回
系统 SHALL 同时使用向量语义检索和 BM25 关键词检索两条通路召回候选文档。

#### Scenario: 向量检索通路
- **WHEN** 执行检索
- **THEN** 系统通过 VectorStore 返回 top-K 语义相似文档

#### Scenario: BM25 检索通路
- **WHEN** 执行检索
- **THEN** 系统通过 Lucene BM25 索引返回 top-K 关键词匹配文档

#### Scenario: 双路结果合并
- **WHEN** 两路检索均返回结果
- **THEN** 系统使用 RRF（Reciprocal Rank Fusion）算法融合排序，K=60

### Requirement: LLM Reranking
系统 SHALL 对融合后的候选文档使用 LLM 进行二级排序，筛选出最相关的 top-N 结果。

#### Scenario: Rerank 排序
- **WHEN** RRF 融合后有 20 条候选文档
- **THEN** 系统调用 LLM 对每条文档与问题的相关性打分（1-10），返回得分最高的 top-5

#### Scenario: Rerank 超时降级
- **WHEN** LLM Rerank 调用超时（>5s）
- **THEN** 系统降级使用 RRF 排序结果，跳过 Rerank

### Requirement: 检索来源溯源
系统 SHALL 在回答中提供每条引用来源的详细信息。

#### Scenario: 带溯源的回答
- **WHEN** RAG 生成回答
- **THEN** 返回结果包含引用来源列表，每条包含：文件名、chunk 序号、相似度分数、内容摘要

### Requirement: 管道可配置
系统 SHALL 支持通过配置开关独立启用/禁用管道的每个阶段。

#### Scenario: 关闭 Query 改写
- **WHEN** 配置 `agent.rag.query-rewrite-enabled=false`
- **THEN** 系统跳过 Query 改写步骤，直接使用原始问题检索

#### Scenario: 关闭 Rerank
- **WHEN** 配置 `agent.rag.rerank-enabled=false`
- **THEN** 系统跳过 LLM Rerank，使用 RRF 融合结果直接生成
