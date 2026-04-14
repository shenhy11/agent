package com.agent.service.nlp.step;

import com.agent.service.nlp.NlpStep;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 翻译步骤
 */
@Component
public class TranslateStep implements NlpStep {

    private final ChatClient chatClient;

    public TranslateStep(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String getId() { return "translate"; }

    @Override
    public String getName() { return "文本翻译"; }

    @Override
    public NlpStepResult execute(String input, Map<String, Object> context) {
        // 从 context 中获取目标语言，默认中译英
        String targetLang = (String) context.getOrDefault("translate_target", "英文");
        String prompt = """
                请将以下文本翻译为%s，自然流畅，以 JSON 返回：
                {"translation": "翻译结果", "sourceLang": "检测到的源语言"}
                
                文本：
                %s
                """.formatted(targetLang, input);
        try {
            String result = chatClient.prompt().user(prompt).call().content();
            return NlpStepResult.success(getId(), getName(), result,
                    Map.of("translation", result, "targetLang", targetLang));
        } catch (Exception e) {
            return NlpStepResult.failure(getId(), getName(), e.getMessage());
        }
    }
}
