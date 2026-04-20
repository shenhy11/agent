package com.agent.config;

import com.agent.model.ApiResult;
import com.agent.model.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 配置：JWT 无状态、CORS、放行认证端点
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（JWT 无状态不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // CORS 配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 无状态 Session（JWT 模式）
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 路径权限
            .authorizeHttpRequests(auth -> auth
                // 认证端点放行
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Actuator 健康检查放行
                .requestMatchers("/actuator/health").permitAll()
                // Swagger UI 放行（仅开发环境建议限制）
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // 静态资源放行（前端 SPA）
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.ico", "/*.js", "/*.css").permitAll()
                // === AI Lab Demo 展示接口全部放行（无需登录） ===
                .requestMatchers("/api/v1/chat/**").permitAll()
                .requestMatchers("/api/v1/nlp/**").permitAll()
                .requestMatchers("/api/v1/multimodal/**").permitAll()
                .requestMatchers("/api/v1/knowledge/**").permitAll()
                .requestMatchers("/api/v1/agent/**").permitAll()
                .requestMatchers("/api/v1/products/**").permitAll()
                .requestMatchers("/api/v1/contact/**").permitAll()
                // ADMIN 端点需要 ADMIN 角色
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            // 未认证 401 处理（返回 JSON，不重定向）
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        objectMapper.writeValueAsString(ApiResult.error(ErrorCode.AUTH_003))
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        objectMapper.writeValueAsString(ApiResult.error(ErrorCode.AUTH_004))
                    );
                })
            )
            // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt 密码编码器 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS 配置：允许前端跨域调用 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
