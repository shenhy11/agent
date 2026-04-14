package com.agent.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.json.JSONUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 具有摘要压缩和 Redis 持久化能力的三级记忆 ChatMemory
 * 1. 工作记忆：最近 N 轮对话原样保留
 * 2. 摘要记忆：超出 N 轮的部分由 LLM 压缩成摘要文本作为 SystemMessage 保留
 * 3. 持久化：自动同步到 Redis，连接失败时优雅降级为内存模式
 */
public class SummarizingChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(SummarizingChatMemory.class);

    private final ChatClient chatClient;
    private final RedisTemplate<String, Object> redisTemplate;
    // 当 Redis 不可用时的内存降级缓存
    private final Map<String, MemoryState> localCache = new ConcurrentHashMap<>();

    private final int windowSize; // 保持原文的最大消息数
    private static final String REDIS_KEY_PREFIX = "chat:memory:";
    private static final long TTL_HOURS = 24L;

    public SummarizingChatMemory(ChatClient chatClient, RedisTemplate<String, Object> redisTemplate, int windowSize) {
        this.chatClient = chatClient;
        this.redisTemplate = redisTemplate;
        this.windowSize = windowSize;
    }

    @Override
    public void add(String conversationId, List<Message> newMessages) {
        MemoryState state = getMemoryState(conversationId);

        // 过滤掉我们自己插入的纯系统摘要消息，避免它不断叠加
        List<Message> cleanNew = newMessages.stream()
                .filter(m -> !(m instanceof SystemMessage && m.getText().startsWith("[历史摘要]")))
                .collect(Collectors.toList());

        state.messages.addAll(cleanNew);

        // 如果超出窗口大小，执行摘要压缩
        if (state.messages.size() > windowSize) {
            compressMemory(state);
        }

        saveMemoryState(conversationId, state);
    }

    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, windowSize);
    }

    public List<Message> get(String conversationId, int lastN) {
        MemoryState state = getMemoryState(conversationId);
        List<Message> result = new ArrayList<>();

        // 如果有以前的摘要，插入到消息开头作为上下文增强
        if (state.summary != null && !state.summary.isBlank()) {
            result.add(new SystemMessage("[历史摘要]: " + state.summary));
        }

        List<Message> msgs = state.messages;
        int startIndex = Math.max(0, msgs.size() - windowSize);
        result.addAll(msgs.subList(startIndex, msgs.size()));

        return result;
    }

    @Override
    public void clear(String conversationId) {
        try {
            redisTemplate.delete(REDIS_KEY_PREFIX + conversationId);
        } catch (Exception e) {
            localCache.remove(conversationId);
        }
    }

    /**
     * 将早期消息压缩成摘要
     */
    private void compressMemory(MemoryState state) {
        // 需要压缩的消息：除保留窗口之外的最早的那些
        int excess = state.messages.size() - (windowSize / 2);
        if (excess <= 0) return;

        List<Message> toCompress = new ArrayList<>(state.messages.subList(0, excess));

        // 拼接成文本
        String historyText = toCompress.stream().map(m -> {
            String role = m instanceof UserMessage ? "User" : "Assistant";
            return role + ": " + m.getText();
        }).collect(Collectors.joining("\n"));

        String prompt = "请将以下新发生的对话内容，追加或融合到现有的「历史摘要」中。如果原来没有摘要，则直接总结以下内容。要求简明扼要，重点保留用户偏好和讨论过的核心结论。\n\n"
                + "【现有摘要】\n" + (state.summary != null ? state.summary : "无") + "\n\n"
                + "【新对话】\n" + historyText;

        try {
            String newSummary = chatClient.prompt().user(prompt).call().content();
            state.summary = newSummary;
            // 移除已压缩的消息
            state.messages.subList(0, excess).clear();
            log.info("对话触发压缩，生成新摘要：{}", newSummary);
        } catch (Exception e) {
            log.warn("对话摘要压缩失败，跳过此次压缩: {}", e.getMessage());
        }
    }

    /**
     * 获取记忆状态（带降级）
     */
    private MemoryState getMemoryState(String conversationId) {
        try {
            String json = (String) redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + conversationId);
            if (json != null) {
                MemoryState state = JSONUtil.toBean(json, MemoryState.class);
                if (state.messages == null) state.messages = new ArrayList<>();
                return state;
            }
        } catch (Exception e) {
            log.warn("Redis 获取会话异常，降级为内存", e);
            return localCache.getOrDefault(conversationId, new MemoryState());
        }
        return new MemoryState();
    }

    /**
     * 保存记忆状态（带降级）
     */
    private void saveMemoryState(String conversationId, MemoryState state) {
        try {
            redisTemplate.opsForValue().set(
                    REDIS_KEY_PREFIX + conversationId,
                    JSONUtil.toJsonStr(state),
                    TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (Exception e) {
            log.warn("Redis 保存会话异常，降级为内存", e);
            localCache.put(conversationId, state);
        }
    }

    /**
     * 内部记忆状态 POJO（直接使用 public 字段简化序列化/反序列化）
     */
    public static class MemoryState {
        public String summary;
        public List<Message> messages = new ArrayList<>();
    }
}
