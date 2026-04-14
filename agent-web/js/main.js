/* ============================================
   ChronoTech Main Scripts — 全局交互逻辑
   ============================================ */

document.addEventListener('DOMContentLoaded', () => {
    // ==== 1. Navbar 滚动毛玻璃效果 ====
    const navbar = document.getElementById('navbar');
    
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });

    // ==== 2. 移动端菜单开关 ====
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const navLinks = document.querySelector('.nav-links');

    if (mobileMenuBtn && navLinks) {
        mobileMenuBtn.addEventListener('click', () => {
            mobileMenuBtn.classList.toggle('active');
            navLinks.classList.toggle('open');
        });

        // 点击链接后自动关闭菜单
        navLinks.querySelectorAll('.nav-link').forEach(link => {
            link.addEventListener('click', () => {
                mobileMenuBtn.classList.remove('active');
                navLinks.classList.remove('open');
            });
        });
    }

    // ==== 3. 入场动画 — Intersection Observer ====
    const fadeElements = document.querySelectorAll('.fade-in-up');
    
    if (fadeElements.length > 0) {
        const fadeObserver = new IntersectionObserver((entries) => {
            entries.forEach((entry, index) => {
                if (entry.isIntersecting) {
                    // 错开入场延迟，更有层次感
                    setTimeout(() => {
                        entry.target.classList.add('visible');
                    }, index * 100);
                    fadeObserver.unobserve(entry.target);
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        });

        fadeElements.forEach(el => fadeObserver.observe(el));
    }

    // ==== 4. 国际化支持 (中英切换) ====
    const i18nData = {
        en: {
            nav_home: "Home",
            nav_products: "Products",
            nav_about: "About Us",
            nav_news: "News",
            nav_contact: "Contact",
            hero_sub: "Smart tech defining the future. Discover the new ChronoTech electronic smartwatch series, merging exquisite craftsmanship with AI.",
            hero_btn_explore: "Explore Products",
            hero_btn_video: "Watch Video",
            sec_products_title: "Discover Masterpieces",
            sec_products_sub: "Whether for pro sports, business, or extreme outdoors, we have the perfect fit.",
            prod_sport_sub: "Flagship Sports, Fearless Challenge",
            prod_elite_sub: "Forged in Titanium, Business First",
            prod_rugged_sub: "Extreme Environments, Strong Survival",
            btn_detail: "View Details",
            sec_value_title: "Core Technology",
            sec_value_sub: "Refining craftsmanship and algorithmic engines from the inside out",
            val_battery: "30-Day Battery",
            val_battery_sub: "In-house low-power chip, no battery anxiety",
            val_water: "10ATM Waterproof",
            val_water_sub: "Supports 100m diving, fearless in any waters",
            val_health: "AI Health Monitor",
            val_health_sub: "24/7 Heart rate, SpO2 & Sleep tracking",
            val_gps: "Global Positioning",
            val_gps_sub: "GPS+Beidou dual-band, never get lost",
            stat_users: "Trusted Users",
            stat_patents: "Core Patents",
            stat_countries: "Countries Covered",
            footer_desc: "Breaking traditions, redefining wrist-worn tech gear.",
            footer_links: "Quick Links",
            footer_support: "Support",
            footer_social: "Follow Us",
            sup_manual: "User Manual",
            sup_warranty: "Warranty Policy"
        },
        zh: {}
    };

    const langBtns = document.querySelectorAll('.lang-btn');
    const elementsToTranslate = document.querySelectorAll('[data-i18n]');

    // 保存初始中文
    const originalTexts = {};
    elementsToTranslate.forEach(el => {
        originalTexts[el.getAttribute('data-i18n')] = el.innerHTML;
    });
    i18nData.zh = originalTexts;

    langBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            langBtns.forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');

            const lang = e.target.getAttribute('data-lang');
            const data = i18nData[lang];
            
            if (data) {
                elementsToTranslate.forEach(el => {
                    const key = el.getAttribute('data-i18n');
                    if (data[key]) {
                        el.innerHTML = data[key];
                    }
                });
            }
        });
    });

    // ==== 5. 数字滚动动画 (Stats Counter) ====
    const statsSection = document.querySelector('.stats-section');
    const statItems = document.querySelectorAll('.stat-item h3');
    let hasAnimated = false;

    const animateValue = (obj, start, end, duration) => {
        let startTimestamp = null;
        const suffix = obj.textContent.includes('+') ? '+' : '';
        const step = (timestamp) => {
            if (!startTimestamp) startTimestamp = timestamp;
            const progress = Math.min((timestamp - startTimestamp) / duration, 1);
            obj.innerHTML = Math.floor(progress * (end - start) + start).toLocaleString() + suffix;
            if (progress < 1) {
                window.requestAnimationFrame(step);
            }
        };
        window.requestAnimationFrame(step);
    };

    if (statsSection) {
        const statsObserver = new IntersectionObserver((entries) => {
            if(entries[0].isIntersecting && !hasAnimated) {
                hasAnimated = true;
                if (statItems[0]) animateValue(statItems[0], 0, 2000000, 2000);
                if (statItems[1]) animateValue(statItems[1], 0, 50, 1500);
                if (statItems[2]) animateValue(statItems[2], 0, 35, 1500);
            }
        });
        statsObserver.observe(statsSection);
    }

    // ==== 6. 产品筛选 (产品列表页) ====
    const filterBtns = document.querySelectorAll('.filter-btn');
    const productCards = document.querySelectorAll('.product-showcase-card');

    if (filterBtns.length > 0 && productCards.length > 0) {
        filterBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                // 切换按钮高亮
                filterBtns.forEach(b => b.classList.remove('active'));
                btn.classList.add('active');

                const category = btn.getAttribute('data-category');

                // 筛选卡片（带动画）
                productCards.forEach(card => {
                    if (category === 'all' || card.getAttribute('data-category') === category) {
                        card.classList.remove('hidden');
                        card.style.animation = 'fadeInUp 0.5s ease forwards';
                    } else {
                        card.classList.add('hidden');
                    }
                });
            });
        });
    }
});

/* 筛选动画关键帧 */
const styleSheet = document.createElement('style');
styleSheet.textContent = `
@keyframes fadeInUp {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}
`;
document.head.appendChild(styleSheet);
