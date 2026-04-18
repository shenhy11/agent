package com.agent.config;

import com.agent.tools.CustomerServiceTools;
import com.agent.tools.ProductTools;
import org.springframework.ai.chat.client.ChatClient;
import com.agent.service.rag.RagPipelineService;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import com.agent.service.agent.SummarizingChatMemory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 核心配置
 * 配置 ChatClient（含 RAG Advisor）、ChatMemory、VectorStore
 */
@Configuration
public class AiConfig {

    @Value("${agent.chat.system-prompt:你是一个专业的智能客服助手}")
    private String systemPrompt;

    @Value("${agent.chat.memory-max-messages:20}")
    private int memoryMaxMessages;

    @Value("${agent.rag.top-k:5}")
    private int ragTopK;

    @Value("${agent.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${agent.vision.model:qwen-vl-max}")
    private String visionModel;

    @Value("${agent.vision.temperature:0.3}")
    private double visionTemperature;

    @Value("${agent.vision.timeout-seconds:30}")
    private long visionTimeoutSeconds;

    /**
     * 对话记忆 Bean
     * 使用 SummarizingChatMemory 实现三级记忆架构 (长、中、短)
     */
    @Bean
    public ChatMemory chatMemory(ChatClient.Builder builder, RedisTemplate<String, Object> redisTemplate) {
        // 创建一个单独的 ChatClient 给 Memory 压缩使用
        ChatClient memoryClient = builder.defaultSystem("你是一个出色的对话总结专家。").build();
        return new SummarizingChatMemory(memoryClient, redisTemplate, memoryMaxMessages);
    }

    // VectorStore Bean 由 spring-ai-pgvector-store-spring-boot-starter 自动配置
    // 启动时 Spring AI 根据 spring.ai.vectorstore.pgvector.* 配置自动初始化 PgVectorStore

    /**
     * 默认 Primary ChatClient
     */
    @Bean
    @Primary
    public ChatClient primaryChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 售前客服 Assistant Bean
     * 专注产品推荐、参数对比
     */
    @Bean("customerServiceChatClient")
    public ChatClient customerServiceChatClient(ChatClient.Builder builder,
                                  ChatMemory chatMemory,
                                  RagPipelineService ragPipelineService,
                                  CustomerServiceTools customerServiceTools,
                                  ProductTools productTools) {
        return builder
                .defaultSystem(systemPrompt + " 你的职责是【售前导购与产品咨询】。不要回答退换货和维修问题。")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(productTools)
                .build();
    }

    /**
     * 售后支持 Assistant Bean
     * 专注退换货、物流、维修
     */
    @Bean("afterSalesChatClient")
    public ChatClient afterSalesChatClient(ChatClient.Builder builder,
                                                ChatMemory chatMemory,
                                                RagPipelineService ragPipelineService,
                                                CustomerServiceTools customerServiceTools) {
        return builder
                .defaultSystem(systemPrompt + " 你的职责是【售后服务与物流追踪】。专注解决退换货、维修和物流问题。")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(customerServiceTools)
                .build();
    }

    /**
     * 微调模型专属 Assistant Bean (供对比页面使用)
     * 临时指向基础模型，未来可替换 baseUrl 指向 LoRA API
     */
    @Bean("finetunedChatClient")
    public ChatClient finetunedChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(systemPrompt + " 这是一个经过 LoRA 微调的模型。")
                .build();
    }

    /**
     * 视觉模型专属 ChatClient Bean
     * 使用 qwen-vl-max 支持图文理解、OCR、以图搜图
     */
    @Bean("visionChatClient")
    public ChatClient visionChatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(
                        org.springframework.ai.chat.prompt.ChatOptions.builder()
                                .model(visionModel)
                                .temperature(visionTemperature)
                                .build()
                )
                .build();
    }
}
