package com.agent.controller;

import com.agent.model.dto.ApiResult;
import com.agent.service.nlp.RuleBasedIntentService;
import com.agent.service.nlp.step.IntentStep;
import com.agent.service.nlp.NlpStep;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 意图识别控制器
 * 支持 LLM/规则引擎/双模式对比
 */
@RestController
@RequestMapping("/api/nlp")
public class IntentController {

    private final IntentStep intentStep;
    private final RuleBasedIntentService ruleBasedIntentService;

    public IntentController(IntentStep intentStep, RuleBasedIntentService ruleBasedIntentService) {
        this.intentStep = intentStep;
        this.ruleBasedIntentService = ruleBasedIntentService;
    }

    /**
     * POST /api/nlp/intent?mode=llm|rule|both
     */
    @PostMapping("/intent")
    public ApiResult<Object> classifyIntent(@RequestBody IntentRequest request,
                                            @RequestParam(defaultValue = "llm") String mode) {
        return switch (mode) {
            case "rule" -> ApiResult.success(ruleBasedIntentService.classify(request.text()));
            case "both" -> {
                // 双模式对比
                Map<String, Object> llmResult = new HashMap<>();
                NlpStep.NlpStepResult llmStepResult = intentStep.execute(request.text(), Map.of());
                llmResult.put("llm", llmStepResult.isSuccess() ? llmStepResult.getStructured() : llmStepResult.getErrorMsg());
                llmResult.put("rule", ruleBasedIntentService.classify(request.text()));
                yield ApiResult.success(llmResult);
            }
            default -> {
                // LLM 模式
                NlpStep.NlpStepResult result = intentStep.execute(request.text(), Map.of());
                yield result.isSuccess()
                        ? ApiResult.success(result.getStructured())
                        : ApiResult.error("意图识别失败: " + result.getErrorMsg());
            }
        };
    }

    public record IntentRequest(String text) {}
}
