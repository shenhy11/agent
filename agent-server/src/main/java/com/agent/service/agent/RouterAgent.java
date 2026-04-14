package com.agent.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 路由 Agent
 * 负责分发用户的请求到最合适的下游 Agent 执行
 */
@Service
public class RouterAgent {
    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);


    private final ChatClient routerChatClient;
    private final ChatClient customerServiceChatClient;
    private final ChatClient afterSalesChatClient;

    public RouterAgent(ChatClient.Builder builder,
                       @Qualifier("customerServiceChatClient") ChatClient customerServiceChatClient,
                       @Qualifier("afterSalesChatClient") ChatClient afterSalesChatClient) {
        this.routerChatClient = builder.defaultSystem("你是一个意图识别路由器。").build();
        this.customerServiceChatClient = customerServiceChatClient;
        this.afterSalesChatClient = afterSalesChatClient;
    }

    /**
     * 根据意图路由获取合适的 ChatClient
     */
    public ChatClient routeChatClient(String message) {
        // 短消息直接给售前引导
        if (message == null || message.trim().length() < 2) {
            return customerServiceChatClient;
        }

        try {
            String prompt = """
                    请分析以下用户输入的意图分类：
                    A - 售前咨询 (询问产品规格、对比、推荐、买手表的建议、线下门店地址等)
                    B - 售后支持 (询问物流、快递情况、退换货规则、手表故障维修等)
                    
                    如果无法判断，默认选 A。
                    仅返回一个字母：A 或 B。不要输出其他任何字符。
                    
                    输入：%s
                    """.formatted(message);

            String category = routerChatClient.prompt().user(prompt).call().content();
            
            if (category != null && category.trim().toUpperCase().contains("B")) {
                log.info("【Router】路由决定: AFTER_SALES (售后)");
                return afterSalesChatClient;
            } else {
                log.info("【Router】路由决定: CUSTOMER_SERVICE (售前)");
                return customerServiceChatClient;
            }
        } catch (Exception e) {
            log.warn("【Router】意图分类失败，降级使用售前客服: {}", e.getMessage());
            return customerServiceChatClient;
        }
    }
}
