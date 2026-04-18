## ADDED Requirements

### Requirement: JWT 认证登录
系统 SHALL 提供基于用户名/密码的 JWT 登录接口，成功后返回 Access Token 和 Refresh Token。

#### Scenario: 用户登录成功
- **WHEN** 用户以正确的 username/password 调用 `POST /api/v1/auth/login`
- **THEN** 系统返回 200，body 包含 `accessToken`（有效期 2 小时）和 `refreshToken`（有效期 7 天）

#### Scenario: 用户登录失败
- **WHEN** 用户以错误的密码调用 `POST /api/v1/auth/login`
- **THEN** 系统返回 401，body 包含错误码 `AUTH_001` 和消息 "用户名或密码错误"

### Requirement: Token 刷新
系统 SHALL 支持通过 Refresh Token 获取新的 Access Token。

#### Scenario: 刷新成功
- **WHEN** 用户以有效的 refreshToken 调用 `POST /api/v1/auth/refresh`
- **THEN** 系统返回新的 accessToken，refreshToken 不变

#### Scenario: Refresh Token 过期
- **WHEN** 用户以过期的 refreshToken 调用 `POST /api/v1/auth/refresh`
- **THEN** 系统返回 401，错误码 `AUTH_002`，用户需重新登录

### Requirement: 接口鉴权保护
系统 SHALL 对所有 `/api/v1/**` 端点（除 `/api/v1/auth/**`）强制要求 Bearer Token 认证。

#### Scenario: 无 Token 访问受保护接口
- **WHEN** 请求未携带 `Authorization` 头访问 `/api/v1/nlp/summarize`
- **THEN** 系统返回 401

#### Scenario: 携带有效 Token 访问
- **WHEN** 请求携带有效 Bearer Token 访问 `/api/v1/nlp/summarize`
- **THEN** 系统正常返回业务结果

### Requirement: 用户注册
系统 SHALL 提供管理员创建用户的接口，密码使用 BCrypt 加密存储。

#### Scenario: 管理员创建用户
- **WHEN** ADMIN 角色用户调用 `POST /api/v1/admin/users` 传入 username/password/role
- **THEN** 系统创建用户，密码 BCrypt 加密后存储，返回用户 ID

#### Scenario: 用户名重复
- **WHEN** 创建已存在的 username
- **THEN** 系统返回 409，错误码 `USER_001`

### Requirement: RBAC 角色控制
系统 SHALL 支持 USER 和 ADMIN 两种角色，ADMIN 可访问管理端点。

#### Scenario: 普通用户访问管理端点
- **WHEN** USER 角色访问 `GET /api/v1/admin/users`
- **THEN** 系统返回 403

#### Scenario: 管理员访问管理端点
- **WHEN** ADMIN 角色访问 `GET /api/v1/admin/users`
- **THEN** 系统正常返回用户列表
