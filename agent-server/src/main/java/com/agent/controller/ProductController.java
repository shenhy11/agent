package com.agent.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.model.ProductData;
import com.agent.model.dto.ApiResult;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 产品 API 控制器
 * 提供产品列表、详情、对比接口
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);


    /**
     * 获取产品列表（支持按系列筛选）
     * GET /api/products?series=sport
     */
    @GetMapping
    public ApiResult<List<Map<String, Object>>> listProducts(
            @RequestParam(required = false) String series) {
        log.info("查询产品列表, series={}", series);
        List<Map<String, Object>> products;
        if (series != null && !series.isBlank()) {
            products = ProductData.getBySeries(series);
        } else {
            products = ProductData.getAll();
        }
        return ApiResult.success(products);
    }

    /**
     * 获取单个产品详情
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ApiResult<Map<String, Object>> getProduct(@PathVariable String id) {
        log.info("查询产品详情, id={}", id);
        return ProductData.getById(id)
                .map(ApiResult::success)
                .orElse(ApiResult.error(404, "产品不存在: " + id));
    }

    /**
     * 产品对比
     * GET /api/products/compare?ids=sport-x2-pro,elite-e2-ultra
     */
    @GetMapping("/compare")
    public ApiResult<List<Map<String, Object>>> compareProducts(
            @RequestParam String ids) {
        log.info("产品对比, ids={}", ids);
        List<String> idList = Arrays.asList(ids.split(","));
        if (idList.size() < 2) {
            return ApiResult.error(400, "至少需要 2 个产品 ID 进行对比");
        }
        List<Map<String, Object>> products = ProductData.getByIds(idList);
        return ApiResult.success(products);
    }
}
