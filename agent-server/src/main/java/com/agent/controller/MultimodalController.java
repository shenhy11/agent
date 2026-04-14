package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.dto.ApiResult;
import com.agent.service.multimodal.VisionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 多模态 API 控制器（升级版）
 * 支持：单图/多图对话、OCR 提取、OCR+LLM 分析、图片描述生成
 */
@RestController
@RequestMapping("/api/multimodal")
public class MultimodalController {
    public MultimodalController(VisionService visionService) {
        this.visionService = visionService;
    }

    private static final Logger log = LoggerFactory.getLogger(MultimodalController.class);

    private final VisionService visionService;

    /** 支持的图片格式 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    /** 最大图片大小 5MB */
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    /**
     * 图文联合问答（同步，单图）
     * POST /api/multimodal/chat
     */
    @PostMapping("/chat")
    public ApiResult<String> chat(
            @RequestParam("question") String question,
            @RequestParam("image") MultipartFile image) throws IOException {

        String error = validateImage(image);
        if (error != null) return ApiResult.error(400, error);

        String result = visionService.chat(question, image.getBytes(), image.getContentType());
        return ApiResult.success(result);
    }

    /**
     * 多图对话（最多 3 张，超出返回 400）
     * POST /api/multimodal/multi-chat
     */
    @PostMapping("/multi-chat")
    public ApiResult<String> multiChat(
            @RequestParam("question") String question,
            @RequestParam("images") List<MultipartFile> images) throws IOException {

        if (images == null || images.isEmpty()) return ApiResult.error(400, "请上传至少一张图片");
        if (images.size() > 3) return ApiResult.error(400, "图片数量超过上限（最多 3 张）");

        List<byte[]> imageBytes = new ArrayList<>();
        List<String> mimeTypes = new ArrayList<>();
        for (MultipartFile img : images) {
            String err = validateImage(img);
            if (err != null) return ApiResult.error(400, err);
            imageBytes.add(img.getBytes());
            mimeTypes.add(img.getContentType());
        }

        String result = visionService.chatMultiImage(question, imageBytes, mimeTypes);
        return ApiResult.success(result);
    }

    /**
     * 图文联合问答（流式）
     * POST /api/multimodal/chat/stream
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestParam("question") String question,
            @RequestParam("image") MultipartFile image) throws IOException {

        String error = validateImage(image);
        if (error != null) return Flux.just("错误: " + error);

        return visionService.streamChat(question, image.getBytes(), image.getContentType());
    }

    /**
     * 纯 OCR 文字提取
     * POST /api/multimodal/ocr
     */
    @PostMapping("/ocr")
    public ApiResult<String> ocr(@RequestParam("image") MultipartFile image) throws IOException {
        String error = validateImage(image);
        if (error != null) return ApiResult.error(400, error);
        String result = visionService.ocr(image.getBytes(), image.getContentType());
        return ApiResult.success(result);
    }

    /**
     * OCR + LLM 串联分析
     * POST /api/multimodal/ocr-analyze
     */
    @PostMapping("/ocr-analyze")
    public ApiResult<String> ocrAnalyze(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "question", required = false) String question) throws IOException {
        String error = validateImage(image);
        if (error != null) return ApiResult.error(400, error);
        String result = visionService.ocrAndAnalyze(image.getBytes(), image.getContentType(), question);
        return ApiResult.success(result);
    }

    /**
     * 图片描述生成（结构化）
     * POST /api/multimodal/describe
     */
    @PostMapping("/describe")
    public ApiResult<String> describe(@RequestParam("image") MultipartFile image) throws IOException {
        String error = validateImage(image);
        if (error != null) return ApiResult.error(400, error);
        String result = visionService.describe(image.getBytes(), image.getContentType());
        return ApiResult.success(result);
    }

    /**
     * 校验上传的图片
     */
    private String validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) return "请上传图片";
        if (!ALLOWED_TYPES.contains(image.getContentType())) return "不支持的图片格式，仅支持 JPEG/PNG/WebP";
        if (image.getSize() > MAX_SIZE) return "图片大小不能超过 5MB";
        return null;
    }
}
