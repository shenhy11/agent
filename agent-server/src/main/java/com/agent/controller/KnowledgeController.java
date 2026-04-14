package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.dto.ApiResult;
import com.agent.service.rag.DocumentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * 知识库 API 控制器
 * 提供文档上传、删除等知识库管理接口
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    public KnowledgeController(DocumentService documentService) {
        this.documentService = documentService;
    }

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);


    private final DocumentService documentService;

    /**
     * 上传文档到知识库
     * POST /api/knowledge/{knowledgeId}/upload
     */
    @PostMapping("/{knowledgeId}/upload")
    public ApiResult<Map<String, Object>> uploadDocument(
            @PathVariable String knowledgeId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("上传文档: {} -> 知识库: {}", file.getOriginalFilename(), knowledgeId);

        InputStreamResource resource = new InputStreamResource(file.getInputStream()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        int chunks = documentService.ingestDocument(resource, knowledgeId);

        return ApiResult.success(Map.of(
                "fileName", file.getOriginalFilename(),
                "knowledgeId", knowledgeId,
                "chunks", chunks
        ));
    }

    /**
     * 删除知识库
     * DELETE /api/knowledge/{knowledgeId}
     */
    @DeleteMapping("/{knowledgeId}")
    public ApiResult<String> deleteKnowledge(@PathVariable String knowledgeId) {
        log.info("删除知识库: {}", knowledgeId);
        documentService.deleteByKnowledgeId(knowledgeId);
        return ApiResult.success("知识库已删除: " + knowledgeId);
    }
}
