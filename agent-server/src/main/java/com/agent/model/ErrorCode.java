package com.agent.model;

import lombok.Getter;

/**
 * 统一错误码枚举，格式：模块缩写_三位数字
 */
@Getter
public enum ErrorCode {

    // ===== 成功 =====
    SUCCESS("SUCCESS", "操作成功"),

    // ===== 认证模块 AUTH =====
    AUTH_001("AUTH_001", "用户名或密码错误"),
    AUTH_002("AUTH_002", "Token 已过期，请重新登录"),
    AUTH_003("AUTH_003", "Token 无效或格式错误"),
    AUTH_004("AUTH_004", "无访问权限"),

    // ===== 用户模块 USER =====
    USER_001("USER_001", "用户名已存在"),
    USER_002("USER_002", "用户不存在"),
    USER_003("USER_003", "账号已被禁用"),

    // ===== NLP 模块 =====
    NLP_001("NLP_001", "文本内容不能为空"),
    NLP_002("NLP_002", "文本长度超出限制（最大 50000 字符）"),
    NLP_003("NLP_003", "Pipeline 步骤数超出限制（最大 5 步）"),

    // ===== 视觉模块 VISION =====
    VISION_001("VISION_001", "图片数量超出限制（最大 3 张）"),
    VISION_002("VISION_002", "图片格式不支持"),
    VISION_003("VISION_003", "视觉模型调用超时"),

    // ===== RAG 模块 =====
    RAG_001("RAG_001", "知识库文档上传失败"),
    RAG_002("RAG_002", "知识库查询失败"),

    // ===== 系统级 SYS =====
    SYS_400("SYS_400", "请求参数错误"),
    SYS_500("SYS_500", "服务器内部错误"),
    SYS_503("SYS_503", "服务暂时不可用，请稍后重试");

    /** 错误码 */
    private final String code;

    /** 错误描述 */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
