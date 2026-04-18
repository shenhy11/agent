## ADDED Requirements

### Requirement: 结构化日志
系统 SHALL 输出 JSON 格式的结构化日志（Logback JSON Encoder），便于日志采集。

#### Scenario: 日志格式
- **WHEN** 应用产生任何日志
- **THEN** 日志输出为单行 JSON，包含 timestamp/level/logger/message/traceId 字段

### Requirement: Micrometer 指标暴露
系统 SHALL 通过 Spring Boot Actuator + Micrometer 暴露 Prometheus 格式指标。

#### Scenario: 指标端点可访问
- **WHEN** 访问 `/actuator/prometheus`
- **THEN** 返回 JVM、HTTP 请求、自定义业务指标（如 AI 调用次数）

### Requirement: API 限流防刷
系统 SHALL 对 AI 类接口实施速率限制，防止恶意调用导致 Token 消耗过大。

#### Scenario: 超出限流
- **WHEN** 同一用户在 1 分钟内调用 NLP 接口超过 30 次
- **THEN** 系统返回 429 Too Many Requests

### Requirement: Docker Compose 生产部署
系统 SHALL 提供 Docker Compose 文件，一键部署 Spring Boot + PostgreSQL + Nginx 三容器。

#### Scenario: 一键启动
- **WHEN** 在阿里云 ECS 上执行 `docker compose up -d`
- **THEN** 三个容器正常启动，Nginx 在 80 端口提供服务

#### Scenario: HTTPS 支持
- **WHEN** 配置 SSL 证书后重启 Nginx
- **THEN** 443 端口提供 HTTPS 服务，80 端口自动重定向至 HTTPS

### Requirement: 健康检查
系统 SHALL 暴露健康检查端点，Docker 和阿里云负载均衡可用。

#### Scenario: 健康检查通过
- **WHEN** 访问 `/actuator/health`
- **THEN** 返回 `{"status": "UP"}`，包含 db 和 redis 的健康状态
