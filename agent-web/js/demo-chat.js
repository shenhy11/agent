/**
 * 智能客服 Demo 交互逻辑
 * SSE 流式对话 + Agent 思考链渲染
 */
(function() {
  const API_BASE = '';
  const chatMessages = document.getElementById('chatMessages');
  const chatInput = document.getElementById('chatInput');
  const chatSendBtn = document.getElementById('chatSendBtn');
  const thinkingSteps = document.getElementById('thinkingSteps');
  const clearChatBtn = document.getElementById('clearChat');

  // 会话 ID
  let conversationId = 'demo-' + Date.now();
  let stepCount = 0;

  // 预置问题按钮
  document.querySelectorAll('.preset-btn[data-question]').forEach(btn => {
    btn.addEventListener('click', () => {
      chatInput.value = btn.dataset.question;
      sendMessage();
    });
  });

  // 发送按钮
  chatSendBtn.addEventListener('click', sendMessage);
  chatInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  });

  // 清空对话
  clearChatBtn.addEventListener('click', () => {
    chatMessages.innerHTML = `
      <div class="chat-msg chat-msg-ai">
        <div class="chat-msg-avatar"><i class="fa-solid fa-robot"></i></div>
        <div class="chat-msg-content">对话已清空。有什么可以帮您的？</div>
      </div>`;
    thinkingSteps.innerHTML = `
      <div style="color:var(--text-muted);font-size:0.85rem;text-align:center;padding:2rem 0;">
        <i class="fa-solid fa-circle-info" style="font-size:1.5rem;margin-bottom:0.5rem;display:block;"></i>
        发送消息后，这里会实时展示 Agent 的推理过程。
      </div>`;
    conversationId = 'demo-' + Date.now();
    stepCount = 0;
  });

  /**
   * 发送消息
   */
  function sendMessage() {
    const text = chatInput.value.trim();
    if (!text) return;

    // 显示用户消息
    appendMessage('user', text);
    chatInput.value = '';
    chatSendBtn.disabled = true;

    // 清空思考链，准备新一轮
    thinkingSteps.innerHTML = '';
    stepCount = 0;
    addThinkingStep('thinking', '正在分析您的问题...');

    // 创建 AI 回复占位
    const aiMsgEl = appendMessage('ai', '');
    const contentEl = aiMsgEl.querySelector('.chat-msg-content');

    // 添加加载动画
    contentEl.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div>';

    // SSE 流式请求
    streamChat(text, contentEl);
  }

  /**
   * SSE 流式对话
   */
  function streamChat(message, contentEl) {
    const url = `${API_BASE}/api/v1/agent/multi?message=${encodeURIComponent(message)}`;
    
    let fullText = '';
    let isFirstChunk = true;

    fetch(url, { method: 'GET' })
      .then(response => {
        if (!response.ok) throw new Error('请求失败: ' + response.status);
        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              chatSendBtn.disabled = false;
              addThinkingStep('complete', '回答生成完成');
              return;
            }

            const chunk = decoder.decode(value, { stream: true });
            // 解析 SSE 数据
            const lines = chunk.split('\n');
            for (const line of lines) {
              if (line.startsWith('data:')) {
                const data = line.substring(5).trim();
                if (data && data !== '[DONE]') {
                  if (isFirstChunk) {
                    contentEl.innerHTML = '';
                    isFirstChunk = false;
                    addThinkingStep('text', '模型开始生成回答...');
                  }
                  fullText += data;
                  contentEl.textContent = fullText;
                  chatMessages.scrollTop = chatMessages.scrollHeight;
                }
              }
            }

            read();
          });
        }

        read();
      })
      .catch(err => {
        console.error('SSE 错误:', err);
        contentEl.innerHTML = `<span style="color:#f87171;">⚠️ 连接失败: ${err.message}。请刷新页面重试。</span>`;
        chatSendBtn.disabled = false;
        addThinkingStep('error', '请求异常: ' + err.message);
      });
  }

  /**
   * 添加消息气泡
   */
  function appendMessage(type, text) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `chat-msg chat-msg-${type}`;
    
    const icon = type === 'user' ? 'fa-user' : 'fa-robot';
    msgDiv.innerHTML = `
      <div class="chat-msg-avatar"><i class="fa-solid ${icon}"></i></div>
      <div class="chat-msg-content">${text}</div>
    `;
    
    chatMessages.appendChild(msgDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    return msgDiv;
  }

  /**
   * 添加思考链步骤
   */
  function addThinkingStep(type, content) {
    stepCount++;
    const stepEl = document.createElement('div');
    stepEl.style.cssText = 'display:flex;gap:10px;align-items:flex-start;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.04);animation:msgIn 0.3s ease;';

    const icons = {
      thinking: { icon: 'fa-brain', color: '#a78bfa' },
      tool_call: { icon: 'fa-wrench', color: '#60a5fa' },
      tool_result: { icon: 'fa-check-circle', color: '#34d399' },
      text: { icon: 'fa-pen', color: 'var(--accent-cyan)' },
      complete: { icon: 'fa-flag-checkered', color: '#34d399' },
      error: { icon: 'fa-exclamation-triangle', color: '#f87171' }
    };

    const { icon, color } = icons[type] || icons.thinking;

    stepEl.innerHTML = `
      <div style="width:28px;height:28px;border-radius:8px;background:rgba(255,255,255,0.05);display:flex;align-items:center;justify-content:center;flex-shrink:0;font-size:0.75rem;color:${color};">
        <i class="fa-solid ${icon}"></i>
      </div>
      <div style="flex:1;">
        <div style="font-size:0.75rem;color:var(--text-muted);margin-bottom:2px;">步骤 ${stepCount}</div>
        <div style="font-size:0.82rem;color:var(--text-secondary);line-height:1.5;">${content}</div>
      </div>
    `;

    thinkingSteps.appendChild(stepEl);
    thinkingSteps.scrollTop = thinkingSteps.scrollHeight;
  }
})();
