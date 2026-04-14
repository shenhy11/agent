package com.agent.service.nlp;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 规则引擎意图识别服务
 * 基于关键词 + 正则匹配，无需调用 LLM，响应极快
 */
@Service
public class RuleBasedIntentService {

    // 意图规则库：意图类型 → 关键词列表（任意匹配即触发）
    private static final Map<String, String[]> INTENT_RULES = new HashMap<>();

    static {
        INTENT_RULES.put("CONSULT", new String[]{
                "规格", "参数", "功能", "材质", "表盘", "续航", "防水", "什么", "介绍", "了解"
        });
        INTENT_RULES.put("COMPARE", new String[]{
                "对比", "比较", "哪款好", "区别", "差别", "优缺点", "vs", "还是"
        });
        INTENT_RULES.put("PURCHASE", new String[]{
                "购买", "价格", "多少钱", "优惠", "折扣", "怎么买", "下单", "付款", "库存"
        });
        INTENT_RULES.put("COMPLAINT", new String[]{
                "投诉", "质量差", "问题", "不满意", "退款", "骗", "假货", "差评"
        });
        INTENT_RULES.put("SUPPORT", new String[]{
                "退货", "换货", "维修", "保修", "快递", "物流", "到了吗", "售后"
        });
    }

    /**
     * 规则引擎意图识别
     *
     * @param text 用户输入文本
     * @return 识别结果
     */
    public Map<String, Object> classify(String text) {
        String matchedIntent = "OTHER";
        String matchedKeyword = null;
        double confidence = 0.5;
        int maxMatches = 0;

        for (Map.Entry<String, String[]> entry : INTENT_RULES.entrySet()) {
            int matches = 0;
            String firstKeyword = null;
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    matches++;
                    if (firstKeyword == null) firstKeyword = keyword;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                matchedIntent = entry.getKey();
                matchedKeyword = firstKeyword;
                confidence = Math.min(0.5 + matches * 0.1, 0.95);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("intent", matchedIntent);
        result.put("confidence", confidence);
        result.put("mode", "rule");
        result.put("matchedKeyword", matchedKeyword);
        result.put("reason", matchedKeyword != null
                ? "匹配到关键词: " + matchedKeyword
                : "未匹配到任何规则，归类为 OTHER");
        return result;
    }
}
