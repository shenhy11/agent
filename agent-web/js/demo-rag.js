/**
 * RAG 知识库 Demo 交互逻辑
 * SSE 对话 + 文档上传 + 检索来源展示
 */
(function() {
  const API_BASE = 'http://localhost:8080';
  const ragMessages = document.getElementById('ragMessages');
  const ragInput = document.getElementById('ragInput');
  const ragSendBtn = document.getElementById('ragSendBtn');
  const ragSources = document.getElementById('ragSources');
  const ragUploadBtn = document.getElementById('ragUploadBtn');
  const ragUploadInput = document.getElementById('ragUpload');
  const ragUploadStatus = document.getElementById('ragUploadStatus');

  let conversationId = 'rag-' + Date.now();

  // 预置问题按钮
  document.querySelectorAll('.preset-btn[data-question]').forEach(btn => {
    btn.addEventListener('click', () => {
      ragInput.value = btn.dataset.question;
      sendMessage();
    });
  });

  // 发送
  ragSendBtn.addEventListener('click', sendMessage);
  ragInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); sendMessage(); }
  });

  // 文档上传
  ragUploadBtn.addEventListener('click', () => ragUploadInput.click());
  ragUploadInput.addEventListener('change', async (e) => {
    if (!e.target.files.length) return;
    const file = e.target.files[0];
    ragUploadStatus.textContent = '上传中...';

    try {
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`${API_BASE}/api/knowledge/user-upload/upload`, {
        method: 'POST',
        body: formData
      });
      const result = await response.json();
      if (result.code === 200) {
        ragUploadStatus.innerHTML = `<span style="color:#34d399;">✓ ${file.name} 上传成功，${result.data.chunks} 个文档块</span>`;
      } else {
        ragUploadStatus.innerHTML = `<span style="color:#f87171;">✗ ${result.message}</span>`;
      }
    } catch(err) {
      ragUploadStatus.innerHTML = `<span style="color:#f87171;">✗ 上传失败: ${err.message}</span>`;
    }
  });

  function sendMessage() {
    const text = ragInput.value.trim();
    if (!text) return;

    appendMessage('user', text);
    ragInput.value = '';
    ragSendBtn.disabled = true;
    ragSources.innerHTML = `<div style="padding:12px;color:var(--text-muted);text-align:center;"><div class="loading-dots"><span></span><span></span><span></span></div><br>检索文档中...</div>`;

    const aiMsgEl = appendMessage('ai', '');
    const contentEl = aiMsgEl.querySelector('.chat-msg-content');
    contentEl.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div>';

    // SSE 流式请求（走主 chatClient，已挂 RAG Advisor）
    const url = `${API_BASE}/api/chat/stream?message=${encodeURIComponent(text)}&conversationId=${conversationId}`;
    let fullText = '';
    let isFirst = true;

    fetch(url)
      .then(response => {
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        function read() {
          reader.read().then(({ done, value }) => {
            if (done) { ragSendBtn.disabled = false; return; }
            
            // SSE decoding
            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');
            let currentEvent = 'message';
            
            for (let i = 0; i < lines.length; i++) {
              const line = lines[i];
              if (line.startsWith('event:')) {
                currentEvent = line.substring(6).trim();
              } else if (line.startsWith('data:')) {
                const data = line.substring(5).trim();
                
                if (currentEvent === 'retrieval' && data && data !== '[DONE]') {
                   // Render real retrieval sources
                   try {
                     const docs = JSON.parse(data);
                     renderRealSources(docs);
                   } catch(e) {}
                } else if (currentEvent === 'message' && data && data !== '[DONE]') {
                   if (isFirst) { contentEl.innerHTML = ''; isFirst = false; }
                   fullText += data;
                   contentEl.textContent = fullText;
                   ragMessages.scrollTop = ragMessages.scrollHeight;
                }
              }
            }
            read();
          });
        }
        read();
      })
      .catch(err => {
        contentEl.innerHTML = `<span style="color:#f87171;">⚠️ 请求失败: ${err.message}</span>`;
        ragSendBtn.disabled = false;
      });
  }

  function appendMessage(type, text) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `chat-msg chat-msg-${type}`;
    const icon = type === 'user' ? 'fa-user' : 'fa-robot';
    msgDiv.innerHTML = `
      <div class="chat-msg-avatar"><i class="fa-solid ${icon}"></i></div>
      <div class="chat-msg-content">${text}</div>
    `;
    ragMessages.appendChild(msgDiv);
    ragMessages.scrollTop = ragMessages.scrollHeight;
    return msgDiv;
  }

  function renderRealSources(docs) {
    if (!docs || docs.length === 0) {
      ragSources.innerHTML = `<div style="padding:12px;color:var(--text-muted);">未检索到相关文档</div>`;
      return;
    }
    
    ragSources.innerHTML = docs.map((doc, i) => {
       const metadata = doc.metadata || {};
       const source = metadata.source || '未知来源';
       // We can use rrf_score or bm25_score if hybrid
       const scoreRaw = metadata.rrf_score || metadata.bm25_score || metadata.distance || 0;
       let scoreDisplay = scoreRaw;
       // Simply cap dummy display for score
       const widthPct = Math.min(100, Math.max(10, (typeof scoreDisplay === 'number' ? scoreDisplay * 100 : parseFloat(scoreDisplay)*100) || 85));

       return `
        <div style="padding:12px;background:rgba(255,255,255,0.03);border:1px solid rgba(255,255,255,0.06);border-radius:12px;margin-bottom:10px;animation:msgIn 0.3s ease ${i*0.1}s both;">
          <div style="display:flex;align-items:center;justify-content:between;margin-bottom:8px;">
            <div style="display:flex;align-items:center;gap:6px;flex:1;overflow:hidden;">
              <i class="fa-solid fa-file-lines" style="color:var(--accent-cyan);font-size:0.8rem;"></i>
              <span style="font-size:0.82rem;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${source} [Chunk: ${metadata.chunk_index || '-'}]</span>
            </div>
            <span style="font-size:0.75rem;font-weight:600;color:#34d399;">Score: ${(scoreDisplay).toFixed(3)}</span>
          </div>
          <p style="font-size:0.78rem;color:var(--text-muted);line-height:1.4;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden;">${doc.text}</p>
        </div>
      `;
    }).join('');
  }
})();
