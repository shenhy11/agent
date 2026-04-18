package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.dto.ApiResult;
import com.agent.service.rag.RagEvaluationService;
import org.springframework.web.bind.annotation.*;

/**
 * 评估控制器
 * 提供 RAG 评估运行和对比的端点
 */
@RestController
@RequestMapping("/api/v1/eval")
public class EvaluationController {
    public EvaluationController(RagEvaluationService ragEvaluationService) {
        this.ragEvaluationService = ragEvaluationService;
    }

    private static final Logger log = LoggerFactory.getLogger(EvaluationController.class);


    private final RagEvaluationService ragEvaluationService;

    /**
     * 运行 RAG 评估
     * POST /api/eval/run
     */
    @PostMapping("/run")
    public ApiResult<RagEvaluationService.EvaluationReport> runEvaluation(
            @RequestParam(defaultValue = "docs/eval/qa_dataset.json") String dataset) {
        
        log.info("触发 RAG 评估, 数据集: {}", dataset);
        RagEvaluationService.EvaluationReport report = ragEvaluationService.runEvaluation(dataset);
        
        if (report.getError() != null) {
            return ApiResult.error(500, "评估失败: " + report.getError());
        }
        
        return ApiResult.success(report);
    }
}
