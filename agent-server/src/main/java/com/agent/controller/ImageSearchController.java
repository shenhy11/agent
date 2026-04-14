package com.agent.controller;

import com.agent.model.dto.ApiResult;
import com.agent.service.multimodal.ImageSearchService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 以图搜图控制器
 */
@RestController
@RequestMapping("/api/multimodal")
public class ImageSearchController {

    private final ImageSearchService imageSearchService;

    public ImageSearchController(ImageSearchService imageSearchService) {
        this.imageSearchService = imageSearchService;
    }

    /**
     * 以图搜图
     * POST /api/multimodal/search-by-image
     */
    @PostMapping("/search-by-image")
    public ApiResult<List<Map<String, Object>>> searchByImage(
            @RequestParam("image") MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) return ApiResult.error(400, "请上传图片");
        List<Map<String, Object>> results = imageSearchService.searchByImage(
                image.getBytes(), image.getContentType());
        return ApiResult.success(results);
    }
}
