## ADDED Requirements

### Requirement: 图文联合问答接口
系统 SHALL 提供 `POST /api/multimodal/chat` 接口，支持用户上传图片并附带文字提问，AI 理解图文内容后给出回答。

#### Scenario: 上传手表图片识别型号
- **WHEN** 用户上传一张手表照片并提问 "这是什么型号的手表？"
- **THEN** AI 基于图片内容回答手表的可能型号和特征描述

#### Scenario: 上传故障截图进行诊断
- **WHEN** 用户上传一张手表屏幕异常的照片并提问 "屏幕显示异常怎么办？"
- **THEN** AI 分析图片中的异常表现并给出诊断建议和解决方案

### Requirement: 多模态模型路由
系统 SHALL 将包含图片的请求路由到 qwen-vl-max 模型，纯文本请求仍走 qwen-plus。

#### Scenario: 图片请求路由到 VL 模型
- **WHEN** `/api/multimodal/chat` 收到包含图片的请求
- **THEN** 系统使用 qwen-vl-max 模型处理，而非默认的 qwen-plus

### Requirement: 图片上传格式支持
系统 SHALL 支持 JPEG、PNG、WebP 格式的图片上传，单张图片大小限制 5MB。

#### Scenario: 上传合法格式图片
- **WHEN** 用户上传一张 2MB 的 PNG 图片
- **THEN** 系统正常处理并返回 AI 回答

#### Scenario: 上传超大图片
- **WHEN** 用户上传一张 10MB 的图片
- **THEN** 返回 400 状态码和文件大小超限的错误信息

### Requirement: 多模态流式响应
系统 SHALL 支持多模态问答的 SSE 流式输出，与文本对话保持一致的响应形式。

#### Scenario: 图文问答流式返回
- **WHEN** 用户提交图文问题
- **THEN** AI 回答通过 SSE 流式逐字返回，前端可实时展示
