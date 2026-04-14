package com.agent.service.nlp.step;

import com.agent.service.nlp.NlpStep;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 意图识别 NlpStep（LLM 模式）
 * 支持 6 种意图分类，搭配规则引擎使用
 */
@Component
public class IntentStep implements NlpStep {

    private final ChatClient chatClient;

    public IntentStep(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String getId() { return "intent"; }

    @Override
    public String getName() { return "意图识别"; }

    @Override
    public NlpStepResult execute(String input, Map<String, Object> context) {
        String prompt = """
                你是一个意图分类专家，针对手表品牌客服场景。
                请分析以下用户输入的意图，以 JSON 返回：
                {
                  "intent": "意图类型",
                  "confidence": 0.0-1.0,
                  "subIntent": "细分意图（可选）",
                  "reason": "判断依据"
                }
                
                意图类型枚举（intent）：
                - CONSULT: 产品咨询（询问规格、功能、材质）
                - COMPARE: 产品对比（对比两款或多款产品）
                - PURCHASE: 购买意向（询问价格、库存、下单）
                - COMPLAINT: 投诉建议（产品质量、服务问题）
                - SUPPORT: 售后支持（退换货、维修、物流）
                - OTHER: 其他
                
                用户输入：
                %s
                """.formatted(input);
        try {
            String result = chatClient.prompt().user(prompt).call().content();
            return NlpStepResult.success(getId(), getName(), input,
                    Map.of("intent", result, "mode", "llm"));
        } catch (Exception e) {
            return NlpStepResult.failure(getId(), getName(), e.getMessage());
        }
    }
}
