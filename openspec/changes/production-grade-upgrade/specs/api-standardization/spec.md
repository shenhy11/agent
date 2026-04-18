## ADDED Requirements

### Requirement: API 路径版本化
系统 SHALL 将所有端点统一到 `/api/v1/` 前缀下。

#### Scenario: 旧路径不可访问
- **WHEN** 客户端请求 `/api/nlp/summarize`（无版本号）
- **THEN** 系统返回 404

#### Scenario: 新路径可访问
- **WHEN** 客户端请求 `/api/v1/nlp/summarize` 并携带有效 Token
- **THEN** 系统正常返回业务结果

### Requirement: 全局异常处理
系统 SHALL 通过 `@RestControllerAdvice` 统一捕获所有异常并返回标准错误格式。

#### Scenario: 业务异常
- **WHEN** 服务层抛出 BusinessException
- **THEN** 系统返回 `{ "code": "NLP_001", "message": "...", "data": null }` 格式

#### Scenario: 未知异常
- **WHEN** 发生未捕获的 RuntimeException
- **THEN** 系统返回 500，错误码 `SYS_500`，不暴露异常堆栈

### Requirement: 统一响应格式
系统 SHALL 所有接口返回统一的 `ApiResult<T>` 包装。

#### Scenario: 成功响应
- **WHEN** 接口处理成功
- **THEN** 返回 `{ "code": "SUCCESS", "message": "操作成功", "data": <T> }`

### Requirement: OpenAPI 文档
系统 SHALL 通过 SpringDoc 自动生成 OpenAPI 3.0 文档，访问 `/swagger-ui.html` 可查看。

#### Scenario: Swagger UI 可访问
- **WHEN** 访问 `/swagger-ui.html`
- **THEN** 显示所有 `/api/v1/*` 端点的文档，包含请求/响应 Schema

### Requirement: 错误码枚举
系统 SHALL 定义统一的错误码枚举，按模块前缀分类（AUTH/USER/NLP/VISION/RAG/SYS）。

#### Scenario: 错误码唯一性
- **WHEN** 两个不同的业务异常被抛出
- **THEN** 它们的错误码 SHALL 不重复
