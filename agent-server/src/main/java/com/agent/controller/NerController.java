package com.agent.controller;

import com.agent.model.dto.ApiResult;
import com.agent.service.nlp.step.NerStep;
import com.agent.service.nlp.NlpStep;
import org.springframework.web.bind.annotation.*;

/**
 * NER 命名实体识别独立端点
 */
@RestController
@RequestMapping("/api/nlp")
public class NerController {

    private final NerStep nerStep;

    public NerController(NerStep nerStep) {
        this.nerStep = nerStep;
    }

    /**
     * POST /api/nlp/ner
     */
    @PostMapping("/ner")
    public ApiResult<Object> ner(@RequestBody NerRequest request) {
        NlpStep.NlpStepResult result = nerStep.execute(request.text(), new java.util.HashMap<>());
        if (result.isSuccess()) {
            return ApiResult.success(result.getStructured());
        }
        return ApiResult.error("NER 识别失败: " + result.getErrorMsg());
    }

    public record NerRequest(String text) {}
}
