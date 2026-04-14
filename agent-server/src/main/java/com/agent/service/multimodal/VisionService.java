package com.agent.service.multimodal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 多模态视觉服务（升级版）
 * 支持：单图/多图对话、OCR 提取、图片描述生成
 */
@Service
public class VisionService {
    private static final Logger log = LoggerFactory.getLogger(VisionService.class);

    @Value("${agent.vision.max-images:3}")
    private int maxImages;

    private final ChatClient chatClient;

    public VisionService(@Qualifier("visionChatClient") ChatClient chatClient) {
        // 使用独立的 visionChatClient（qwen-vl-max）处理图文理解
        this.chatClient = chatClient;
    }

    /**
     * 单图图文联合问答（同步）
     */
    public String chat(String question, byte[] imageBytes, String mimeType) {
        log.info("多模态问答: question={}, imageSize={}bytes, mimeType={}", question, imageBytes.length, mimeType);
        MimeType mediaType = parseMimeType(mimeType);
        Resource imageResource = new ByteArrayResource(imageBytes);

        return chatClient.prompt()
                .user(u -> u.text(question).media(mediaType, imageResource))
                .call()
                .content();
    }

    /**
     * 多图对话（最多 maxImages 张，超出拒绝）
     * §7.1、§7.2、§7.3
     */
    public String chatMultiImage(String question, List<byte[]> imageBytesList, List<String> mimeTypes) {
        if (imageBytesList == null || imageBytesList.isEmpty()) {
            throw new IllegalArgumentException("图片列表不能为空");
        }
        if (imageBytesList.size() > maxImages) {
            throw new IllegalArgumentException(
                    "图片数量超过上限（最多 " + maxImages + " 张），当前上传 " + imageBytesList.size() + " 张");
        }

        log.info("多图对话: question={}, imageCount={}", question, imageBytesList.size());

        return chatClient.prompt()
                .user(u -> {
                    u.text(question);
                    for (int i = 0; i < imageBytesList.size(); i++) {
                        MimeType mt = parseMimeType(mimeTypes.size() > i ? mimeTypes.get(i) : null);
                        u.media(mt, new ByteArrayResource(imageBytesList.get(i)));
                    }
                })
                .call()
                .content();
    }

    /**
     * 图文联合问答（流式）
     */
    public Flux<String> streamChat(String question, byte[] imageBytes, String mimeType) {
        log.info("多模态流式问答: question={}, imageSize={}bytes", question, imageBytes.length);
        MimeType mediaType = parseMimeType(mimeType);
        Resource imageResource = new ByteArrayResource(imageBytes);

        return chatClient.prompt()
                .user(u -> u.text(question).media(mediaType, imageResource))
                .stream()
                .content();
    }

    /**
     * OCR 文字提取（§9.1）
     * 利用 qwen-vl-max 视觉能力识别图中文字
     */
    public String ocr(byte[] imageBytes, String mimeType) {
        log.info("OCR 请求: imageSize={}bytes", imageBytes.length);
        String ocrPrompt = """
                请仔细识别图片中的所有文字内容，以 JSON 格式返回：
                {
                  "text_blocks": ["逐行文字1", "逐行文字2"...],
                  "structured_data": {
                    "title": "标题（如有）",
                    "tables": [["表格行..."]],
                    "keyValues": {"键": "值"}
                  }
                }
                请原样输出所有可见文字，不要添加解释。
                """;
        MimeType mediaType = parseMimeType(mimeType);
        return chatClient.prompt()
                .user(u -> u.text(ocrPrompt).media(mediaType, new ByteArrayResource(imageBytes)))
                .call()
                .content();
    }

    /**
     * OCR + LLM 串联分析（§9.3）
     * 先提取文字，再用 LLM 理解分析
     */
    public String ocrAndAnalyze(byte[] imageBytes, String mimeType, String analyzeQuestion) {
        // Step 1: OCR 提取
        String ocrResult = ocr(imageBytes, mimeType);
        // Step 2: LLM 理解分析
        String analyzePrompt = """
                以下是从图片中提取的文字内容：
                %s
                
                请根据上述内容回答以下问题或进行分析：
                %s
                """.formatted(ocrResult, analyzeQuestion != null ? analyzeQuestion : "请对内容进行综合分析和总结");
        return chatClient.prompt().user(analyzePrompt).call().content();
    }

    /**
     * 图片描述生成（§14.1）
     * 生成结构化图片描述（颜色/材质/外观/场景/风格）
     */
    public String describe(byte[] imageBytes, String mimeType) {
        log.info("图片描述生成: imageSize={}bytes", imageBytes.length);
        String describePrompt = """
                请仔细观察这张图片，以 JSON 格式生成结构化描述：
                {
                  "summary": "一句话整体描述",
                  "attributes": {
                    "color": ["颜色1", "颜色2"],
                    "material": "主要材质",
                    "style": "风格/系列",
                    "shape": "形状/外观",
                    "scene": "使用场景",
                    "special": "特殊特征或亮点"
                  }
                }
                """;
        MimeType mediaType = parseMimeType(mimeType);
        return chatClient.prompt()
                .user(u -> u.text(describePrompt).media(mediaType, new ByteArrayResource(imageBytes)))
                .call()
                .content();
    }

    /**
     * 解析 MIME 类型
     */
    public MimeType parseMimeType(String mimeType) {
        if (mimeType == null) return MimeTypeUtils.IMAGE_PNG;
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> MimeTypeUtils.IMAGE_JPEG;
            case "image/png" -> MimeTypeUtils.IMAGE_PNG;
            case "image/webp" -> MimeType.valueOf("image/webp");
            default -> MimeTypeUtils.IMAGE_PNG;
        };
    }
}
