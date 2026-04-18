-- ============================================================
-- V1: 初始化核心业务表结构
-- 包含：sys_user / chat_session / chat_message / audit_log
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,       -- BCrypt 加密
    nickname    VARCHAR(100),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',   -- USER / ADMIN
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);

-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES sys_user(id) ON DELETE SET NULL,
    title       VARCHAR(200),
    module      VARCHAR(50),                -- chat / nlp / vision / rag / agent
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_session_user_id ON chat_session(user_id);

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT REFERENCES chat_session(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,       -- user / assistant / system
    content     TEXT        NOT NULL,
    token_count INT,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_message_session_id ON chat_message(session_id);

-- 操作审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(50),
    action      VARCHAR(100) NOT NULL,      -- 操作动作，如 NLP_SUMMARIZE
    module      VARCHAR(50),               -- 模块，如 nlp / vision / rag
    detail      TEXT,
    ip          VARCHAR(45),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user_id   ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
