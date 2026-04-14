## ADDED Requirements

### Requirement: SFT 数据集构造
系统 SHALL 构造客服领域的 SFT 训练数据集，格式符合 ModelScope 微调要求。

#### Scenario: 数据集生成
- **WHEN** 执行数据集构造
- **THEN** 系统生成 ~500 条 ChronoTech 客服场景的指令对，JSON Lines 格式包含 instruction/input/output 三字段

#### Scenario: 数据质量
- **WHEN** 检查数据集
- **THEN** 数据涵盖产品咨询、售后服务、对比推荐、故障排查等多种场景，语言风格符合品牌调性

### Requirement: 云端 LoRA 微调
系统 SHALL 支持通过 ModelScope 平台对 qwen-7b-chat 进行 LoRA 微调。

#### Scenario: 微调配置
- **WHEN** 准备微调
- **THEN** 提供微调参数配置文档：LoRA rank=8、alpha=32、learning_rate=2e-4、epochs=3

#### Scenario: 微调执行
- **WHEN** 上传数据集到 ModelScope
- **THEN** 启动 LoRA 微调任务，训练完成后可通过 API 调用微调后模型

### Requirement: A/B 效果对比
系统 SHALL 支持 base model 与微调模型的并排对比评测。

#### Scenario: 并排对话对比
- **WHEN** 用户在对比页面发送问题
- **THEN** 系统同时调用 base model（qwen-plus）和微调模型，并排展示两个回答

#### Scenario: 量化评测
- **WHEN** 使用预置测试题评测
- **THEN** 系统输出两个模型在准确性、专业性、响应速度的对比数据
