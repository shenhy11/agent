package com.agent.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT Token 工具类：生成、解析、验证 Access/Refresh Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /** Token 中存储的用户名字段 */
    private static final String CLAIM_USERNAME = "sub";

    /** Token 中存储的角色字段 */
    private static final String CLAIM_ROLE = "role";

    /** Token 类型字段 */
    private static final String CLAIM_TYPE = "type";

    /**
     * 生成 Access Token（短期，2 小时）
     */
    public String generateAccessToken(String username, String role) {
        return buildToken(username, role, "access", jwtProperties.getAccessTokenExpiration());
    }

    /**
     * 生成 Refresh Token（长期，7 天）
     */
    public String generateRefreshToken(String username) {
        return buildToken(username, null, "refresh", jwtProperties.getRefreshTokenExpiration());
    }

    /**
     * 构建 JWT Token
     */
    private String buildToken(String username, String role, String type, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new java.util.HashMap<>();
        claims.put(CLAIM_TYPE, type);
        if (role != null) {
            claims.put(CLAIM_ROLE, role);
        }

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中解析用户名
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * 从 Token 中解析角色
     */
    public String extractRole(String token) {
        return extractClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * 校验 Token 是否有效（签名合法 + 未过期）
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 校验是否为 Access Token 类型
     */
    public boolean isAccessToken(String token) {
        try {
            String type = extractClaims(token).get(CLAIM_TYPE, String.class);
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 提取 Claims
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
