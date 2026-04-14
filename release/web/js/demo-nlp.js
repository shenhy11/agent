/**
 * NLP 工具箱 Demo 交互逻辑
 */
(function() {
  const API_BASE = 'http://localhost:8080';
  let currentTab = 'summarize';

  // 示例文本
  const examples = {
    summarize: `ChronoTech 在 2026 年春季发布了全新的 Elite E2 Ultra 旗舰智能手表。这款手表采用航空级钛合金表壳和蓝宝石玻璃镜面，搭载自研 ChronoChip C2 芯片，实现了 21 天超长续航。在健康监测方面，E2 Ultra 首次搭载 ECG 心电图功能和体温传感器，支持全天候血氧监测。此外，这款手表还支持 eSIM 独立通话和 NFC 支付功能，是目前市面上功能最全面的智能手表之一。新品定价 3999 元，已在全国线下门店和电商平台同步开售。`,
    sentiment: `刚收到 Sport X2 Pro，真的太惊喜了！续航表现远超预期，连续用了两周才充电。GPS 定位非常精准，跑步轨迹一丝不差。最重要的是才 52g，戴在手上完全没有负担。唯一的小遗憾是不支持 NFC，如果下一代能加上就完美了。总体来说性价比超高，强烈推荐给跑步爱好者！`,
    keywords: `ChronoTech 全新 Rugged R2 Titan 户外旗舰手表通过了 MIL-STD-810H 军规认证，采用钛合金材质和蓝宝石玻璃，支持 GPS+北斗+GLONASS+Galileo 全频段定位。手表内置离线地图功能，支持气压风暴预警和轨迹返航。在续航方面，普通模式下可达 25 天，GPS 持续模式下可达 60 小时，特别适合登山、越野跑和长途徒步等极限户外运动场景。`,
    translate: `智能科技，定义未来。ChronoTech 致力于将极致工艺与人工智能完美融合，打造每个人手腕上的智能中枢。我们相信，科技不仅是冰冷的数字，更是温暖人心的力量。`
  };

  // Tab 切换
  document.querySelectorAll('.nlp-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.nlp-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentTab = tab.dataset.tab;
      // 切换翻译选项显示
      document.getElementById('translateOptions').style.display = currentTab === 'translate' ? 'block' : 'none';
    });
  });

  // 填入示例
  document.getElementById('fillExample').addEventListener('click', () => {
    document.getElementById('nlpInput').value = examples[currentTab] || '';
  });

  // 开始分析
  document.getElementById('nlpAnalyze').addEventListener('click', analyze);

  async function analyze() {
    const text = document.getElementById('nlpInput').value.trim();
    if (!text) {
      alert('请输入需要分析的文本');
      return;
    }

    const outputEl = document.getElementById('nlpOutput');
    const durationEl = document.getElementById('nlpDuration');
    const btn = document.getElementById('nlpAnalyze');

    // 加载状态
    btn.disabled = true;
    btn.innerHTML = '<div class="loading-dots"><span></span><span></span><span></span></div> 分析中...';
    outputEl.innerHTML = '<div style="text-align:center;padding:3rem 0;"><div class="loading-dots"><span></span><span></span><span></span></div><p style="color:var(--text-muted);margin-top:1rem;font-size:0.85rem;">AI 正在分析中，请稍候...</p></div>';

    const startTime = Date.now();

    try {
      const endpoint = `/api/nlp/${currentTab}`;
      const body = { text };
      if (currentTab === 'translate') {
        body.targetLang = document.getElementById('targetLang').value;
      }

      const response = await fetch(`${API_BASE}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });

      const result = await response.json();
      const duration = Date.now() - startTime;
      durationEl.textContent = `耗时 ${duration}ms`;

      if (result.code === 200) {
        renderResult(currentTab, result.data);
      } else {
        outputEl.innerHTML = `<p style="color:#f87171;">⚠️ 分析失败: ${result.message}</p>`;
      }
    } catch (err) {
      outputEl.innerHTML = `<p style="color:#f87171;">⚠️ 请求失败: ${err.message}<br><span style="font-size:0.8rem;">请确保后端服务在 localhost:8080 运行</span></p>`;
      durationEl.textContent = '';
    }

    btn.disabled = false;
    btn.innerHTML = '<i class="fa-solid fa-bolt"></i> 开始分析';
  }

  /**
   * 渲染分析结果
   */
  function renderResult(tab, data) {
    const outputEl = document.getElementById('nlpOutput');

    // 尝试解析 JSON 字符串
    let parsed = data;
    if (typeof data === 'string') {
      try {
        // 移除可能的 markdown 代码块包裹
        let cleaned = data.replace(/```json\n?/g, '').replace(/```\n?/g, '').trim();
        parsed = JSON.parse(cleaned);
      } catch(e) {
        // 非 JSON，直接显示文本
        outputEl.innerHTML = `<div style="line-height:1.8;font-size:0.9rem;">${data}</div>`;
        return;
      }
    }

    switch(tab) {
      case 'summarize':
        outputEl.innerHTML = `
          <h4 style="margin-bottom:0.8rem;font-size:0.95rem;color:var(--text-primary);">📋 摘要结果</h4>
          <div style="padding:1rem;background:rgba(0,198,255,0.05);border:1px solid rgba(0,198,255,0.15);border-radius:12px;line-height:1.7;font-size:0.9rem;">
            ${parsed.summary || data}
          </div>
          ${parsed.wordCount ? `<p style="font-size:0.8rem;color:var(--text-muted);margin-top:0.8rem;">原文 ${parsed.wordCount} 字 → 摘要 ${parsed.summaryWordCount || '?'} 字</p>` : ''}
        `;
        break;

      case 'sentiment':
        const sentimentClass = parsed.sentiment === 'positive' ? 'sentiment-positive' :
                               parsed.sentiment === 'negative' ? 'sentiment-negative' : 'sentiment-neutral';
        const sentimentEmoji = parsed.sentiment === 'positive' ? '😊' :
                               parsed.sentiment === 'negative' ? '😞' : '😐';
        const sentimentLabel = parsed.sentiment === 'positive' ? '正面' :
                               parsed.sentiment === 'negative' ? '负面' : '中性';
        outputEl.innerHTML = `
          <div class="sentiment-badge ${sentimentClass}">${sentimentEmoji} ${sentimentLabel}</div>
          <div style="margin-bottom:1rem;">
            <span style="font-size:0.85rem;color:var(--text-muted);">置信度：</span>
            <div style="display:flex;align-items:center;gap:8px;margin-top:4px;">
              <div style="flex:1;height:8px;background:rgba(255,255,255,0.05);border-radius:4px;overflow:hidden;">
                <div style="width:${(parsed.confidence||0)*100}%;height:100%;background:linear-gradient(90deg,var(--accent-blue),var(--accent-cyan));border-radius:4px;transition:width 0.5s ease;"></div>
              </div>
              <span style="font-size:0.85rem;font-weight:600;">${((parsed.confidence||0)*100).toFixed(1)}%</span>
            </div>
          </div>
          ${parsed.keywords && parsed.keywords.length ? `
            <div style="margin-bottom:0.8rem;">
              <span style="font-size:0.85rem;color:var(--text-muted);">关键词：</span>
              <div style="margin-top:6px;">${parsed.keywords.map(k => `<span class="keyword-tag">${k}</span>`).join('')}</div>
            </div>` : ''}
          ${parsed.reason ? `<p style="font-size:0.85rem;color:var(--text-secondary);line-height:1.6;"><strong>分析理由：</strong>${parsed.reason}</p>` : ''}
        `;
        break;

      case 'keywords':
        const keywords = parsed.keywords || [];
        outputEl.innerHTML = `
          <h4 style="margin-bottom:1rem;font-size:0.95rem;">🏷️ 提取到 ${keywords.length} 个关键词</h4>
          <div style="display:flex;flex-wrap:wrap;gap:6px;">
            ${keywords.map(k => {
              const word = typeof k === 'string' ? k : k.word;
              const weight = typeof k === 'string' ? 1 : (k.weight || 0.5);
              const size = 0.75 + weight * 0.35;
              return `<span class="keyword-tag" style="font-size:${size}rem;opacity:${0.6+weight*0.4}">${word}</span>`;
            }).join('')}
          </div>
        `;
        break;

      case 'translate':
        outputEl.innerHTML = `
          <h4 style="margin-bottom:0.8rem;font-size:0.95rem;">🌐 翻译结果</h4>
          <div style="padding:1rem;background:rgba(0,198,255,0.05);border:1px solid rgba(0,198,255,0.15);border-radius:12px;line-height:1.7;font-size:0.9rem;">
            ${parsed.translated || data}
          </div>
          <p style="font-size:0.8rem;color:var(--text-muted);margin-top:0.5rem;">${parsed.sourceLang || ''} → ${parsed.targetLang || ''}</p>
        `;
        break;
    }
  }
})();
