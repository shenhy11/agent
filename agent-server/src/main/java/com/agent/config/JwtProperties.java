package com.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性，与 application.yml 中的 jwt.* 配置绑定
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** HMAC 签名密钥（生产环境通过环境变量 JWT_SECRET 注入） */
    private String secret = "changeme-replace-in-production-env-256bit";

    /** Access Token 有效期（毫秒），默认 2 小时 */
    private long accessTokenExpiration = 7200000;

    /** Refresh Token 有效期（毫秒），默认 7 天 */
    private long refreshTokenExpiration = 604800000;
}
