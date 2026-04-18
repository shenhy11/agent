package com.agent.controller;

import com.agent.model.dto.ApiResult;
import com.agent.service.nlp.LongDocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 长文档处理控制器
 */
@RestController
@RequestMapping("/api/v1/nlp")
public class LongDocumentController {

    private final LongDocumentService longDocumentService;

    public LongDocumentController(LongDocumentService longDocumentService) {
        this.longDocumentService = longDocumentService;
    }

    /**
     * 长文档摘要/问答接口（SSE 流式，实时推送分段进度）
     * POST /api/nlp/long-doc
     */
    @PostMapping(value = "/long-doc", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> processLongDocument(@RequestBody LongDocRequest request) {
        if (request.text() == null || request.text().trim().isEmpty()) {
            return Flux.just(ServerSentEvent.builder().data("{\"error\": \"文本不能为空\"}").build());
        }
        if (request.text().length() > 50000) {
            return Flux.just(ServerSentEvent.builder().data("{\"error\": \"长文档超过 50000 字上限，当前字数：" + request.text().length() + "\"}").build());
        }
        String task = request.task() != null ? request.task() : "summarize";
        return longDocumentService.processWithProgress(request.text(), task, request.query());
    }

    public record LongDocRequest(String text, String task, String query) {}
}
