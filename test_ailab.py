"""
ChronoTech AI Lab 前端自动化测试脚本
覆盖：ai-lab.html 主页、demo-nlp.html 各 Tab、demo-vision.html 各 Tab
"""
from playwright.sync_api import sync_playwright
import time, json, os

BASE = "http://localhost:8000"
REPORT = {}
SHOT_DIR = "C:\\tmp_shots"
os.makedirs(SHOT_DIR, exist_ok=True)

def log(key, value):
    REPORT[key] = value
    print(f"[{key}] {value}")

def shot(page, name):
    p = f"{SHOT_DIR}\\{name}.png"
    page.screenshot(path=p, full_page=True)
    log(f"screenshot_{name}", p)

def test_ailab_home(page):
    page.goto(f"{BASE}/ai-lab.html")
    page.wait_for_load_state("networkidle")
    log("home_title", page.title())
    log("home_card_count", page.locator(".lab-card").count())
    shot(page, "ailab_home")

def test_nlp_tabs(page):
    page.goto(f"{BASE}/demo-nlp.html")
    page.wait_for_load_state("networkidle")
    tabs = page.locator(".nlp-tab").all()
    log("nlp_tab_count", len(tabs))

    for tab in tabs:
        name = tab.inner_text().strip().replace(" ", "_").replace("/", "_")
        tab.click()
        page.wait_for_timeout(400)
        shot(page, f"nlp_{name}")

    # 情感分析 → 点预置
    page.locator(".nlp-tab").first.click()
    page.wait_for_timeout(300)
    presets = page.locator(".preset-btn").all()
    if presets:
        presets[0].click()
        page.wait_for_timeout(400)
        page.locator("#nlpAnalyze").click()
        try:
            page.wait_for_selector("#nlpOutput h4, #nlpOutput .sentiment-badge", timeout=30000)
            log("nlp_result", page.locator("#nlpOutput").inner_text()[:200])
        except Exception as e:
            log("nlp_result_timeout", str(e))
        shot(page, "nlp_result")

def test_vision_tabs(page):
    page.goto(f"{BASE}/demo-vision.html")
    page.wait_for_load_state("networkidle")
    tabs = page.locator(".vision-tab").all()
    log("vision_tab_count", len(tabs))

    for tab in tabs:
        name = tab.inner_text().strip().replace(" ", "_").replace("/", "_")
        tab.click()
        page.wait_for_timeout(400)
        shot(page, f"vision_{name}")

    # 预置图片加载测试
    page.locator(".vision-tab[data-tab='chat']").click()
    page.wait_for_timeout(300)
    presets = page.locator(".vision-preset-btn").all()
    if presets:
        presets[0].click()
        page.wait_for_timeout(2500)
        shot(page, "vision_preset_loaded")

def test_rag_page(page):
    page.goto(f"{BASE}/demo-rag.html")
    page.wait_for_load_state("networkidle")
    log("rag_h1", page.locator("h1").first.inner_text())
    shot(page, "rag_page")

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1280, "height": 900})
    console_errors = []
    page.on("console", lambda msg: console_errors.append(msg.text) if msg.type == "error" else None)

    try:
        test_ailab_home(page)
        test_nlp_tabs(page)
        test_vision_tabs(page)
        test_rag_page(page)
    finally:
        log("console_errors", console_errors[:10])
        browser.close()

print("\n===== 测试报告 =====")
print(json.dumps(REPORT, ensure_ascii=False, indent=2))
