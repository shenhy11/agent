## ADDED Requirements

### Requirement: 命名实体提取
系统 SHALL 从文本中提取结构化命名实体，返回实体文本、类型、位置信息。

#### Scenario: 产品实体提取
- **WHEN** 输入"我想了解 Sport X2 Pro，价格 ¥1999 对吗？"
- **THEN** 返回实体列表：[{text: "Sport X2 Pro", type: "PRODUCT"}, {text: "¥1999", type: "PRICE"}]

#### Scenario: 多类型实体
- **WHEN** 输入包含产品、品牌、价格、规格等多类型实体
- **THEN** 系统支持识别：PRODUCT、BRAND、PRICE、SPEC、TIME、PERSON 六种类型

### Requirement: NER 前端可视化
系统 SHALL 返回实体的位置信息（start/end），前端可实现文本中的实体高亮标注。

#### Scenario: 位置标注
- **WHEN** NER 返回结果
- **THEN** 每个实体包含 start（起始字符位置）和 end（结束字符位置）字段
