## ADDED Requirements

### Requirement: 产品列表查询接口
系统 SHALL 提供 `GET /api/products` 接口，返回全部产品列表（含系列分类信息）。

#### Scenario: 获取全部产品
- **WHEN** 前端请求 `GET /api/products`
- **THEN** 返回 6 款产品的 JSON 数组，每项包含 id、name、series、subtitle、price、specs、imageUrl

#### Scenario: 按系列筛选产品
- **WHEN** 前端请求 `GET /api/products?series=sport`
- **THEN** 仅返回 series 为 "sport" 的产品

### Requirement: 产品详情查询接口
系统 SHALL 提供 `GET /api/products/{id}` 接口，返回单个产品的完整规格信息。

#### Scenario: 查询存在的产品
- **WHEN** 前端请求 `GET /api/products/sport-x2-pro`
- **THEN** 返回该产品的完整规格（电池、防水、材质、传感器、NFC、重量等）

#### Scenario: 查询不存在的产品
- **WHEN** 前端请求 `GET /api/products/nonexistent`
- **THEN** 返回 404 状态码和错误信息

### Requirement: 产品对比接口
系统 SHALL 提供 `GET /api/products/compare?ids=id1,id2` 接口，返回多款产品的并排对比数据。

#### Scenario: 对比两款产品
- **WHEN** 前端请求 `GET /api/products/compare?ids=sport-x2-pro,elite-e2-ultra`
- **THEN** 返回两款产品的对比结构体，包含各维度规格

### Requirement: 联系表单提交接口
系统 SHALL 提供 `POST /api/contact` 接口，接收并处理用户提交的联系表单。

#### Scenario: 提交有效表单
- **WHEN** 前端提交包含 name、email、subject、message 的 JSON 请求
- **THEN** 返回 200 状态码和成功消息

#### Scenario: 提交缺少必填字段的表单
- **WHEN** 前端提交缺少 email 字段的 JSON 请求
- **THEN** 返回 400 状态码和校验错误信息

### Requirement: 产品数据硬编码存储
系统 SHALL 在后端以硬编码方式存储 6 款手表产品数据（Sport X2 Pro、Sport X1 Lite、Elite E2 Ultra、Elite E1 Classic、Rugged R2 Titan、Rugged R1 Explorer），无需数据库。

#### Scenario: 服务启动后产品数据可用
- **WHEN** Spring Boot 应用启动完成
- **THEN** 通过 `GET /api/products` 可获取全部 6 款产品数据
