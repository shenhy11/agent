package com.agent.service.nlp.step;

import com.agent.service.nlp.NlpStep;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 关键词提取步骤
 */
@Component
public class KeywordsStep implements NlpStep {

    private final ChatClient chatClient;

    public KeywordsStep(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String getId() { return "keywords"; }

    @Override
    public String getName() { return "关键词提取"; }

    @Override
    public NlpStepResult execute(String input, Map<String, Object> context) {
        String prompt = """
                从以下文本提取最重要的关键词（5-10个），以 JSON 返回：
                {"keywords": ["词1", "词2", ...], "tags": ["标签1", "标签2"]}
                
                文本：
                %s
                """.formatted(input);
        try {
            String result = chatClient.prompt().user(prompt).call().content();
            return NlpStepResult.success(getId(), getName(), input, Map.of("keywords", result));
        } catch (Exception e) {
            return NlpStepResult.failure(getId(), getName(), e.getMessage());
        }
    }
}
