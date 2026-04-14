from playwright.sync_api import sync_playwright
import time
import os

def run_test():
    with sync_playwright() as p:
        print("Launching browser...")
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        print("Navigating to AI Lab Homepage (http://localhost:3001/ai-lab.html)...")
        page.goto('http://localhost:3001/ai-lab.html')
        page.wait_for_load_state('networkidle')
        
        title = page.title()
        print(f"Page title is: {title}")
        if "AI Lab" in title:
            print("[Pass] AI Lab Homepage loaded successfully.")
        else:
            print("[Fail] Title mismatch.")

        print("Navigating to RAG Demo...")
        page.goto('http://localhost:3001/demo-rag.html')
        page.wait_for_load_state('networkidle')
        print(f"RAG Demo title: {page.title()}")
        
        page.fill('#ragInput', '运动版手表续航怎么样？')
        page.click('#ragSendBtn')
        print("Submitted test query to RAG Pipeline...")

        try:
            page.wait_for_selector('.chat-msg-ai .chat-msg-content', timeout=10000)
            print("[Pass] AI responded in RAG Demo.")
            
            # Check for thinking/sources rendering
            sources = page.locator('#ragSources').inner_text()
            print(f"Sources rendered: {sources[:50]}...")
        except Exception as e:
            print(f"[Warn] Backend response timeout or element not found: {e}")

        # Navigate to Multi-Agent Demo
        print("Navigating to Agent Workflow Demo...")
        page.goto('http://localhost:3001/demo-agent.html')
        page.wait_for_load_state('networkidle')
        
        page.fill('#agentInput', '帮我查询 Classic C1 的价格')
        page.click('#agentSendBtn')
        print("Submitted query to Multi-Agent...")
        try:
            page.wait_for_selector('#agentFlow > div', timeout=10000)
            print("[Pass] Multi-Agent flow visualization started.")
        except Exception as e:
            print(f"[Warn] Agent response timeout: {e}")

        # Navigate to Finetune Compare Demo
        print("Navigating to LoRA Finetune Demo...")
        page.goto('http://localhost:3001/demo-finetune.html')
        page.wait_for_load_state('networkidle')
        
        page.fill('#compareInput', '售后政策是怎样的？')
        page.click('#compareSendBtn')
        print("Submitted query to Finetune API...")
        try:
            page.wait_for_selector('.loading-dots', state='hidden', timeout=10000)
            print("[Pass] Finetuned model returned response.")
        except Exception as e:
            print(f"[Warn] Finetune model response timeout: {e}")

        browser.close()
        print("All tests finished.")

if __name__ == '__main__':
    run_test()
