/**
 * ChronoTech 联系我们页 — 表单提交 + FAQ 交互
 */
document.addEventListener('DOMContentLoaded', () => {
    // ==== 1. 联系表单提交 ====
    const form = document.getElementById('contactForm');
    const submitBtn = document.getElementById('contactSubmitBtn');
    const successEl = document.getElementById('formSuccess');
    const ticketEl = document.getElementById('ticketId');

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();

            // 获取表单数据
            const formData = {
                name: document.getElementById('contactName').value.trim(),
                email: document.getElementById('contactEmail').value.trim(),
                phone: document.getElementById('contactPhone').value.trim(),
                subject: document.getElementById('contactSubject').value,
                message: document.getElementById('contactMessage').value.trim()
            };

            // 基础校验
            if (!formData.name || !formData.email || !formData.subject || !formData.message) {
                alert('请填写所有必填项');
                return;
            }

            // 按钮加载态
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin" style="margin-right:8px;"></i>提交中...';

            try {
                // 调用后端 API
                const response = await fetch('/api/contact', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });

                if (response.ok) {
                    const result = await response.json();
                    // 显示成功反馈
                    form.style.display = 'none';
                    successEl.style.display = 'block';
                    if (result.data && result.data.ticketId) {
                        ticketEl.textContent = `工单编号：${result.data.ticketId}`;
                    }
                } else {
                    throw new Error('提交失败');
                }
            } catch (error) {
                // 后端不可用时模拟成功（开发阶段）
                console.warn('联系 API 不可用，使用模拟模式:', error);
                form.style.display = 'none';
                successEl.style.display = 'block';
                // 生成模拟工单号
                const mockTicket = `CT-${new Date().toISOString().slice(0,10).replace(/-/g,'')}-${String(Math.floor(Math.random() * 999)).padStart(3,'0')}`;
                ticketEl.textContent = `工单编号：${mockTicket}`;
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fa-solid fa-paper-plane" style="margin-right:8px;"></i>提交留言';
            }
        });
    }

    // ==== 2. FAQ 折叠面板 ====
    const faqItems = document.querySelectorAll('.faq-item');

    faqItems.forEach(item => {
        const question = item.querySelector('.faq-question');
        question.addEventListener('click', () => {
            const isOpen = item.classList.contains('open');

            // 先关闭所有
            faqItems.forEach(i => i.classList.remove('open'));

            // 切换当前
            if (!isOpen) {
                item.classList.add('open');
            }
        });
    });
});
