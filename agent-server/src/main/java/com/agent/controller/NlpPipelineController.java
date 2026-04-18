package com.agent.controller;

import com.agent.model.dto.ApiResult;
import com.agent.service.nlp.NlpPipeline;
import com.agent.service.nlp.NlpStep;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * NLP Pipeline 控制器
 * 提供多步 NLP 任务编排接口
 */
@RestController
@RequestMapping("/api/v1/nlp")
public class NlpPipelineController {

    private final NlpPipeline nlpPipeline;

    public NlpPipelineController(NlpPipeline nlpPipeline) {
        this.nlpPipeline = nlpPipeline;
    }

    /**
     * 获取所有可用的 NLP 步骤（供前端展示）
     * GET /api/nlp/steps
     */
    @GetMapping("/steps")
    public ApiResult<List<Map<String, String>>> getAvailableSteps() {
        return ApiResult.success(nlpPipeline.getAvailableSteps());
    }

    /**
     * 执行 NLP Pipeline（同步）
     * POST /api/nlp/pipeline
     */
    @PostMapping("/pipeline")
    public ApiResult<NlpPipeline.PipelineResult> runPipeline(@RequestBody PipelineRequest request) {
        if (request.stepIds() == null || request.stepIds().isEmpty()) {
            return ApiResult.error("步骤列表不能为空");
        }
        if (request.stepIds().size() > 5) {
            return ApiResult.error("最多只允许串联执行 5 个步骤，以防止 token 消耗过大");
        }
        NlpPipeline.PipelineResult result = nlpPipeline.execute(request.text(), request.stepIds());
        return ApiResult.success(result);
    }

    /**
     * 执行 NLP Pipeline（SSE 流式，每步完成推送一次）
     * POST /api/nlp/pipeline/stream
     */
    @PostMapping(value = "/pipeline/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> runPipelineStream(@RequestBody PipelineRequest request) {
        if (request.stepIds() == null || request.stepIds().isEmpty()) {
            return Flux.just(ServerSentEvent.builder()
                    .event("error")
                    .data((Object) "步骤列表不能为空")
                    .build());
        }
        if (request.stepIds().size() > 5) {
            return Flux.just(ServerSentEvent.builder()
                    .event("error")
                    .data((Object) "{\"error\":\"最多只允许串联执行 5 个步骤，以防止 token 消耗过大\"}")
                    .build());
        }
        return Flux.fromIterable(request.stepIds())
                .delayElements(Duration.ofMillis(50))
                .flatMapSequential(stepId -> {
                    NlpPipeline.PipelineResult partial = nlpPipeline.execute(request.text(), List.of(stepId));
                    NlpStep.NlpStepResult stepResult = partial.getStepResults().get(0);
                    return Flux.just(ServerSentEvent.builder()
                            .event("nlp_step")
                            .data((Object) stepResult)
                            .build());
                })
                .concatWith(Flux.just(ServerSentEvent.builder()
                        .event("done")
                        .data((Object) "pipeline_complete")
                        .build()));
    }

    /**
     * Pipeline 请求体
     */
    public record PipelineRequest(String text, List<String> stepIds) {}
}
