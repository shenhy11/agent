package com.agent.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户画像服务
 * 异步从对话中提取用户偏好与关注点，存入 Redis Hash 中，形成长期记忆。
 */
@Service
public class UserProfileService {
    public UserProfileService(ChatClient chatClient, RedisTemplate<String, Object> redisTemplate) {
        this.chatClient = chatClient;
        this.redisTemplate = redisTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);


    private final ChatClient chatClient;
    private final RedisTemplate<String, Object> redisTemplate;
    // 降级内存
    private final Map<String, Object> localCache = new ConcurrentHashMap<>();

    private static final String PROFILE_KEY_PREFIX = "user:profile:";

    /**
     * 根据最近对话内容分析并更新用户标签
     *
     * @param userId      用户ID或会话ID
     * @param chatContent 最近几轮对话的文本
     */
    public void extractAndUpdateProfile(String userId, String chatContent) {
        if (chatContent == null || chatContent.length() < 10) return;

        try {
            String prompt = """
                    你是一个用户研究系统。请从以下用户对话中，提取出用户的产品偏好、关注点等静态标签特征（不超过5个词语，用逗号分隔）。
                    如果无法提取出有意义的偏好，返回"无"。
                    
                    对话内容：
                    %s
                    """.formatted(chatContent);

            String tagsDesc = chatClient.prompt().user(prompt).call().content();
            if (tagsDesc != null && !tagsDesc.trim().equals("无")) {
                String[] tags = tagsDesc.split("[,，]");
                for (String tag : tags) {
                    if (!tag.isBlank()) {
                        incrementTagWeight(userId, tag.trim());
                    }
                }
                log.info("用户 [{}] 画像已更新：{}", userId, tagsDesc);
            }
        } catch (Exception e) {
            log.warn("特征提取失败，忽略: {}", e.getMessage());
        }
    }

    /**
     * 增加标签权重
     */
    private void incrementTagWeight(String userId, String tag) {
        String key = PROFILE_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForHash().increment(key, tag, 1);
        } catch (Exception e) {
            // 降级为本地哈希
            String mapKey = key + ":" + tag;
            localCache.put(mapKey, (Integer) localCache.getOrDefault(mapKey, 0) + 1);
        }
    }

    /**
     * 获取用户所有标签及权重
     */
    public Map<Object, Object> getUserProfile(String userId) {
        String key = PROFILE_KEY_PREFIX + userId;
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.warn("无法从 Redis 获取画像", e);
            // 这里返回模拟结果
            return Map.of();
        }
    }
}
