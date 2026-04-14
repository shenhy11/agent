import json
import random

# 生成基于 ChronoTech 手表客服场景的 SFT (Supervised Fine-Tuning) 数据集
# 格式按 LLaMA-Factory 所需格式

intents = [
    ("查询价格", "Sport X2 Pro现在多少钱？", "ChronoTech Sport X2 Pro 的官方指导价为 2499 元。目前我们有折旧换新活动，最高可抵扣 500 元。"),
    ("对比产品", "Elite E2和Classic C1选哪个好？", "这取决于您的使用场景：如果您喜欢商务或正式场合佩戴，Classic C1 的机械美学和真皮表带非常适合您；如果您关注健康监测并在意佩戴舒适度，Elite E2 的钛合金机身更轻便，且拥有 ECG 心电图功能。"),
    ("询问售后", "屏幕摔碎了能保修吗？", "您好，屏幕摔碎属于人为意外损坏，不在免费保修范围内。如果您的设备还在保修期内，我们可以为您提供优惠维修服务。更换屏幕的起步价大约是 399 元，具体请以工程师检测为准。"),
    ("询问续航", "这款表能用几天？", "ChronoTech 全系手表均搭载了智能省电算法。在开启全天心率和睡眠监测的日常模式下，续航可达 7-10 天。在极限省电模式下，最高可达 21 天。"),
]

def generate_dataset(num_samples=50):
    dataset = []
    for i in range(num_samples):
        intent, user_q, bot_a = random.choice(intents)
        # 添加一些随机句式变化
        greetings = ["您好，", "客服小C为您解答：", ""]
        bot_a = random.choice(greetings) + bot_a
        
        sample = {
            "instruction": "你是一个名为 ChronoTech AI 的智能手表专属客服。请专业、礼貌地回答用户的提问。",
            "input": user_q,
            "output": bot_a
        }
        dataset.append(sample)
    return dataset

if __name__ == "__main__":
    ds = generate_dataset(500)
    with open("chronotech_sft.json", "w", encoding="utf-8") as f:
        json.dump(ds, f, ensure_ascii=False, indent=2)
    print(f"Generated {len(ds)} SFT samples in chronotech_sft.json")
