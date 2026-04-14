/**
 * Agent 工作流 Demo 交互逻辑
 * SSE 事件流解析 + 步骤节点动态渲染
 */
(function() {
  const API_BASE = 'http://localhost:8080';
  const agentInput = document.getElementById('agentInput');
  const agentSendBtn = document.getElementById('agentSendBtn');
  const agentResult = document.getElementById('agentResult');
  const agentFlow = document.getElementById('agentFlow');
  const agentDuration = document.getElementById('agentDuration');

  let conversationId = 'agent-' + Date.now();
  let stepIndex = 0;

  // 预置任务
  document.querySelectorAll('.preset-btn[data-question]').forEach(btn => {
    btn.addEventListener('click', () => {
      agentInput.value = btn.dataset.question;
      executeTask();
    });
  });

  agentSendBtn.addEventListener('click', executeTask);
  agentInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); executeTask(); }
  });

  function executeTask() {
    const task = agentInput.value.trim();
    if (!task) return;

    agentSendBtn.disabled = true;
    agentSendBtn.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div>';
    stepIndex = 0;
    agentFlow.innerHTML = '';
    agentResult.innerHTML = '<div style="text-align:center;padding:2rem 0;"><div class="loading-dots"><span></span><span></span><span></span></div><p style="color:var(--text-muted);margin-top:1rem;font-size:0.85rem;">Agent 正在执行任务...</p></div>';

    // 添加"任务接收"步骤
    addFlowNode('start', '任务接收', task.substring(0, 60) + (task.length > 60 ? '...' : ''));

    const startTime = Date.now();

    // 真实 SSE 请求连接 ReAct 引擎
    const url = `${API_BASE}/api/agent/react?message=${encodeURIComponent(task)}`;
    let fullText = '';
    let isFirst = true;

    fetch(url)
      .then(response => {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              agentSendBtn.disabled = false;
              agentSendBtn.innerHTML = '<i class="fa-solid fa-rocket"></i> 执行';
              const duration = Date.now() - startTime;
              agentDuration.textContent = `总耗时 ${(duration/1000).toFixed(1)}s`;
              addFlowNode('complete', '任务完成', `总耗时 ${(duration/1000).toFixed(1)}s`);
              return;
            }

            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');
            let currentEvent = 'message';
            
            for (let i = 0; i < lines.length; i++) {
              const line = lines[i];
              if (line.startsWith('event:')) {
                currentEvent = line.substring(6).trim();
              } else if (line.startsWith('data:')) {
                const data = line.substring(5).trim();
                
                if (data === '[DONE]') continue;
                if (!data) continue;
                
                if (currentEvent === 'thinking') {
                  const t = JSON.parse(data);
                  addFlowNode('thinking', '思考规划', t.thought);
                } else if (currentEvent === 'tool_call') {
                  const t = JSON.parse(data);
                  addFlowNode('tool_call', '工具调用: ' + t.tool, `参数: ${t.input}`);
                } else if (currentEvent === 'tool_result') {
                  const t = JSON.parse(data);
                  addFlowNode('tool_result', '返回结果', `耗时: ${t.durationMs}ms`);
                } else if (currentEvent === 'error') {
                  addFlowNode('error', '异常报错', data);
                } else if (currentEvent === 'message') {
                  if (isFirst) {
                    agentResult.innerHTML = '';
                    isFirst = false;
                    addFlowNode('text', '生成回答', '整理最终输出内容...');
                  }
                  fullText += data;
                  agentResult.innerHTML = `<div style="line-height:1.8;font-size:0.9rem;white-space:pre-wrap;">${fullText}</div>`;
                  agentResult.scrollTop = agentResult.scrollHeight;
                }
              }
            }
            read();
          });
        }
        read();
      })
      .catch(err => {
        agentResult.innerHTML = `<p style="color:#f87171;">⚠️ 执行失败: ${err.message}<br><span style="font-size:0.8rem;">请确保后端服务在 localhost:8080 运行</span></p>`;
        agentSendBtn.disabled = false;
        agentSendBtn.innerHTML = '<i class="fa-solid fa-rocket"></i> 执行';
        addFlowNode('error', '执行异常', err.message);
      });
  }

  /**
   * 添加流程节点
   */
  function addFlowNode(type, title, detail) {
    stepIndex++;
    const node = document.createElement('div');
    node.style.cssText = 'display:flex;gap:12px;animation:msgIn 0.3s ease;margin-bottom:0;';

    const typeConfig = {
      start: { icon: 'fa-play', color: '#6366f1', bg: 'rgba(99,102,241,0.12)' },
      thinking: { icon: 'fa-brain', color: '#a78bfa', bg: 'rgba(167,139,250,0.12)' },
      tool_call: { icon: 'fa-wrench', color: '#60a5fa', bg: 'rgba(96,165,250,0.12)' },
      tool_result: { icon: 'fa-check-circle', color: '#34d399', bg: 'rgba(52,211,153,0.12)' },
      text: { icon: 'fa-pen-nib', color: 'var(--accent-cyan)', bg: 'rgba(0,198,255,0.12)' },
      complete: { icon: 'fa-flag-checkered', color: '#34d399', bg: 'rgba(52,211,153,0.12)' },
      error: { icon: 'fa-exclamation-triangle', color: '#f87171', bg: 'rgba(248,113,113,0.12)' }
    };

    const cfg = typeConfig[type] || typeConfig.thinking;
    const isLast = type === 'complete' || type === 'error';

    node.innerHTML = `
      <div style="display:flex;flex-direction:column;align-items:center;width:36px;">
        <div style="width:32px;height:32px;border-radius:10px;background:${cfg.bg};display:flex;align-items:center;justify-content:center;">
          <i class="fa-solid ${cfg.icon}" style="font-size:0.8rem;color:${cfg.color};"></i>
        </div>
        ${!isLast ? '<div style="width:2px;flex:1;min-height:16px;background:rgba(255,255,255,0.06);margin:4px 0;"></div>' : ''}
      </div>
      <div style="flex:1;padding-bottom:${isLast?'0':'16px'};">
        <div style="font-size:0.82rem;font-weight:600;color:var(--text-primary);margin-bottom:2px;">${title}</div>
        <div style="font-size:0.78rem;color:var(--text-muted);line-height:1.5;">${detail}</div>
        <div style="font-size:0.7rem;color:var(--text-muted);margin-top:4px;opacity:0.6;">步骤 ${stepIndex}</div>
      </div>
    `;

    agentFlow.appendChild(node);
    agentFlow.scrollTop = agentFlow.scrollHeight;
  }
})();
