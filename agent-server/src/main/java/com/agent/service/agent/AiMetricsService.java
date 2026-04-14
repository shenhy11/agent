package com.agent.service.agent;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 可观测性服务
 * 追踪 Token 消耗、耗时、工具调用等指标 (内存简化版，实际生产可用 Micrometer 注入)
 */
@Service
public class AiMetricsService {

    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong promptTokens = new AtomicLong(0);
    private final AtomicLong completionTokens = new AtomicLong(0);

    private final AtomicInteger toolCallsTotal = new AtomicInteger(0);
    private final AtomicInteger toolCallsSuccess = new AtomicInteger(0);
    private final AtomicInteger toolCallsFailed = new AtomicInteger(0);

    private final AtomicLong totalRequestMs = new AtomicLong(0);
    private final AtomicInteger totalRequests = new AtomicInteger(0);

    public void recordTokens(long prompt, long completion) {
        promptTokens.addAndGet(prompt);
        completionTokens.addAndGet(completion);
        totalTokens.addAndGet(prompt + completion);
    }

    public void recordToolCall(boolean success) {
        toolCallsTotal.incrementAndGet();
        if (success) {
            toolCallsSuccess.incrementAndGet();
        } else {
            toolCallsFailed.incrementAndGet();
        }
    }

    public void recordRequestMetrics(long ms) {
        totalRequests.incrementAndGet();
        totalRequestMs.addAndGet(ms);
    }

    public MetricsReport getReport() {
        MetricsReport report = new MetricsReport();
        report.setTotalTokens(totalTokens.get());
        report.setPromptTokens(promptTokens.get());
        report.setCompletionTokens(completionTokens.get());
        report.setToolCallsTotal(toolCallsTotal.get());
        report.setToolCallsSuccess(toolCallsSuccess.get());
        report.setToolCallsFailed(toolCallsFailed.get());
        report.setTotalRequests(totalRequests.get());
        if (totalRequests.get() > 0) {
            report.setAverageLatencyMs(totalRequestMs.get() / totalRequests.get());
        } else {
            report.setAverageLatencyMs(0);
        }
        return report;
    }
    public static class MetricsReport {
        private long totalRequests;
        private long totalTokens;
        private long promptTokens;
        private long completionTokens;
        private long averageLatencyMs;
        private long toolCallsTotal;
        private long toolCallsSuccess;
        private long toolCallsFailed;

        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

        public long getTotalTokens() { return totalTokens; }
        public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }

        public long getPromptTokens() { return promptTokens; }
        public void setPromptTokens(long promptTokens) { this.promptTokens = promptTokens; }

        public long getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(long completionTokens) { this.completionTokens = completionTokens; }

        public long getAverageLatencyMs() { return averageLatencyMs; }
        public void setAverageLatencyMs(long averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

        public long getToolCallsTotal() { return toolCallsTotal; }
        public void setToolCallsTotal(long toolCallsTotal) { this.toolCallsTotal = toolCallsTotal; }

        public long getToolCallsSuccess() { return toolCallsSuccess; }
        public void setToolCallsSuccess(long toolCallsSuccess) { this.toolCallsSuccess = toolCallsSuccess; }

        public long getToolCallsFailed() { return toolCallsFailed; }
        public void setToolCallsFailed(long toolCallsFailed) { this.toolCallsFailed = toolCallsFailed; }
    }

}
