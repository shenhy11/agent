package com.agent.service.nlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 文本分析服务
 * 通过 prompt engineering 调用 LLM 实现 NLP 能力
 */
@Service
public class TextAnalysisService {
    public TextAnalysisService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    private static final Logger log = LoggerFactory.getLogger(TextAnalysisService.class);


    private final ChatClient chatClient;

    /**
     * 文本摘要
     */
    public String summarize(String text) {
        log.info("文本摘要请求, 文本长度={}", text.length());

        if (text.length() < 20) {
            return "{\"summary\": \"" + text + "\", \"note\": \"文本过短，无需摘要\"}";
        }

        String prompt = """
                请对以下文本生成一份简洁的摘要。要求：
                1. 摘要长度不超过原文的30%%
                2. 保留核心信息和关键数据
                3. 以JSON格式返回，格式为: {"summary": "摘要内容", "wordCount": 原文字数, "summaryWordCount": 摘要字数}
                
                原文：
                %s
                """.formatted(text);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 情感分析
     */
    public String analyzeSentiment(String text) {
        log.info("情感分析请求, 文本长度={}", text.length());

        String prompt = """
                请分析以下文本的情感倾向。要求以JSON格式返回，格式为:
                {
                  "sentiment": "positive/negative/neutral",
                  "confidence": 0.0-1.0之间的置信度,
                  "keywords": ["关键词1", "关键词2"],
                  "reason": "判断理由的简短说明"
                }
                
                文本：
                %s
                """.formatted(text);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 关键词提取
     */
    public String extractKeywords(String text) {
        log.info("关键词提取请求, 文本长度={}", text.length());

        String prompt = """
                请从以下文本中提取关键词和短语。要求：
                1. 最多提取10个关键词
                2. 以JSON格式返回，格式为:
                {
                  "keywords": [
                    {"word": "关键词", "weight": 0.0-1.0之间的权重},
                    ...
                  ]
                }
                3. 按权重从高到低排序
                
                文本：
                %s
                """.formatted(text);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 多语翻译
     */
    public String translate(String text, String targetLang) {
        log.info("翻译请求, 目标语言={}, 文本长度={}", targetLang, text.length());

        String langName = "en".equals(targetLang) ? "英文" : "中文";

        String prompt = """
                请将以下文本翻译成%s。要求以JSON格式返回:
                {
                  "original": "原文",
                  "translated": "翻译结果",
                  "sourceLang": "源语言代码",
                  "targetLang": "目标语言代码"
                }
                
                文本：
                %s
                """.formatted(langName, text);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 通用 LLM 调用（供相似度接口使用）
     */
    public String rawChat(String prompt) {
        return chatClient.prompt().user(prompt).call().content();
    }
}

