package com.agent.service.nlp;

import java.util.Map;

/**
 * NLP 流水线步骤接口
 * 每个步骤接收输入文本和上下文，返回步骤执行结果
 */
public interface NlpStep {

    /**
     * 步骤标识符，用于前端展示和路由
     */
    String getId();

    /**
     * 步骤人类可读名称
     */
    String getName();

    /**
     * 执行步骤
     *
     * @param input   当前输入文本
     * @param context 共享上下文（前序步骤输出可在此累积）
     * @return 步骤结果
     */
    NlpStepResult execute(String input, Map<String, Object> context);

    /**
     * NLP 步骤执行结果
     */
    class NlpStepResult {
        private final String stepId;
        private final String stepName;
        private final String output;        // 步骤文本输出（可传递给下一步）
        private final Object structured;   // 结构化数据（如实体列表等）
        private final boolean success;
        private final String errorMsg;

        private NlpStepResult(String stepId, String stepName, String output,
                              Object structured, boolean success, String errorMsg) {
            this.stepId = stepId;
            this.stepName = stepName;
            this.output = output;
            this.structured = structured;
            this.success = success;
            this.errorMsg = errorMsg;
        }

        public static NlpStepResult success(String stepId, String stepName,
                                            String output, Object structured) {
            return new NlpStepResult(stepId, stepName, output, structured, true, null);
        }

        public static NlpStepResult failure(String stepId, String stepName, String errorMsg) {
            return new NlpStepResult(stepId, stepName, null, null, false, errorMsg);
        }

        public String getStepId() { return stepId; }
        public String getStepName() { return stepName; }
        public String getOutput() { return output; }
        public Object getStructured() { return structured; }
        public boolean isSuccess() { return success; }
        public String getErrorMsg() { return errorMsg; }
    }
}
