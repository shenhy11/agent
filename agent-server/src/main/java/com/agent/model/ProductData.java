package com.agent.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 产品数据仓库（硬编码）
 * 存储 ChronoTech 6 款手表的完整规格信息
 */
public class ProductData {

    /** 全部产品列表 */
    private static final List<Map<String, Object>> PRODUCTS = List.of(
            buildProduct("sport-x2-pro", "Sport X2 Pro", "sport",
                    "旗舰运动，无惧挑战", 1999,
                    "images/sport-x2-pro.jpg", "热销推荐",
                    Map.of(
                            "battery", "30天超长续航",
                            "waterproof", "5ATM (50m)",
                            "material", "不锈钢+树脂",
                            "display", "1.43\" AMOLED 466×466",
                            "sensors", "心率/血氧/加速度/陀螺仪/气压计",
                            "gps", "GPS+北斗双频定位",
                            "nfc", false,
                            "weight", "52g",
                            "sportModes", "150+运动模式",
                            "highlight", "专业运动算法，VO2max评估，训练负荷分析"
                    )),
            buildProduct("sport-x1-lite", "Sport X1 Lite", "sport",
                    "轻量入门，活力无限", 999,
                    "images/sport-x1-lite.jpg", null,
                    Map.of(
                            "battery", "14天续航",
                            "waterproof", "5ATM (50m)",
                            "material", "聚碳酸酯+硅胶",
                            "display", "1.39\" AMOLED 454×454",
                            "sensors", "心率/血氧/加速度",
                            "gps", "GPS单频定位",
                            "nfc", false,
                            "weight", "38g",
                            "sportModes", "100+运动模式",
                            "highlight", "超轻机身，全天候心率监测"
                    )),
            buildProduct("elite-e2-ultra", "Elite E2 Ultra", "business",
                    "钛合金精密锻造，商务首选", 3999,
                    "images/elite-e2-ultra.jpg", "旗舰新品",
                    Map.of(
                            "battery", "21天续航",
                            "waterproof", "10ATM (100m)",
                            "material", "钛合金+蓝宝石玻璃",
                            "display", "1.5\" LTPO AMOLED 480×480",
                            "sensors", "心率/血氧/体温/心电图/加速度/陀螺仪",
                            "gps", "GPS+北斗+GLONASS三频定位",
                            "nfc", true,
                            "weight", "62g",
                            "sportModes", "100+运动模式",
                            "highlight", "蓝宝石镜面，NFC支付，ECG心电监测，支持eSIM独立通话"
                    )),
            buildProduct("elite-e1-classic", "Elite E1 Classic", "business",
                    "经典商务，低调奢华", 2499,
                    "images/elite-e1-classic.jpg", null,
                    Map.of(
                            "battery", "18天续航",
                            "waterproof", "5ATM (50m)",
                            "material", "不锈钢+蓝宝石玻璃",
                            "display", "1.43\" AMOLED 466×466",
                            "sensors", "心率/血氧/体温/加速度",
                            "gps", "GPS+北斗双频定位",
                            "nfc", true,
                            "weight", "56g",
                            "sportModes", "80+运动模式",
                            "highlight", "商务表盘，日程提醒，NFC支付，蓝宝石镜面"
                    )),
            buildProduct("rugged-r2-titan", "Rugged R2 Titan", "outdoor",
                    "极限环境，强悍生存", 2999,
                    "images/rugged-r2-titan.jpg", null,
                    Map.of(
                            "battery", "25天续航（GPS模式60小时）",
                            "waterproof", "10ATM (100m)",
                            "material", "钛合金+蓝宝石玻璃",
                            "display", "1.47\" MIP 常显屏 + AMOLED 双屏",
                            "sensors", "心率/血氧/气压/高度/指南针/温度",
                            "gps", "GPS+北斗+GLONASS+Galileo全频段",
                            "nfc", true,
                            "weight", "78g",
                            "sportModes", "170+运动模式（含登山/滑雪/潜水专项）",
                            "highlight", "MIL-STD-810H军规认证，离线地图，气压风暴预警"
                    )),
            buildProduct("rugged-r1-explorer", "Rugged R1 Explorer", "outdoor",
                    "户外探索，轻装上阵", 1799,
                    "images/rugged-r1-explorer.jpg", null,
                    Map.of(
                            "battery", "20天续航（GPS模式40小时）",
                            "waterproof", "5ATM (50m)",
                            "material", "高强度聚合物+康宁大猩猩玻璃",
                            "display", "1.43\" AMOLED 466×466",
                            "sensors", "心率/血氧/气压/高度/指南针",
                            "gps", "GPS+北斗双频定位",
                            "nfc", false,
                            "weight", "65g",
                            "sportModes", "120+运动模式（含越野跑/骑行专项）",
                            "highlight", "轨迹返航，日出日落提醒，恶劣天气警报"
                    ))
    );

    /**
     * 构建单个产品数据
     */
    private static Map<String, Object> buildProduct(
            String id, String name, String series,
            String subtitle, int price,
            String imageUrl, String badge,
            Map<String, Object> specs) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("id", id);
        product.put("name", name);
        product.put("series", series);
        product.put("subtitle", subtitle);
        product.put("price", price);
        product.put("imageUrl", imageUrl);
        if (badge != null) {
            product.put("badge", badge);
        }
        product.put("specs", specs);
        return product;
    }

    /**
     * 获取全部产品
     */
    public static List<Map<String, Object>> getAll() {
        return PRODUCTS;
    }

    /**
     * 按系列筛选产品
     */
    public static List<Map<String, Object>> getBySeries(String series) {
        return PRODUCTS.stream()
                .filter(p -> series.equalsIgnoreCase((String) p.get("series")))
                .collect(Collectors.toList());
    }

    /**
     * 按 ID 查询单个产品
     */
    public static Optional<Map<String, Object>> getById(String id) {
        return PRODUCTS.stream()
                .filter(p -> id.equals(p.get("id")))
                .findFirst();
    }

    /**
     * 按 ID 列表批量查询（用于对比）
     */
    public static List<Map<String, Object>> getByIds(List<String> ids) {
        return PRODUCTS.stream()
                .filter(p -> ids.contains(p.get("id")))
                .collect(Collectors.toList());
    }
}
