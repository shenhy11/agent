/**
 * 微调对比页面请求逻辑
 */
(function() {
  const API_BASE = '';
  const inputEl = document.getElementById('compareInput');
  const sendBtn = document.getElementById('compareSendBtn');
  const baseOut = document.getElementById('baseOutput');
  const loraOut = document.getElementById('loraOutput');

  document.querySelectorAll('.preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      inputEl.value = btn.textContent;
      sendCompareReq();
    });
  });

  sendBtn.addEventListener('click', sendCompareReq);
  inputEl.addEventListener('keydown', e => {
    if (e.key === 'Enter') sendCompareReq();
  });

  function sendCompareReq() {
    const text = inputEl.value.trim();
    if (!text) return;
    
    sendBtn.disabled = true;
    baseOut.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div>';
    loraOut.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div>';

    Promise.all([
      fetchStream(`${API_BASE}/api/finetune/base/stream?message=${encodeURIComponent(text)}`, baseOut),
      fetchStream(`${API_BASE}/api/finetune/lora/stream?message=${encodeURIComponent(text)}`, loraOut)
    ]).finally(() => {
      sendBtn.disabled = false;
    });
  }

  function fetchStream(url, container) {
    return new Promise((resolve, reject) => {
      let isFirst = true;
      let fullText = '';
      fetch(url)
        .then(res => {
          const reader = res.body.getReader();
          const decoder = new TextDecoder();
          function read() {
            reader.read().then(({done, value}) => {
              if (done) { resolve(); return; }
              const chunk = decoder.decode(value, {stream: true});
              const lines = chunk.split('\n');
              for (const line of lines) {
                if (line.startsWith('data:')) {
                  const data = line.substring(5).trim();
                  if (data && data !== '[DONE]') {
                    if (isFirst) { container.innerHTML = ''; isFirst = false; }
                    fullText += data;
                    container.textContent = fullText;
                    container.scrollTop = container.scrollHeight;
                  }
                }
              }
              read();
            });
          }
          read();
        }).catch(err => {
          container.innerHTML = `<span style="color:#f87171;">请求报错: ${err.message}</span>`;
          reject(err);
        });
    });
  }
})();
