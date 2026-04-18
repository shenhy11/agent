package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.service.agent.RouterAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 多 Agent 协作控制器
 * 提供经过 Router 分发的多 Agent 对话接口
 */
@RestController
@RequestMapping("/api/v1/agent")
public class MultiAgentController {
    public MultiAgentController(RouterAgent routerAgent) {
        this.routerAgent = routerAgent;
    }

    private static final Logger log = LoggerFactory.getLogger(MultiAgentController.class);


    private final RouterAgent routerAgent;

    /**
     * 多 Agent 路由对话端点
     * GET /api/agent/multi?message=xxx
     */
    @GetMapping(value = "/multi", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMultiAgent(@RequestParam String message) {
        log.info("触发多 Agent 路由，问题: {}", message);
        ChatClient routedClient = routerAgent.routeChatClient(message);
        return routedClient.prompt().user(message).stream().content();
    }
}
