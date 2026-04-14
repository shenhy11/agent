package com.agent.service.multimodal;

import com.agent.service.rag.RagPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 以图搜图服务
 * 流程：上传图片 → 生成结构化描述 → 描述 embedding → VectorStore 匹配 → 返回产品推荐
 */
@Service
public class ImageSearchService {
    private static final Logger log = LoggerFactory.getLogger(ImageSearchService.class);

    private final VisionService visionService;
    private final VectorStore vectorStore;

    public ImageSearchService(VisionService visionService, VectorStore vectorStore) {
        this.visionService = visionService;
        this.vectorStore = vectorStore;
    }

    @jakarta.annotation.PostConstruct
    public void initProductData() {
        log.info("初始化以图搜图产品数据...");
        // §10.2: 编写 6 款产品的结构化视觉描述文本
        List<org.springframework.ai.document.Document> docs = List.of(
            new org.springframework.ai.document.Document("这是一款银色不锈钢表带的商务智能手表，表盘圆形，黑色背景，带有日期显示。风格沉稳，适合商务场合。外围有刻度圈设计。", Map.of("docType", "product_visual", "productId", "P001", "productName", "Business Pro", "series", "Classic")),
            new org.springframework.ai.document.Document("这是一款采用橙色硅胶表带的运动智能手表，表盘全屏幕显示无刻度，黑色圆形外观，轻量化设计。风格动感，适合户外运动场景。带有明显的三颗右侧实体按键。", Map.of("docType", "product_visual", "productId", "P002", "productName", "Sport Active", "series", "Sport")),
            new org.springframework.ai.document.Document("这是一款极简风格的白色真皮表带智能手表，方形玫瑰金表盘外框，白色极简表盘设计。适合日常休闲或女士佩戴，风格温馨且优雅。", Map.of("docType", "product_visual", "productId", "P003", "productName", "Elegant Square", "series", "Fashion")),
            new org.springframework.ai.document.Document("这是一款黑色氟橡胶表带的极限运动手表，钛金属银色大表盘，屏幕周围带有防护圈，厚重硬汉风格，带有潜水标识设计，适合潜水极限运动。", Map.of("docType", "product_visual", "productId", "P004", "productName", "Diver Max", "series", "Extreme")),
            new org.springframework.ai.document.Document("这是一块粉色编织回环表带的儿童智能手表，表盘较小，带有卡通动物界面和摄像头设计，整体偏塑料材质，可爱童趣风格。", Map.of("docType", "product_visual", "productId", "P005", "productName", "Kids Watch", "series", "Kids")),
            new org.springframework.ai.document.Document("这是一款采用米兰尼斯不锈钢编织表带的复古智能手表，金铜色圆形表壳，液晶显示数字表盘，风格融合了80年代复古与现代科技感。", Map.of("docType", "product_visual", "productId", "P006", "productName", "Retro Digital", "series", "Vintage"))
        );
        try {
            vectorStore.add(docs);
            log.info("成功加载 6 款产品的视觉描述至 VectorStore");
        } catch(Exception e) {
            log.warn("向 VectorStore 加载产品视觉描述失败: {}", e.getMessage());
        }
    }

    /**
     * 以图搜图主流程（§10.1、10.3、10.4）
     *
     * @param imageBytes 图片字节数组
     * @param mimeType   图片类型
     * @return 匹配到的产品描述列表（top-3）
     */
    public List<Map<String, Object>> searchByImage(byte[] imageBytes, String mimeType) {
        log.info("以图搜图: imageSize={}bytes", imageBytes.length);

        // Step 1: 用视觉模型生成图片结构化描述
        String imageDescription = visionService.describe(imageBytes, mimeType);
        log.debug("图片描述: {}", imageDescription);

        // Step 2: 用描述文本在产品向量库中做语义匹配（top-3）
        var results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(imageDescription)
                        .topK(3)
                        .filterExpression("docType == 'product_visual'")
                        .build()
        );

        if (results == null || results.isEmpty()) {
            // 容错：去掉 filter 重试
            results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(imageDescription)
                            .topK(3)
                            .build()
            );
        }

        String queryDesc = imageDescription;
        return results.stream()
                .map(doc -> Map.of(
                        "productId", doc.getMetadata().getOrDefault("productId", "unknown"),
                        "productName", doc.getMetadata().getOrDefault("productName", "未知产品"),
                        "series", doc.getMetadata().getOrDefault("series", ""),
                        "score", doc.getScore(),
                        "visualDescription", doc.getText(),
                        "imageDescription", (Object) queryDesc
                ))
                .collect(Collectors.toList());
    }
}
