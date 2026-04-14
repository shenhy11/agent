package com.agent.service.rag;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 递归字符分块器
 * 按段落→句子→字符逐级切分，优先保持语义完整性
 * 分级分隔符：\n\n（段落）→ \n（换行）→ 。！？（句子）→ 固定长度
 */
public class RecursiveCharacterSplitter {

    /** 递归分隔符列表（从大粒度到小粒度） */
    private static final String[] SEPARATORS = {"\n\n", "\n", "。", "！", "？", ".", "!", "?", " ", ""};

    private final int chunkSize;
    private final int chunkOverlap;

    public RecursiveCharacterSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * 对文档列表执行递归字符分块
     */
    public List<Document> apply(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            List<String> chunks = splitText(doc.getText(), 0);
            for (int i = 0; i < chunks.size(); i++) {
                // 继承原文档的元数据
                Map<String, Object> metadata = new java.util.HashMap<>(doc.getMetadata());
                metadata.put("chunk_index", i);
                metadata.put("chunk_strategy", "recursive");
                result.add(new Document(chunks.get(i), metadata));
            }
        }
        return result;
    }

    /**
     * 递归切分文本
     *
     * @param text           待切分文本
     * @param separatorIndex 当前使用的分隔符索引
     * @return 切分后的文本块列表
     */
    private List<String> splitText(String text, int separatorIndex) {
        // 文本已足够短，直接返回
        if (text.length() <= chunkSize) {
            return text.isBlank() ? List.of() : List.of(text.strip());
        }

        // 所有分隔符用完，强制按固定长度切分
        if (separatorIndex >= SEPARATORS.length) {
            return forceSplit(text);
        }

        String separator = SEPARATORS[separatorIndex];
        String[] parts;

        if (separator.isEmpty()) {
            return forceSplit(text);
        } else {
            parts = text.split(java.util.regex.Pattern.quote(separator), -1);
        }

        // 如果分隔符无法切分（只有一个部分），尝试下一个分隔符
        if (parts.length <= 1) {
            return splitText(text, separatorIndex + 1);
        }

        // 将小片段合并为不超过 chunkSize 的块
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            String partWithSep = part + separator;

            if (current.length() + partWithSep.length() > chunkSize && !current.isEmpty()) {
                // 当前块已够大，保存并开始新块
                result.add(current.toString().strip());

                // 重叠处理：保留最后 chunkOverlap 个字符
                String overlap = current.length() > chunkOverlap
                        ? current.substring(current.length() - chunkOverlap)
                        : current.toString();
                current = new StringBuilder(overlap);
            }

            current.append(partWithSep);
        }

        // 最后一块
        if (!current.isEmpty()) {
            String lastChunk = current.toString().strip();
            if (!lastChunk.isEmpty()) {
                // 如果最后一块太长，递归用下一级分隔符切分
                if (lastChunk.length() > chunkSize) {
                    result.addAll(splitText(lastChunk, separatorIndex + 1));
                } else {
                    result.add(lastChunk);
                }
            }
        }

        return result;
    }

    /**
     * 强制按固定长度切分（最后手段）
     */
    private List<String> forceSplit(String text) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            result.add(text.substring(start, end).strip());
            start = end - chunkOverlap;
            if (start >= text.length()) break;
        }
        return result;
    }
}
