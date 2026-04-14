## ADDED Requirements

### Requirement: 独立视觉模型配置
系统 SHALL 配置独立的 `visionChatClient` Bean，使用 qwen-vl-max 模型。

#### Scenario: 独立 Bean 注入
- **WHEN** VisionService 初始化
- **THEN** 通过 @Qualifier("visionChatClient") 注入独立的视觉 ChatClient

### Requirement: 多图对话
系统 SHALL 支持单次上传最多 3 张图片进行对比分析。

#### Scenario: 双图对比
- **WHEN** 用户上传两张手表图片并问"这两款有什么区别？"
- **THEN** 系统同时分析两张图片并给出对比回答

#### Scenario: 超限拒绝
- **WHEN** 用户上传超过 3 张图片
- **THEN** 系统返回提示"单次最多支持 3 张图片"

### Requirement: 多轮图文对话
系统 SHALL 支持在对话中保持图片上下文，后续追问可引用之前的图片。

#### Scenario: 图片上下文保持
- **WHEN** 用户第 1 轮上传图片并问"这是什么手表？"，第 2 轮追问"它防水吗？"
- **THEN** 系统在第 2 轮仍能引用第 1 轮的图片回答

### Requirement: 图片描述生成
系统 SHALL 支持输入图片生成结构化描述。

#### Scenario: 结构化描述
- **WHEN** 用户上传手表图片并请求描述
- **THEN** 返回结构化 JSON：{type, color, material, display, brand_guess, scene}
