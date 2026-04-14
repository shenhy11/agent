## ADDED Requirements

### Requirement: LLM 视觉 OCR
系统 SHALL 使用 qwen-vl-max 的视觉理解能力从图片中提取文字内容。

#### Scenario: 文字提取
- **WHEN** 用户上传包含文字的图片（手表表盘/说明书/包装盒）
- **THEN** 系统返回提取的文字列表和结构化数据

#### Scenario: 表格数据提取
- **WHEN** 图片包含参数表格
- **THEN** 系统以结构化 JSON 返回表格内容

### Requirement: OCR + LLM 分析串联
系统 SHALL 在 OCR 提取文字后，自动用 LLM 进行语义分析。

#### Scenario: 手表表盘识别
- **WHEN** 用户上传手表表盘图片
- **THEN** 系统先提取表盘显示内容，再分析显示的信息含义（时间/心率/步数等）

#### Scenario: 说明书理解
- **WHEN** 用户上传产品说明书图片并提问
- **THEN** 系统先提取文字，再基于文字内容回答问题
