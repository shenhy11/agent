/**
 * ChronoTech 智能客服组件的交互逻辑与 SSE API 调用
 */
document.addEventListener('DOMContentLoaded', () => {
    // ==== 1. HTML 结构注入 ====
    const container = document.getElementById('ai-chat-container');
    if (!container) return;

    container.innerHTML = `
        <!-- 面板 -->
        <div class="chat-panel" id="chatPanel">
            <div class="chat-header">
                <div class="chat-title">
                    <i class="fa-solid fa-robot"></i> ChronoTech 智能管家
                </div>
                <button class="chat-close" id="chatCloseBtn"><i class="fa-solid fa-xmark"></i></button>
            </div>
            <div class="chat-messages" id="chatMessages">
                <!-- 预设欢迎语 -->
                <div class="message ai">
                    你好！我是 ChronoTech 专属智能助理，请问有什么可以帮助您的吗？
                    <div class="quick-actions">
                        <button class="quick-btn">了解新品</button>
                        <button class="quick-btn">订单查询</button>
                        <button class="quick-btn">售后服务</button>
                    </div>
                </div>
            </div>
            <div class="chat-input-area">
                <input type="text" id="chatInput" class="chat-input" placeholder="输入消息..." autocomplete="off">
                <button id="chatSendBtn" class="chat-send"><i class="fa-solid fa-paper-plane"></i></button>
            </div>
        </div>

        <!-- 悬浮按钮 -->
        <div class="chat-launcher" id="chatLauncherBtn">
            <i class="fa-solid fa-comment-dots"></i>
            <span class="chat-badge" id="chatBadge">1</span>
        </div>
    `;

    // ==== 2. UI 交互控制 ====
    const launcherBtn = document.getElementById('chatLauncherBtn');
    const panel = document.getElementById('chatPanel');
    const closeBtn = document.getElementById('chatCloseBtn');
    const badge = document.getElementById('chatBadge');
    
    const messagesEl = document.getElementById('chatMessages');
    const inputEl = document.getElementById('chatInput');
    const sendBtn = document.getElementById('chatSendBtn');

    // 生成唯一的 session ID，模拟后端对话上下文
    let conversationId = "web-" + Math.random().toString(36).substring(2, 9);
    let isWaitingForResponse = false; // 是否在等待流式响应中

    // 打开/关闭面板
    const togglePanel = () => {
        const isActive = panel.classList.contains('active');
        if (isActive) {
            panel.classList.remove('active');
        } else {
            panel.classList.add('active');
            badge.style.display = 'none'; // 清除未读红点
            inputEl.focus();
        }
    };

    launcherBtn.addEventListener('click', togglePanel);
    closeBtn.addEventListener('click', () => panel.classList.remove('active'));

    // 快捷按钮点击
    document.body.addEventListener('click', (e) => {
        if (e.target.classList.contains('quick-btn')) {
            inputEl.value = e.target.innerText;
            handleSend();
        }
    });

    // 监听回车发送
    inputEl.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleSend();
    });
    sendBtn.addEventListener('click', handleSend);

    // 回滚滚动条到最底
    const scrollToBottom = () => {
        // 使用 setTimeout 保证 DOM 已更新
        setTimeout(() => {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }, 50);
    };

    // 渲染自己的消息
    const appendUserMessage = (text) => {
        const div = document.createElement('div');
        div.className = 'message user';
        div.innerText = text;
        messagesEl.appendChild(div);
        scrollToBottom();
    };

    // ==== 3. 核心 API 通信 (SSE 发送逻辑) ====
    async function handleSend() {
        const text = inputEl.value.trim();
        if (!text || isWaitingForResponse) return;

        // 1. 发送展示
        appendUserMessage(text);
        inputEl.value = '';
        isWaitingForResponse = true;
        sendBtn.disabled = true;

        // 2. 创建一个空的 AI 提示框 + 打字机动画
        const aiMessageDiv = document.createElement('div');
        aiMessageDiv.className = 'message ai';
        aiMessageDiv.innerHTML = '<div class="typing-indicator"><span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span></div>';
        messagesEl.appendChild(aiMessageDiv);
        scrollToBottom();

        // 3. 准备要请求的 Spring Boot 后端 SSE 接口 (按需将 localhost 替换)
        const apiUrl = '/api/chat/stream';

        try {
            // 注意: 原生的 EventSource 只能发送 GET 请求。
            // 我们的接口是 POST 请求，所以这里需要用 fetch 直接消费 ReadableStream

            const response = await fetch(apiUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream'
                },
                body: JSON.stringify({
                    message: text,
                    conversationId: conversationId
                })
            });

            if (!response.ok) {
                throw new Error("API 请求失败 " + response.status);
            }

            // 清除打字机动画，准备拼接文字
            aiMessageDiv.innerHTML = '';
            
            // 4. 解析流 
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let aiText = '';

            // 循环读取每一截 Stream
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                const chunk = decoder.decode(value, { stream: true });
                
                /*
                 * SSE 数据块格式通常是:
                 * data: 你
                 * \n\n
                 * 这里简单解析把 'data: ' 取出即可
                 */
                const lines = chunk.split('\n');
                for (let line of lines) {
                    if (line.startsWith('data:')) {
                        let token = line.replace('data:', '').trimStart();
                        // 特殊标记直接忽略
                        if(token === '[DONE]') continue; 
                        
                        // 由于后端流式返回可能是单个字，也可能是带逗号格式，为了适配最简单写法:
                        // 这里我们将其作为纯文本追加
                        aiText += token;
                        // 因为 Spring AI 打出来的可能是转义字符，简单的应用 innerText 会对 HTML 转义
                        aiMessageDiv.innerText = aiText;
                        scrollToBottom();
                    }
                }
            }

        } catch (error) {
            console.error("Chat Error:", error);
            aiMessageDiv.innerText = "抱歉，系统通讯异常，请稍后再试。";
            aiMessageDiv.style.color = 'var(--color-error)';
        } finally {
            isWaitingForResponse = false;
            sendBtn.disabled = false;
            inputEl.focus();
            scrollToBottom();
        }
    }
});
