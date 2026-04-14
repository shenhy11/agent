package com.agent.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agent.tools.CustomerServiceTools;
import com.agent.tools.ProductTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工具执行服务
 * 封装了项目中已有的工具供 ReAct Engine 统一调度
 */
@Service
public class ToolExecutionService {
    public ToolExecutionService(ProductTools productTools, CustomerServiceTools customerServiceTools) {
        this.productTools = productTools;
        this.customerServiceTools = customerServiceTools;
    }

    private static final Logger log = LoggerFactory.getLogger(ToolExecutionService.class);


    private final ProductTools productTools;
    private final CustomerServiceTools customerServiceTools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getToolsDescription() {
        return """
                - queryProductInfo: 查询产品型号的价格和简要说明
                - compareProducts: 对比两款手表的设计和功能差异
                - findStore: 查找指定城市的线下门店地址
                - queryOrderTracking: 查询订单的发货和物流状态
                """;
    }

    public String getToolNames() {
        return "queryProductInfo, compareProducts, findStore, queryOrderTracking";
    }

    /**
     * 根据工具名和 JSON 参数动态调用对应的方法
     * (使用反射/硬编码适配即可，这里为了简单安全使用硬编码 switch)
     */
    public String execute(String toolName, String jsonInput) {
        log.info("执行工具: {}, param: {}", toolName, jsonInput);
        try {
            JsonNode params = objectMapper.readTree(jsonInput);
            
            if ("queryProduct".equals(toolName) || "queryProductInfo".equals(toolName)) {
                JsonNode modelNode = params.get("model");
                if (modelNode == null) modelNode = params.get("productId");
                return productTools.queryProduct(modelNode != null ? modelNode.asText() : "");
            }

            if ("compareProducts".equals(toolName)) {
                JsonNode modelANode = params.get("modelA");
                JsonNode modelBNode = params.get("modelB");
                String productIds = (modelANode != null ? modelANode.asText() : "") + "," + (modelBNode != null ? modelBNode.asText() : "");
                return productTools.compareProducts(productIds);
            }

            return switch (toolName) {
                case "queryOrderStatus", "queryOrderTracking", "findStore" -> {
                    String orderId = params.has("orderId") ? params.get("orderId").asText() : "";
                    yield customerServiceTools.queryOrderStatus(orderId);
                }
                case "createTicket" -> {
                    String userId = params.has("userId") ? params.get("userId").asText() : "";
                    String title = params.has("title") ? params.get("title").asText() : "";
                    String desc = params.has("description") ? params.get("description").asText() : "";
                    String priority = params.has("priority") ? params.get("priority").asText() : "low";
                    yield customerServiceTools.createTicket(userId, title, desc, priority);
                }
                case "getCurrentTime" -> {
                    yield customerServiceTools.getCurrentTime();
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
        } catch (Exception e) {
            throw new RuntimeException("参数解析或执行失败: " + e.getMessage(), e);
        }
    }
}
