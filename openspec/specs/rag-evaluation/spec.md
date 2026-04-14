## ADDED Requirements

### Requirement: QA 测试集管理
系统 SHALL 支持加载和管理 QA 测试集（JSON 格式），每条包含问题、标准答案、相关文档标注。

#### Scenario: 加载测试集
- **WHEN** 指定 QA 测试集文件路径
- **THEN** 系统解析 JSON 并加载所有 QA 对，每条包含 question、expected_answer、relevant_doc_ids

### Requirement: Recall@K 计算
系统 SHALL 自动计算检索阶段的 Recall@K 指标。

#### Scenario: 评估检索召回率
- **WHEN** 对测试集所有问题执行检索
- **THEN** 系统计算 Recall@1、Recall@3、Recall@5，输出每个 K 值的平均召回率

### Requirement: 答案质量评估
系统 SHALL 使用 LLM 对生成的回答进行质量评估。

#### Scenario: 评估答案相关性
- **WHEN** RAG 生成回答后
- **THEN** 系统调用 LLM 对回答与标准答案的相关性打分（1-5），并输出评估理由

#### Scenario: 评估答案忠实度
- **WHEN** RAG 生成回答后
- **THEN** 系统检查回答是否仅基于检索到的文档内容，不产生幻觉

### Requirement: 配置对比实验
系统 SHALL 支持对比不同 RAG 配置的评估结果。

#### Scenario: A/B 对比
- **WHEN** 用户指定两组不同配置（如有无 Rerank）
- **THEN** 系统分别评估并输出对比报告，包含各指标差异
