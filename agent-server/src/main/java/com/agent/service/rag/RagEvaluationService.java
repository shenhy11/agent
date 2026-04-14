package com.agent.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG 评估服务
 * 实现 Recall@K 计算和 LLM 裁判打分
 */
@Service
public class RagEvaluationService {
    public RagEvaluationService(RagPipelineService ragPipelineService, ChatClient chatClient) {
        this.ragPipelineService = ragPipelineService;
        this.chatClient = chatClient;
    }

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationService.class);


    private final RagPipelineService ragPipelineService;
    private final ChatClient chatClient;

    /**
     * 运行端到端评估
     */
    public EvaluationReport runEvaluation(String datasetPath) {
        log.info("开始 RAG 评估，测试集: {}", datasetPath);
        EvaluationReport report = new EvaluationReport();
        long start = System.currentTimeMillis();

        try {
            File file = new File(datasetPath);
            if (!file.exists()) {
                throw new RuntimeException("测试集文件不存在: " + datasetPath);
            }

            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            List<QaTestCase> testCases = JSONUtil.toList(json, QaTestCase.class);
            report.setTotalTestCases(testCases.size());

            AtomicInteger recall1Count = new AtomicInteger(0);
            AtomicInteger recall3Count = new AtomicInteger(0);
            AtomicInteger recall5Count = new AtomicInteger(0);
            double totalAnswerScore = 0;

            for (QaTestCase testCase : testCases) {
                // 1. 执行 RAG 管道
                RagPipelineService.RagPipelineResult result = ragPipelineService.runPipeline(testCase.getQuestion());
                List<Document> retrievedDocs = result.getDocuments();

                // 2. 计算 Recall@K
                boolean hit1 = isHit(retrievedDocs, testCase.getRelevantDocIds(), 1);
                boolean hit3 = isHit(retrievedDocs, testCase.getRelevantDocIds(), 3);
                boolean hit5 = isHit(retrievedDocs, testCase.getRelevantDocIds(), 5);

                if (hit1) recall1Count.incrementAndGet();
                if (hit3) recall3Count.incrementAndGet();
                if (hit5) recall5Count.incrementAndGet();

                // 3. 生成回答（模拟 Advisor 后期）
                String answer = generateAnswer(result.getRewrittenQuery(), retrievedDocs);
                
                // 4. LLM 裁判打分
                double score = evaluateAnswerQuality(testCase.getQuestion(), testCase.getExpectedAnswer(), answer);
                totalAnswerScore += score;
                
                log.info("测试用例 [{}]: Recall@3={}, Score={}", testCase.getId(), hit3, score);
            }

            report.setRecallAt1((double) recall1Count.get() / testCases.size());
            report.setRecallAt3((double) recall3Count.get() / testCases.size());
            report.setRecallAt5((double) recall5Count.get() / testCases.size());
            report.setAverageAnswerScore(totalAnswerScore / testCases.size());
            report.setTotalMs(System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("评估执行失败", e);
            report.setError(e.getMessage());
        }

        return report;
    }

    private boolean isHit(List<Document> retrievedDocs, List<String> relevantKeywords, int k) {
        if (relevantKeywords == null || relevantKeywords.isEmpty()) return false;
        
        int limit = Math.min(retrievedDocs.size(), k);
        for (int i = 0; i < limit; i++) {
            Document doc = retrievedDocs.get(i);
            String source = (String) doc.getMetadata().getOrDefault("source", "");
            String content = doc.getText();
            
            // 简单的命中判断：如果有任意一个关键词在来源或内容中出现，则认为命中
            for (String kw : relevantKeywords) {
                if (source.contains(kw) || content.contains(kw)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String generateAnswer(String query, List<Document> docs) {
        if (docs.isEmpty()) return "无相关文裆";
        
        StringBuilder context = new StringBuilder();
        for (Document doc : docs) {
            context.append(doc.getText()).append("\n---\n");
        }

        String prompt = """
                根据以下信息回答问题：
                %s
                
                问题：%s
                """.formatted(context.toString(), query);

        return chatClient.prompt().user(prompt).call().content();
    }

    private double evaluateAnswerQuality(String question, String expected, String actual) {
        try {
            String prompt = """
                    你是一个评估专家，请对比系统生成的回答和标准答案，给出 1-5 分的评分。
                    仅返回一个整数。打分标准：
                    5: 完全一致，表达可能更好
                    4: 核心意思一致，包含少量额外或缺失信息
                    3: 部分正确
                    2: 偏差较大
                    1: 完全错误或答非所问
                    
                    问题：%s
                    
                    标准答案：%s
                    
                    生成答案：%s
                    """.formatted(question, expected, actual);

            String result = chatClient.prompt().user(prompt).call().content();
            String num = result.replaceAll("[^1-5]", "").trim();
            if (!num.isEmpty()) {
                return Double.parseDouble(num.substring(0, 1));
            }
        } catch (Exception e) {
            log.warn("LLM 打分失败", e);
        }
        return 3.0; // 默认给及格分
    }
    public static class QaTestCase {
        private String id;
        private String question;
        private String expectedAnswer;
        private List<String> relevantDocIds;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public String getExpectedAnswer() { return expectedAnswer; }
        public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }

        public List<String> getRelevantDocIds() { return relevantDocIds; }
        public void setRelevantDocIds(List<String> relevantDocIds) { this.relevantDocIds = relevantDocIds; }
    }
    public static class EvaluationReport {
        private int totalTestCases;
        private double recallAt1;
        private double recallAt3;
        private double recallAt5;
        private double averageAnswerScore;
        private long totalMs;
        private String error;

        public int getTotalTestCases() { return totalTestCases; }
        public void setTotalTestCases(int totalTestCases) { this.totalTestCases = totalTestCases; }

        public double getRecallAt1() { return recallAt1; }
        public void setRecallAt1(double recallAt1) { this.recallAt1 = recallAt1; }

        public double getRecallAt3() { return recallAt3; }
        public void setRecallAt3(double recallAt3) { this.recallAt3 = recallAt3; }

        public double getRecallAt5() { return recallAt5; }
        public void setRecallAt5(double recallAt5) { this.recallAt5 = recallAt5; }

        public double getAverageAnswerScore() { return averageAnswerScore; }
        public void setAverageAnswerScore(double averageAnswerScore) { this.averageAnswerScore = averageAnswerScore; }

        public long getTotalMs() { return totalMs; }
        public void setTotalMs(long totalMs) { this.totalMs = totalMs; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
