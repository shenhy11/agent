package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.service.agent.ReActEngine;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * ReAct 引擎 API
 * 为前端提供纯 ReAct 过程的可视化流
 */
@RestController
@RequestMapping("/api/v1/agent")
public class ReActController {
    public ReActController(ReActEngine reActEngine) {
        this.reActEngine = reActEngine;
    }

    private static final Logger log = LoggerFactory.getLogger(ReActController.class);


    private final ReActEngine reActEngine;

    /**
     * ReAct 对话端点
     * GET /api/agent/react?message=xxx
     */
    @GetMapping(value = "/react", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamReAct(@RequestParam String message) {
        log.info("触发 ReAct 推理循环，问题: {}", message);
        return reActEngine.runReActStream(message);
    }
}
