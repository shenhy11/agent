package com.agent.service.nlp.step;

import com.agent.service.nlp.NlpStep;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 情感分析步骤
 */
@Component
public class SentimentStep implements NlpStep {

    private final ChatClient chatClient;

    public SentimentStep(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String getId() { return "sentiment"; }

    @Override
    public String getName() { return "情感分析"; }

    @Override
    public NlpStepResult execute(String input, Map<String, Object> context) {
        String prompt = """
                分析以下文本的情感倾向，以 JSON 格式返回：
                {"sentiment": "positive/negative/neutral", "confidence": 0.0-1.0, "reason": "简短理由"}
                
                文本：
                %s
                """.formatted(input);
        try {
            String result = chatClient.prompt().user(prompt).call().content();
            return NlpStepResult.success(getId(), getName(), input, Map.of("sentiment", result));
        } catch (Exception e) {
            return NlpStepResult.failure(getId(), getName(), e.getMessage());
        }
    }
}
