package com.agent.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体，对应 audit_log 表
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_log")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 操作用户 ID */
    @Column(name = "user_id")
    private Long userId;

    /** 操作用户名（冗余，方便查询） */
    @Column(length = 50)
    private String username;

    /** 操作动作，如 NLP_SUMMARIZE */
    @Column(length = 100, nullable = false)
    private String action;

    /** 模块：nlp / vision / rag / agent */
    @Column(length = 50)
    private String module;

    /** 操作详情（JSON 或文本描述） */
    @Column(columnDefinition = "TEXT")
    private String detail;

    /** 客户端 IP */
    @Column(length = 45)
    private String ip;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
