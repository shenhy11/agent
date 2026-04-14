package com.agent.service.nlp.step;

import com.agent.service.nlp.NlpStep;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 摘要步骤
 */
@Component
public class SummarizeStep implements NlpStep {

    private final ChatClient chatClient;

    public SummarizeStep(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String getId() { return "summarize"; }

    @Override
    public String getName() { return "摘要提取"; }

    @Override
    public NlpStepResult execute(String input, Map<String, Object> context) {
        String prompt = """
                请对以下文本生成一份简洁摘要，保留核心信息。字数不超过原文30%%。
                直接返回摘要文本，不要输出额外标签。
                
                文本：
                %s
                """.formatted(input);
        try {
            String result = chatClient.prompt().user(prompt).call().content();
            return NlpStepResult.success(getId(), getName(), result,
                    Map.of("summary", result, "originalLength", input.length()));
        } catch (Exception e) {
            return NlpStepResult.failure(getId(), getName(), e.getMessage());
        }
    }
}
