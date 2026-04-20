/**
 * 多模态 Vision Demo 交互逻辑
 * 支持多图、多模式 API
 */
(function() {
  const API_BASE = '';
  let currentTab = 'chat';
  let selectedFiles = []; // File objects
  let maxImages = 1;

  const dropZone = document.getElementById('dropZone');
  const imageInput = document.getElementById('imageInput');
  const previewArea = document.getElementById('previewArea');
  const previewImages = document.getElementById('previewImages');
  const removeImgBtn = document.getElementById('removeImg');
  const questionInput = document.getElementById('visionQuestion');
  const questionArea = document.getElementById('questionArea');
  const sendBtn = document.getElementById('visionSendBtn');
  const outputEl = document.getElementById('visionOutput');
  const durationEl = document.getElementById('visionDuration');
  const uploadLimitText = document.getElementById('uploadLimitText');

  // Tab 切换逻辑
  document.querySelectorAll('.vision-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.vision-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentTab = tab.dataset.tab;
      
      // 更新 UI 和限制
      if (currentTab === 'multi-chat') {
          maxImages = 3;
          uploadLimitText.innerText = '支持 JPEG/PNG/WebP，单图最大 5MB（当前模式最多支持 3 张）';
          questionArea.style.display = 'block';
      } else {
          maxImages = 1;
          uploadLimitText.innerText = '支持 JPEG/PNG/WebP，最大 5MB（当前模式支持 1 张）';
          // OCR、Describe、Search 不需要提问输入
          if (['ocr', 'describe', 'search'].includes(currentTab)) {
              questionArea.style.display = 'none';
          } else {
              questionArea.style.display = 'block';
          }
      }
      
      // 切换模式时清空已选图片
      clearImages();
    });
  });

  // 上传区域事件
  dropZone.addEventListener('click', () => imageInput.click());
  
  dropZone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropZone.style.borderColor = 'var(--accent-cyan)';
    dropZone.style.background = 'rgba(0,198,255,0.05)';
  });

  dropZone.addEventListener('dragleave', () => {
    dropZone.style.borderColor = 'rgba(255,255,255,0.1)';
    dropZone.style.background = 'transparent';
  });

  dropZone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropZone.style.borderColor = 'rgba(255,255,255,0.1)';
    dropZone.style.background = 'transparent';
    handleFiles(e.dataTransfer.files);
  });

  imageInput.addEventListener('change', (e) => {
    handleFiles(e.target.files);
    imageInput.value = ''; // reset
  });

  removeImgBtn.addEventListener('click', clearImages);
  
  // 预装图片 URL 逻辑
  document.querySelectorAll('.vision-preset-btn').forEach(btn => {
      btn.addEventListener('click', async (e) => {
          clearImages();
          const url = e.target.dataset.url;
          if (e.target.dataset.q) questionInput.value = e.target.dataset.q;
          
          try {
              const res = await fetch(url);
              const blob = await res.blob();
              const file = new File([blob], "preset.jpg", { type: blob.type });
              handleFiles([file]);
          } catch(err) {
              console.error(err);
          }
      });
  });

  function clearImages() {
    selectedFiles = [];
    previewImages.innerHTML = '';
    previewArea.style.display = 'none';
    dropZone.style.display = 'flex';
  }

  function handleFiles(files) {
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    
    Array.from(files).forEach(file => {
        if (selectedFiles.length >= maxImages) return;
        if (!allowed.includes(file.type)) {
            alert(`不支持的文件: ${file.name}`);
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            alert(`文件过大: ${file.name}`);
            return;
        }
        selectedFiles.push(file);
        
        // 生成预览
        const reader = new FileReader();
        reader.onload = (e) => {
            const img = document.createElement('img');
            img.src = e.target.result;
            img.style.maxWidth = maxImages === 1 ? '100%' : '30%';
            img.style.maxHeight = '200px';
            img.style.borderRadius = '8px';
            img.style.border = '1px solid rgba(255,255,255,0.1)';
            img.style.objectFit = 'cover';
            previewImages.appendChild(img);
        };
        reader.readAsDataURL(file);
    });

    if (selectedFiles.length > 0) {
        previewArea.style.display = 'block';
        if (selectedFiles.length >= maxImages) {
            dropZone.style.display = 'none';
        }
    }
  }

  // 提交分析
  sendBtn.addEventListener('click', async () => {
    if (selectedFiles.length === 0) {
      alert('请先上传图片');
      return;
    }

    sendBtn.disabled = true;
    sendBtn.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div> 处理中...';
    outputEl.innerHTML = '<div style="text-align:center;padding:3rem 0;"><div class="loading-dots"><span></span><span></span><span></span></div><p style="color:var(--text-muted);margin-top:1rem;font-size:0.85rem;">视觉模型处理中，可能需要几秒钟...</p></div>';
    const startTime = Date.now();

    try {
      const formData = new FormData();
      if (currentTab === 'multi-chat') {
          selectedFiles.forEach(f => formData.append('images', f));
          formData.append('question', questionInput.value.trim() || '请对比这些图片内容');
      } else {
          formData.append('image', selectedFiles[0]);
          if (currentTab === 'chat') {
              formData.append('question', questionInput.value.trim() || '请描述图片内容');
          }
      }

      let endpoint = `/api/multimodal/${currentTab}`;
      if (currentTab === 'search') endpoint = '/api/multimodal/search-by-image';

      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        body: formData
      });

      const result = await response.json();
      durationEl.textContent = `耗时 ${Date.now() - startTime}ms`;

      if (result.code === 200) {
        renderResult(result.data);
      } else {
        outputEl.innerHTML = `<p style="color:#f87171;">⚠️ 失败: ${result.message}</p>`;
      }
    } catch(err) {
      outputEl.innerHTML = `<p style="color:#f87171;">⚠️ 请求错误: ${err.message}</p>`;
      durationEl.textContent = '';
    }

    sendBtn.disabled = false;
    sendBtn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> 提交分析';
  });

  function renderResult(data) {
     if (currentTab === 'chat' || currentTab === 'multi-chat' || currentTab === 'ocr') {
        outputEl.innerHTML = `<div style="line-height:1.8;font-size:0.9rem;white-space:pre-wrap;">${data}</div>`;
     } else if (currentTab === 'describe') {
        try {
            const parsed = typeof data === 'string' ? JSON.parse(data.replace(/```json/g, '').replace(/```/g, '')) : data;
            outputEl.innerHTML = `
               <h4 style="margin-bottom:0.8rem;color:var(--accent-cyan);">✨ 结构化视觉理解</h4>
               <p style="font-size:0.9rem;margin-bottom:1rem;">${parsed.summary || ''}</p>
               <div style="background:rgba(255,255,255,0.05);padding:1rem;border-radius:12px;">
                 <div style="margin-bottom:0.5rem;color:var(--text-muted);font-size:0.8rem;">属性标签：</div>
                 <pre style="font-size:0.85rem;color:var(--text-secondary);white-space:pre-wrap;">${JSON.stringify(parsed.attributes, null, 2)}</pre>
               </div>
            `;
        } catch(e) {
            outputEl.innerHTML = `<div>${data}</div>`;
        }
     } else if (currentTab === 'search') {
         if (!data || data.length === 0) {
             outputEl.innerHTML = '<p>没有找到相似产品</p>';
             return;
         }
         let html = '<h4 style="margin-bottom:1rem;">🔍 检索结果（Top 3）</h4>';
         data.forEach(item => {
             const dist = typeof item.distance === 'number' ? item.distance : 0;
             const score = Math.max(0, 100 - (dist * 100)).toFixed(1);
             html += `
             <div style="margin-bottom:1rem;background:rgba(255,255,255,0.05);padding:1rem;border-radius:12px;display:flex;gap:1rem;">
                <div style="flex:1;">
                   <div style="font-weight:600;color:var(--text-primary);margin-bottom:0.5rem;">${item.metadata && item.metadata.docType || '产品片段'}</div>
                   <div style="font-size:0.85rem;color:var(--text-secondary);margin-bottom:0.5rem;">${item.content}</div>
                </div>
                <div style="width:80px;text-align:right;">
                   <div style="font-size:1.2rem;font-weight:700;color:var(--accent-cyan);">${score}%</div>
                   <div style="font-size:0.75rem;color:var(--text-muted);">相似度</div>
                </div>
             </div>`;
         });
         outputEl.innerHTML = html;
     }
  }
})();
