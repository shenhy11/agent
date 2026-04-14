# ChronoTech AI Lab 垂直领域大模型微调指南

为了让大语言模型更符合 ChronoTech 智能手表的品牌调性和业务需求，我们通过 LoRA (Low-Rank Adaptation) 技术进行轻量级指令微调。

## 1. 数据准备

运行生成脚本：
```bash
python generate_sft.py
```
这会生成 `chronotech_sft.json`，包含了针对产品查询、对比、售后等特定意图的指令数据对。

## 2. 微调框架推荐

使用 [LLaMA-Factory](https://github.com/hiyouga/LLaMA-Factory) 是目前最高效的开源微调方案。

## 3. LoRA 超参数配置参考

- **Base Model**: `qwen/Qwen1.5-7B-Chat` 或 `qwen/Qwen2-7B-Instruct`
- **Method**: LoRA
- **LoRA Rank (r)**: 8
- **LoRA Alpha**: 32
- **Learning Rate**: 2e-4
- **Epochs**: 3
- **Batch Size**: 4
- **Max Source Length**: 1024

## 4. 部署与接入

微调完成后：
1. 将经过 LoRA 微调的模型导出（或动态加载 Adapter）。
2. 使用 vLLM 或 Ollama 在带有 GPU 的环境部署为兼容 OpenAI 协议的 API。
3. 在 `application.yml` 中配置微调后模型的专属 BASE_URL 和 API_KEY，或配置一个专用的 `FinetunedChatClient` 指向该 API 进行对比。
