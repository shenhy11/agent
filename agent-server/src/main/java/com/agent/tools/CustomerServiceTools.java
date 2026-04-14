package com.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 客服常用工具集
 * 通过 @Tool 注解声明，Agent 可在对话中动态调用
 */
@Component
public class CustomerServiceTools {

    /**
     * 查询订单状态（模拟）
     */
    @Tool(description = "根据订单号查询订单的当前状态，包括物流信息")
    public String queryOrderStatus(
            @ToolParam(description = "订单号，格式如 ORD-20260101-001") String orderId) {
        // TODO: 对接实际的订单系统
        return String.format("订单 %s 状态：已发货，预计 3 天内送达。物流单号：SF1234567890", orderId);
    }

    /**
     * 创建工单
     */
    @Tool(description = "为用户创建客服工单，适用于需要人工处理的复杂问题")
    public String createTicket(
            @ToolParam(description = "用户 ID") String userId,
            @ToolParam(description = "工单标题，简要描述问题") String title,
            @ToolParam(description = "工单详细描述") String description,
            @ToolParam(description = "优先级：low/medium/high") String priority) {
        // TODO: 对接实际的工单系统
        String ticketId = "TK-" + System.currentTimeMillis();
        return String.format("工单已创建成功！工单号：%s，优先级：%s，预计 24 小时内处理。", ticketId, priority);
    }

    /**
     * 获取当前时间
     */
    @Tool(description = "获取当前系统时间")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
