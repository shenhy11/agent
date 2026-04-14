# 📡 API 接口设计文档 — ChronoTech 官网

> **版本**: v1.0
> **日期**: 2026-03-26
> **Base URL**: `http://localhost:8080`
> **Content-Type**: `application/json`

---

## 1. 产品 API

### 1.1 获取产品列表

```
GET /api/products
GET /api/products?category={category}
```

**Query Parameters**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 产品系列: sport / elite / rugged |

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "sport-x1",
      "name": "ChronoTech Sport X1",
      "nameZh": "运动系列 X1",
      "category": "sport",
      "tagline": "Professional Sports Smartwatch",
      "taglineZh": "专业运动智能手表",
      "price": 1299,
      "image": "/assets/images/products/sport-x1.png",
      "features": ["GPS+Beidou", "5ATM", "Heart Rate"],
      "isNew": false,
      "isFeatured": true
    }
  ]
}
```

### 1.2 获取产品详情

```
GET /api/products/{id}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "elite-e2-ultra",
    "name": "ChronoTech Elite E2 Ultra",
    "nameZh": "商务旗舰 E2 Ultra",
    "category": "elite",
    "tagline": "Ultimate Business Flagship",
    "taglineZh": "顶级商务旗舰",
    "description": "Crafted with premium titanium...",
    "descriptionZh": "采用钛合金精工打造...",
    "price": 3999,
    "images": [
      "/assets/images/products/elite-e2-ultra-front.png",
      "/assets/images/products/elite-e2-ultra-side.png",
      "/assets/images/products/elite-e2-ultra-back.png"
    ],
    "colors": [
      { "name": "Midnight Black", "nameZh": "午夜黑", "hex": "#1a1a2e" },
      { "name": "Silver", "nameZh": "星光银", "hex": "#c0c0c0" },
      { "name": "Rose Gold", "nameZh": "玫瑰金", "hex": "#b76e79" }
    ],
    "features": [
      { "icon": "display", "title": "1.52\" AMOLED", "titleZh": "1.52寸 AMOLED 屏幕" },
      { "icon": "battery", "title": "30-Day Battery", "titleZh": "30天超长续航" },
      { "icon": "water", "title": "10ATM Waterproof", "titleZh": "10ATM 防水" },
      { "icon": "heart", "title": "Blood Oxygen + Heart Rate", "titleZh": "血氧+心率监测" },
      { "icon": "location", "title": "GPS+Beidou+GLONASS", "titleZh": "GPS+北斗+GLONASS" },
      { "icon": "bluetooth", "title": "Bluetooth 5.3 NFC", "titleZh": "蓝牙5.3 + NFC" }
    ],
    "specs": {
      "display": "1.52\" AMOLED, 466×466px",
      "battery": "580mAh, 30 days typical",
      "waterproof": "10ATM (100m)",
      "sensors": "Heart Rate, SpO2, Barometer, Compass, Accelerometer",
      "connectivity": "Bluetooth 5.3, NFC",
      "navigation": "GPS, Beidou, GLONASS, Galileo",
      "os": "ChronoOS 3.0",
      "weight": "52g (without strap)",
      "material": "Titanium Alloy + Sapphire Glass"
    },
    "buyLinks": {
      "jd": "https://jd.com/chronotech-elite-e2-ultra",
      "tmall": "https://tmall.com/chronotech-elite-e2-ultra"
    }
  }
}
```

---

## 2. 对话 API（已有）

### 2.1 同步对话

```
POST /api/chat
```

**Request Body**:
```json
{
  "message": "你好，请推荐一款适合跑步的手表",
  "conversationId": "abc123"
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "abc123",
    "content": "您好！推荐您看看 ChronoTech Sport X2 Pro...",
    "type": "text"
  }
}
```

### 2.2 流式对话 (SSE)

```
POST /api/chat/stream
Content-Type: application/json
Accept: text/event-stream
```

**Request Body**: 同 2.1

**Response**: Server-Sent Events 流
```
data: 您
data: 好
data: ！
data: 推荐
data: 您
...
```

---

## 3. 联系表单 API（新增）

```
POST /api/contact
```

**Request Body**:
```json
{
  "name": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "subject": "product_inquiry",
  "message": "我想了解 Elite E2 Ultra 的企业采购优惠"
}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ticketId": "CT-20260326-001",
    "status": "received"
  }
}
```

**subject 枚举值**:
| 值 | 说明 |
|---|------|
| product_inquiry | 产品咨询 |
| business_cooperation | 商务合作 |
| after_sales | 售后服务 |
| other | 其他 |

---

## 4. 新闻 API（新增）

### 4.1 获取新闻列表

```
GET /api/news
GET /api/news?category={category}&page={page}&size={size}
```

**Response 200**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": "news-001",
        "title": "ChronoTech Launches Elite E2 Ultra at CES 2026",
        "titleZh": "ChronoTech 在 CES 2026 发布 Elite E2 Ultra",
        "category": "product",
        "date": "2026-03-20",
        "excerpt": "The latest flagship smartwatch...",
        "excerptZh": "最新旗舰智能手表...",
        "image": "/assets/images/news/ces-2026.png",
        "isFeatured": true
      }
    ],
    "total": 12,
    "page": 1,
    "size": 6
  }
}
```

---

## 5. 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "参数错误: name 不能为空",
  "data": null
}
```
