package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 微调对比控制器
 * 提供同时访问基础模型和微调模型流的端点
 */
@RestController
@RequestMapping("/api/v1/finetune")
public class FinetuneCompareController {
    private static final Logger log = LoggerFactory.getLogger(FinetuneCompareController.class);


    private final ChatClient baseChatClient;
    private final ChatClient finetunedChatClient;

    public FinetuneCompareController(
            @Qualifier("customerServiceChatClient") ChatClient baseChatClient,
            @Qualifier("finetunedChatClient") ChatClient finetunedChatClient) {
        this.baseChatClient = baseChatClient;
        this.finetunedChatClient = finetunedChatClient;
    }

    /**
     * 访问基础模型
     */
    @GetMapping(value = "/base/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamBase(@RequestParam String message) {
        return baseChatClient.prompt().user(message).stream().content();
    }

    /**
     * 访问微调模型
     */
    @GetMapping(value = "/lora/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamLoRA(@RequestParam String message) {
        return finetunedChatClient.prompt().user(message).stream().content();
    }
}
