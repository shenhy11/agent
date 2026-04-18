package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.dto.ApiResult;
import com.agent.service.nlp.TextAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * NLP 文本分析 API 控制器
 * 提供摘要、情感分析、关键词提取、翻译接口
 */
@RestController
@RequestMapping("/api/v1/nlp")
public class NlpController {
    public NlpController(TextAnalysisService textAnalysisService) {
        this.textAnalysisService = textAnalysisService;
    }

    private static final Logger log = LoggerFactory.getLogger(NlpController.class);


    private final TextAnalysisService textAnalysisService;

    /**
     * 文本摘要
     * POST /api/nlp/summarize
     */
    @PostMapping("/summarize")
    public ApiResult<String> summarize(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.isBlank()) {
            return ApiResult.error(400, "text 参数不能为空");
        }
        String result = textAnalysisService.summarize(text);
        return ApiResult.success(result);
    }

    /**
     * 情感分析
     * POST /api/nlp/sentiment
     */
    @PostMapping("/sentiment")
    public ApiResult<String> sentiment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.isBlank()) {
            return ApiResult.error(400, "text 参数不能为空");
        }
        String result = textAnalysisService.analyzeSentiment(text);
        return ApiResult.success(result);
    }

    /**
     * 关键词提取
     * POST /api/nlp/keywords
     */
    @PostMapping("/keywords")
    public ApiResult<String> keywords(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.isBlank()) {
            return ApiResult.error(400, "text 参数不能为空");
        }
        String result = textAnalysisService.extractKeywords(text);
        return ApiResult.success(result);
    }

    /**
     * 多语翻译
     * POST /api/nlp/translate
     */
    @PostMapping("/translate")
    public ApiResult<String> translate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String targetLang = request.getOrDefault("targetLang", "en");
        if (text == null || text.isBlank()) {
            return ApiResult.error(400, "text 参数不能为空");
        }
        String result = textAnalysisService.translate(text, targetLang);
        return ApiResult.success(result);
    }

    /**
     * 批量文本处理（§6.1）
     * POST /api/nlp/batch
     * Body: {"texts": ["text1","text2"...], "task": "summarize|sentiment|keywords"}
     */
    @PostMapping("/batch")
    public ApiResult<java.util.List<String>> batch(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        java.util.List<String> texts = (java.util.List<String>) request.get("texts");
        String task = (String) request.getOrDefault("task", "summarize");
        if (texts == null || texts.isEmpty()) {
            return ApiResult.error(400, "texts 数组不能为空");
        }
        java.util.List<String> results = texts.stream().map(t -> switch (task) {
            case "sentiment" -> textAnalysisService.analyzeSentiment(t);
            case "keywords" -> textAnalysisService.extractKeywords(t);
            default -> textAnalysisService.summarize(t);
        }).toList();
        return ApiResult.success(results);
    }

    /**
     * 文本相似度计算（§6.2）
     * POST /api/nlp/similarity
     */
    @PostMapping("/similarity")
    public ApiResult<Map<String, Object>> similarity(@RequestBody Map<String, String> request) {
        String textA = request.get("textA");
        String textB = request.get("textB");
        if (textA == null || textB == null) {
            return ApiResult.error(400, "textA 和 textB 均为必填");
        }
        String prompt = """
                计算以下两段文本的语义相似度（0.0 到 1.0），以 JSON 返回：
                {"similarity": 0.0-1.0, "reason": "简短解释"}
                
                文本A：%s
                
                文本B：%s
                """.formatted(textA, textB);
        String result = textAnalysisService.rawChat(prompt);
        return ApiResult.success(Map.of("textA", textA, "textB", textB, "result", result));
    }
}

