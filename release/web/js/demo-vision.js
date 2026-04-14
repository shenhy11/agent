/**
 * 多模态 Vision Demo 交互逻辑
 * 拖拽上传 + 图片预览 + FormData 提交
 */
(function() {
  const API_BASE = 'http://localhost:8080';
  const dropZone = document.getElementById('dropZone');
  const imageInput = document.getElementById('imageInput');
  const previewArea = document.getElementById('previewArea');
  const previewImg = document.getElementById('previewImg');
  const removeImgBtn = document.getElementById('removeImg');
  const questionInput = document.getElementById('visionQuestion');
  const sendBtn = document.getElementById('visionSendBtn');
  const outputEl = document.getElementById('visionOutput');
  const durationEl = document.getElementById('visionDuration');

  let selectedFile = null;

  // 点击上传区触发文件选择
  dropZone.addEventListener('click', () => imageInput.click());

  // 拖拽事件
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
    const files = e.dataTransfer.files;
    if (files.length > 0) handleFile(files[0]);
  });

  // 文件选择
  imageInput.addEventListener('change', (e) => {
    if (e.target.files.length > 0) handleFile(e.target.files[0]);
  });

  // 移除图片
  removeImgBtn.addEventListener('click', () => {
    selectedFile = null;
    previewArea.style.display = 'none';
    dropZone.style.display = 'flex';
    imageInput.value = '';
  });

  // 处理文件
  function handleFile(file) {
    // 校验格式
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) {
      alert('不支持的图片格式，仅支持 JPEG/PNG/WebP');
      return;
    }
    // 校验大小
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过 5MB');
      return;
    }

    selectedFile = file;

    // 预览
    const reader = new FileReader();
    reader.onload = (e) => {
      previewImg.src = e.target.result;
      previewArea.style.display = 'block';
      dropZone.style.display = 'none';
    };
    reader.readAsDataURL(file);
  }

  // 提交分析
  sendBtn.addEventListener('click', async () => {
    if (!selectedFile) {
      alert('请先上传一张图片');
      return;
    }
    const question = questionInput.value.trim() || '请描述这张图片的内容';

    sendBtn.disabled = true;
    sendBtn.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div> 分析中...';
    outputEl.innerHTML = '<div style="text-align:center;padding:3rem 0;"><div class="loading-dots"><span></span><span></span><span></span></div><p style="color:var(--text-muted);margin-top:1rem;font-size:0.85rem;">多模态模型分析中，可能需要几秒钟...</p></div>';

    const startTime = Date.now();

    try {
      const formData = new FormData();
      formData.append('image', selectedFile);
      formData.append('question', question);

      const response = await fetch(`${API_BASE}/api/multimodal/chat`, {
        method: 'POST',
        body: formData
      });

      const result = await response.json();
      const duration = Date.now() - startTime;
      durationEl.textContent = `耗时 ${duration}ms`;

      if (result.code === 200) {
        outputEl.innerHTML = `
          <div style="line-height:1.8;font-size:0.9rem;white-space:pre-wrap;">${result.data}</div>
        `;
      } else {
        outputEl.innerHTML = `<p style="color:#f87171;">⚠️ 分析失败: ${result.message}</p>`;
      }
    } catch(err) {
      outputEl.innerHTML = `<p style="color:#f87171;">⚠️ 请求失败: ${err.message}<br><span style="font-size:0.8rem;">请确保后端服务在 localhost:8080 运行</span></p>`;
      durationEl.textContent = '';
    }

    sendBtn.disabled = false;
    sendBtn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> 提交分析';
  });
})();
