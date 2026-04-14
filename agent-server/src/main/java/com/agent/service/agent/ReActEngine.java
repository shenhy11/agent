package com.agent.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct 推理引擎
 * 实现多步推理循环 (Reason-Act-Observe)，通过 SSE 实时向前端推送思考和动作
 */
@Service
public class ReActEngine {
    public ReActEngine(ChatClient chatClient, ToolExecutionService toolExecutionService) {
        this.chatClient = chatClient;
        this.toolExecutionService = toolExecutionService;
    }

    private static final Logger log = LoggerFactory.getLogger(ReActEngine.class);


    private final ChatClient chatClient;
    // 假设已有这个服务用于执行工具，这里用一个模拟调度器
    private final ToolExecutionService toolExecutionService;

    // 解析 ReAct 格式的正则
    private static final Pattern THINK_PATTERN = Pattern.compile("Thought:(.*?)(?:Action:|Final Answer:|$)", Pattern.DOTALL);
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*([a-zA-Z0-9_]+)\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("Final Answer:(.*)", Pattern.DOTALL);

    private static final String REACT_PROMPT_TEMPLATE = """
            尽可能回答以下问题。你可以使用以下工具：
            
            %s
            
            请严格按照以下格式进行思考和操作：
            
            Question: 需要你回答的输入问题
            Thought: 你应该总是思考下一步要做什么
            Action: 工具的名称，必须是 [%s] 之一，加上方括号括起来的参数。如 Action: tool_name [{"arg":"val"}]
            Observation: 动作的结果
            ... (Thought/Action/Observation 可以重复最多 10 次)
            Thought: 我现在知道最终答案了
            Final Answer: 提供给用户的最终回答
            
            开始！
            
            Question: %s
            """;
    public static class ThinkResult {
        private String thought;
        private String action;
        private String actionInput;
        private String finalAnswer;
        private String actionId;

        public String getThought() { return thought; }
        public void setThought(String thought) { this.thought = thought; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getActionInput() { return actionInput; }
        public void setActionInput(String actionInput) { this.actionInput = actionInput; }

        public String getFinalAnswer() { return finalAnswer; }
        public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }

        public String getActionId() { return actionId; }
        public void setActionId(String actionId) { this.actionId = actionId; }
    }
    public static class ToolCallEvent {
        private String tool;
        private String input;
        private String callId;

        public String getTool() { return tool; }
        public void setTool(String tool) { this.tool = tool; }

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }

        public String getCallId() { return callId; }
        public void setCallId(String callId) { this.callId = callId; }
    }
    public static class ToolResultEvent {
        private String result;
        private long durationMs;
        private String callId;

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public String getCallId() { return callId; }
        public void setCallId(String callId) { this.callId = callId; }
    }

    /**
     * 运行 ReAct 循环并作为 Flux 流返回 SSE 事件
     */
    public Flux<ServerSentEvent<String>> runReActStream(String question) {
        return Flux.create(sink -> {
            try {
                int step = 0;
                int maxSteps = 10;
                List<Message> history = new ArrayList<>();
                
                // 1. 获取可用工具说明 (这里简化，实际应从 registry 动态获取)
                String toolsDesc = toolExecutionService.getToolsDescription();
                String toolNames = toolExecutionService.getToolNames();

                // 2. 初始化第一条提示词
                String initialPrompt = String.format(REACT_PROMPT_TEMPLATE, toolsDesc, toolNames, question);
                history.add(new UserMessage(initialPrompt));

                while (step < maxSteps) {
                    step++;
                    log.info("ReAct Step {} 开始", step);

                    // 调用 LLM
                    String response = chatClient.prompt(new Prompt(history)).call().content();
                    history.add(new AssistantMessage(response)); // 将 LLM 回复加入历史

                    // 解析回复
                    Matcher thinkMatcher = THINK_PATTERN.matcher(response);
                    if (thinkMatcher.find()) {
                        String thought = thinkMatcher.group(1).trim();
                        if (!thought.isEmpty()) {
                            ThinkResult tr = new ThinkResult();
                            tr.setThought(thought);
                            sink.next(ServerSentEvent.<String>builder()
                                    .event("thinking")
                                    .data(JSONUtil.toJsonStr(tr))
                                    .build());
                        }
                    }

                    Matcher answerMatcher = FINAL_ANSWER_PATTERN.matcher(response);
                    if (answerMatcher.find()) {
                        String finalAnswer = answerMatcher.group(1).trim();
                        sink.next(ServerSentEvent.<String>builder()
                                .event("message")
                                .data(finalAnswer)
                                .build());
                        sink.complete();
                        return;
                    }

                    Matcher actionMatcher = ACTION_PATTERN.matcher(response);
                    if (actionMatcher.find()) {
                        String toolName = actionMatcher.group(1).trim();
                        String toolInput = actionMatcher.group(2).trim();

                        ToolCallEvent tc = new ToolCallEvent();
                        tc.setTool(toolName);
                        tc.setInput(toolInput);
                        sink.next(ServerSentEvent.<String>builder()
                                .event("tool_call")
                                .data(JSONUtil.toJsonStr(tc))
                                .build());

                        long start = System.currentTimeMillis();
                        String observation;
                        try {
                            // 执行工具
                            observation = toolExecutionService.execute(toolName, toolInput);
                        } catch (Exception e) {
                            // 错误恢复
                            observation = "Error executing tool: " + e.getMessage() + ". Please modify your inputs and try again, or use another tool.";
                        }
                        long duration = System.currentTimeMillis() - start;

                        ToolResultEvent trEvent = new ToolResultEvent();
                        trEvent.setResult(observation);
                        trEvent.setDurationMs(duration);
                        sink.next(ServerSentEvent.<String>builder()
                                .event("tool_result")
                                .data(JSONUtil.toJsonStr(trEvent))
                                .build());

                        // 将观察结果作为用户消息返回给 LLM
                        history.add(new UserMessage("Observation: " + observation));
                    } else {
                        // 如果既没有 action 也没有 final answer，强行结束或提示
                        history.add(new UserMessage("Observation: Invalid format. Please use 'Final Answer:' or 'Action:' format."));
                    }
                }

                sink.next(ServerSentEvent.<String>builder()
                        .event("message")
                        .data("我已经尽力寻找答案了，但没有得出最终结论。(Reached max steps)")
                        .build());
                sink.complete();

            } catch (Exception e) {
                log.error("ReAct Loop 失败", e);
                sink.next(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(e.getMessage())
                        .build());
                sink.complete();
            }
        });
    }
}
