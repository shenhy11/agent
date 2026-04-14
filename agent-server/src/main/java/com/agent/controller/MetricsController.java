package com.agent.controller;

import com.agent.model.dto.ApiResult;
import com.agent.service.agent.AiMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监控指控接口
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    public MetricsController(AiMetricsService aiMetricsService) {
        this.aiMetricsService = aiMetricsService;
    }


    private final AiMetricsService aiMetricsService;

    @GetMapping("/summary")
    public ApiResult<AiMetricsService.MetricsReport> getSummary() {
        return ApiResult.success(aiMetricsService.getReport());
    }
}
