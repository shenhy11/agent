package com.agent.service.nlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NLP Pipeline 编排引擎
 * 按顺序串联执行多个 NlpStep，每步输出作为下一步的上下文增强
 */
@Service
public class NlpPipeline {
    private static final Logger log = LoggerFactory.getLogger(NlpPipeline.class);

    /** 最大允许步骤数（安全防护） */
    private static final int MAX_STEPS = 5;

    private final Map<String, NlpStep> stepRegistry;

    public NlpPipeline(List<NlpStep> steps) {
        this.stepRegistry = new HashMap<>();
        for (NlpStep step : steps) {
            stepRegistry.put(step.getId(), step);
        }
        log.info("NLP Pipeline 已注册 {} 个步骤: {}", steps.size(), stepRegistry.keySet());
    }

    /**
     * Pipeline 执行结果
     */
    public static class PipelineResult {
        private final List<NlpStep.NlpStepResult> stepResults;
        private final boolean success;
        private final String finalOutput;

        public PipelineResult(List<NlpStep.NlpStepResult> stepResults,
                              boolean success, String finalOutput) {
            this.stepResults = stepResults;
            this.success = success;
            this.finalOutput = finalOutput;
        }

        public List<NlpStep.NlpStepResult> getStepResults() { return stepResults; }
        public boolean isSuccess() { return success; }
        public String getFinalOutput() { return finalOutput; }
    }

    /**
     * 按指定步骤 ID 列表串联执行 Pipeline
     *
     * @param input   原始文本
     * @param stepIds 步骤 ID 列表（有序）
     * @return Pipeline 执行结果
     */
    public PipelineResult execute(String input, List<String> stepIds) {
        // 安全上限：超过 MAX_STEPS 步拒绝执行
        if (stepIds.size() > MAX_STEPS) {
            throw new IllegalArgumentException(
                    "Pipeline 步骤数超过上限（最多 " + MAX_STEPS + " 步），当前请求 " + stepIds.size() + " 步");
        }

        List<NlpStep.NlpStepResult> results = new ArrayList<>();
        Map<String, Object> context = new HashMap<>();
        String currentInput = input;

        for (String stepId : stepIds) {
            NlpStep step = stepRegistry.get(stepId);
            if (step == null) {
                log.warn("未找到 NLP 步骤: {}", stepId);
                results.add(NlpStep.NlpStepResult.failure(stepId, stepId,
                        "未找到步骤: " + stepId));
                return new PipelineResult(results, false, currentInput);
            }

            log.info("执行 NLP 步骤: {} (输入 {} 字)", stepId, currentInput.length());
            try {
                NlpStep.NlpStepResult result = step.execute(currentInput, context);
                results.add(result);

                if (!result.isSuccess()) {
                    log.warn("步骤 {} 执行失败: {}", stepId, result.getErrorMsg());
                    return new PipelineResult(results, false, currentInput);
                }

                // 将步骤输出传递给下一步，并存入上下文
                if (result.getOutput() != null) {
                    currentInput = result.getOutput();
                }
                context.put(stepId + "_result", result.getStructured());
                context.put(stepId + "_output", result.getOutput());

            } catch (Exception e) {
                log.error("步骤 {} 执行异常", stepId, e);
                results.add(NlpStep.NlpStepResult.failure(stepId, step.getName(), e.getMessage()));
                return new PipelineResult(results, false, currentInput);
            }
        }

        return new PipelineResult(results, true, currentInput);
    }

    /**
     * 获取所有已注册的步骤信息（供前端展示可用步骤列表）
     */
    public List<Map<String, String>> getAvailableSteps() {
        List<Map<String, String>> list = new ArrayList<>();
        for (NlpStep step : stepRegistry.values()) {
            Map<String, String> info = new HashMap<>();
            info.put("id", step.getId());
            info.put("name", step.getName());
            list.add(info);
        }
        return list;
    }
}
