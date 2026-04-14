package com.agent.service.nlp.step;

import com.agent.service.nlp.NlpStep;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * NER 命名实体识别步骤
 * 通过 few-shot prompt 让 LLM 提取结构化实体，支持 6 种实体类型
 */
@Component
public class NerStep implements NlpStep {

    private final ChatClient chatClient;

    public NerStep(@Qualifier("primaryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String getId() { return "ner"; }

    @Override
    public String getName() { return "命名实体识别"; }

    @Override
    public NlpStepResult execute(String input, Map<String, Object> context) {
        String prompt = """
                你是一个命名实体识别专家，专注于手表/智能设备领域。
                请从下面的文本中提取所有命名实体，以 JSON 格式返回：
                {
                  "entities": [
                    {"text": "实体文本", "type": "实体类型", "start": 起始偏移, "end": 结束偏移}
                  ]
                }
                
                支持的实体类型（type）：
                - PRODUCT: 产品型号（如 Sport X2 Pro、Chronos Elite）
                - BRAND: 品牌名（如 ChronoTech、Apple Watch）
                - PRICE: 价格（如 ¥1999、2999元）
                - SPEC: 规格参数（如 防水50米、42mm表盘、蓝宝石玻璃）
                - TIME: 时间相关（如 7天续航、2024年款）
                - PERSON: 人名
                
                few-shot 示例：
                输入："ChronoTech Sport X2 Pro，价格 ¥1999，防水50米，7天续航"
                输出：{"entities":[
                  {"text":"ChronoTech","type":"BRAND","start":0,"end":10},
                  {"text":"Sport X2 Pro","type":"PRODUCT","start":11,"end":23},
                  {"text":"¥1999","type":"PRICE","start":28,"end":33},
                  {"text":"防水50米","type":"SPEC","start":35,"end":40},
                  {"text":"7天续航","type":"SPEC","start":42,"end":47}
                ]}
                
                待分析文本：
                %s
                """.formatted(input);
        try {
            String result = chatClient.prompt().user(prompt).call().content();
            return NlpStepResult.success(getId(), getName(), input,
                    Map.of("ner", result, "text", input));
        } catch (Exception e) {
            return NlpStepResult.failure(getId(), getName(), e.getMessage());
        }
    }
}
