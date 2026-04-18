package com.agent.config;

import com.agent.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;

/**
 * Web MVC 配置：注册限流拦截器 + Swagger OpenAPI 文档
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流拦截器作用于所有 /api/v1/ 接口，放行鉴权路径
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/actuator/**");
    }

    /**
     * Swagger OpenAPI 3.0 文档配置
     * 访问地址：http://localhost:8080/swagger-ui/index.html
     */
    @Bean
    public OpenAPI chronoTechOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChronoTech AI Lab API")
                        .description("企业级 AI 能力平台 — 后端 REST API 文档")
                        .version("v2.0.0")
                        .contact(new Contact().name("ChronoTech Team").email("dev@chronotech.ai")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
