package com.agent.service.nlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 长文档 Map-Reduce 处理服务
 * 对超过 4000 字的文本自动分段→各段处理→LLM 合并
 */
@Service
public class LongDocumentService {
    private static final Logger log = LoggerFactory.getLogger(LongDocumentService.class);

    /** 触发 Map-Reduce 的字数阈值 */
    private static final int MAP_REDUCE_THRESHOLD = 4000;
    /** 每段最大字数 */
    private static final int SEGMENT_SIZE = 2000;
    /** 分段重叠字数 */
    private static final int OVERLAP = 200;
    /** 最大字数上限（安全防护） */
    private static final int MAX_TEXT_LENGTH = 50000;

    private final ChatClient chatClient;

    public LongDocumentService(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 长文档处理（SSE 流式，每段完成推送进度）
     *
     * @param text  原始长文本
     * @param task  任务类型：summarize / qa
     * @param query 当 task=qa 时必填的问题
     */
    public Flux<ServerSentEvent<Object>> processWithProgress(String text, String task, String query) {
        // 安全上限校验
        if (text.length() > MAX_TEXT_LENGTH) {
            return Flux.just(ServerSentEvent.builder()
                    .event("error")
                    .data((Object) "文本超过最大长度限制（50000 字），请缩短后重试")
                    .build());
        }

        // 短文本直接处理无需分段
        if (text.length() <= MAP_REDUCE_THRESHOLD) {
            String result = directProcess(text, task, query);
            return Flux.just(
                    ServerSentEvent.builder().event("progress").data((Object) "1/1").build(),
                    ServerSentEvent.builder().event("result").data((Object) result).build(),
                    ServerSentEvent.builder().event("done").data((Object) "complete").build()
            );
        }

        // 分段
        List<String> segments = segment(text);
        int total = segments.size();
        AtomicInteger counter = new AtomicInteger(0);

        return Flux.fromIterable(segments)
                .flatMapSequential(segment -> {
                    int idx = counter.incrementAndGet();
                    String segResult = directProcess(segment, task, query);
                    return Flux.just(
                            ServerSentEvent.builder()
                                    .event("segment")
                                    .data((Object) java.util.Map.of(
                                            "index", idx,
                                            "total", total,
                                            "result", segResult
                                    ))
                                    .build()
                    );
                })
                .collectList()
                .flatMapMany(eventsCollect -> {
                    // Reduce：合并各段摘要
                    List<String> segResults = new ArrayList<>();
                    for (ServerSentEvent<Object> e : eventsCollect) {
                        if (e.data() instanceof java.util.Map<?,?> m) {
                            segResults.add((String) m.get("result"));
                        }
                    }

                    String mergePrompt = "以下是对一篇长文档各段的" +
                            ("summarize".equals(task) ? "摘要" : "分析") + "：\n\n" +
                            String.join("\n---\n", segResults) +
                            "\n\n请将上述各段内容综合整理，生成最终的完整" +
                            ("summarize".equals(task) ? "摘要" : "回答") + "：";
                    String finalResult = chatClient.prompt().user(mergePrompt).call().content();

                    return Flux.concat(
                            Flux.fromIterable(eventsCollect),
                            Flux.just(
                                    ServerSentEvent.builder().event("reduce").data((Object) finalResult).build(),
                                    ServerSentEvent.builder().event("done").data((Object) "complete").build()
                            )
                    );
                });
    }

    /**
     * 将文本按 SEGMENT_SIZE 分段（带 OVERLAP 重叠）
     */
    private List<String> segment(String text) {
        List<String> segments = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + SEGMENT_SIZE, text.length());
            segments.add(text.substring(start, end));
            start += SEGMENT_SIZE - OVERLAP;
            if (start >= text.length()) break;
        }
        log.info("长文档分段完成：共 {} 段", segments.size());
        return segments;
    }

    /**
     * 处理单段文本
     */
    private String directProcess(String text, String task, String query) {
        String prompt = "summarize".equals(task)
                ? "请对以下文本做简洁摘要：\n" + text
                : "请根据以下文本回答问题「" + query + "」：\n" + text;
        return chatClient.prompt().user(prompt).call().content();
    }
}
