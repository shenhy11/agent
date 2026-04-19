package com.agent;

import com.agent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ChronoTech AI Lab 应用启动类
 */
@SpringBootApplication(exclude = {
    org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration.class
})
@EnableJpaAuditing
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    /**
     * 应用启动后初始化默认管理员账号（若不存在）
     */
    @Bean
    public ApplicationRunner initData(UserService userService) {
        return args -> userService.initAdminIfAbsent();
    }
}
