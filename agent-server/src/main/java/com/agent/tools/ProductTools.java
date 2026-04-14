package com.agent.tools;

import com.agent.model.ProductData;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 产品相关工具集
 * Agent 可在对话中动态调用以获取产品信息
 */
@Component
public class ProductTools {

    /**
     * 查询单个产品的详细规格
     */
    @Tool(description = "根据产品ID查询ChronoTech手表的详细规格信息，包括价格、续航、防水、材质、传感器、GPS等参数")
    public String queryProduct(
            @ToolParam(description = "产品ID，如 sport-x2-pro、elite-e2-ultra、rugged-r2-titan 等") String productId) {
        return ProductData.getById(productId)
                .map(p -> formatProduct(p))
                .orElse("未找到产品: " + productId + "。可用的产品ID有: sport-x2-pro, sport-x1-lite, elite-e2-ultra, elite-e1-classic, rugged-r2-titan, rugged-r1-explorer");
    }

    /**
     * 对比多款产品的规格差异
     */
    @Tool(description = "对比两款或多款ChronoTech手表的规格差异，输入逗号分隔的产品ID列表")
    public String compareProducts(
            @ToolParam(description = "逗号分隔的产品ID列表，如 sport-x2-pro,elite-e2-ultra") String productIds) {
        String[] ids = productIds.split(",");
        List<Map<String, Object>> products = ProductData.getByIds(List.of(ids));

        if (products.size() < 2) {
            return "需要至少2个有效产品ID进行对比。可用的ID: sport-x2-pro, sport-x1-lite, elite-e2-ultra, elite-e1-classic, rugged-r2-titan, rugged-r1-explorer";
        }

        StringBuilder sb = new StringBuilder("## 产品对比\n\n");
        for (Map<String, Object> p : products) {
            sb.append(formatProduct(p)).append("\n---\n");
        }
        return sb.toString();
    }

    /**
     * 根据用户需求推荐最适合的产品
     */
    @Tool(description = "根据用户描述的使用场景和需求，从ChronoTech全系列中推荐最适合的手表。请传入用户的需求描述")
    public String recommendProduct(
            @ToolParam(description = "用户的需求描述，如'经常跑步和游泳'、'商务场景需要NFC支付'、'户外登山探险'") String requirement) {
        // 返回全部产品让 LLM 自己判断推荐
        List<Map<String, Object>> all = ProductData.getAll();
        StringBuilder sb = new StringBuilder("以下是ChronoTech全部产品，请根据用户需求\"" + requirement + "\"推荐最合适的：\n\n");
        for (Map<String, Object> p : all) {
            sb.append(formatProductBrief(p)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 查询产品当前优惠活动
     */
    @Tool(description = "查询ChronoTech当前的促销优惠活动信息")
    public String getPromotions() {
        return """
                ## 当前优惠活动
                
                🔥 **春季焕新季** (即日起至4月30日)
                - Sport X2 Pro: 直降 ¥200，到手 ¥1,799
                - Elite E2 Ultra: 赠送价值 ¥299 的真皮表带
                - Rugged R2 Titan: 购机享 12 期免息分期
                
                🎁 **以旧换新加码**
                - 旧款 ChronoTech 手表最高抵扣新品价格的 35%（限时加码 5%）
                
                📦 **全系列购机权益**
                - 免费刻字服务
                - 延长保修至 3 年（注册会员专享）
                - 首单赠 30 天退货保障
                """;
    }

    /**
     * 格式化产品详细信息
     */
    @SuppressWarnings("unchecked")
    private String formatProduct(Map<String, Object> product) {
        Map<String, Object> specs = (Map<String, Object>) product.get("specs");
        return String.format("""
                ### %s (%s系列)
                - 副标题: %s
                - 价格: ¥%s
                - 续航: %s
                - 防水: %s
                - 材质: %s
                - 屏幕: %s
                - 传感器: %s
                - 定位: %s
                - NFC: %s
                - 重量: %s
                - 运动模式: %s
                - 亮点: %s
                """,
                product.get("name"), product.get("series"),
                product.get("subtitle"), product.get("price"),
                specs.get("battery"), specs.get("waterproof"),
                specs.get("material"), specs.get("display"),
                specs.get("sensors"), specs.get("gps"),
                Boolean.TRUE.equals(specs.get("nfc")) ? "支持" : "不支持",
                specs.get("weight"), specs.get("sportModes"),
                specs.get("highlight"));
    }

    /**
     * 格式化产品简要信息（用于推荐）
     */
    private String formatProductBrief(Map<String, Object> product) {
        return String.format("- **%s** (%s系列) ¥%s — %s",
                product.get("name"), product.get("series"),
                product.get("price"), product.get("subtitle"));
    }
}
