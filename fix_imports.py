import os

def fix_imports(directory):
    for root, _, files in os.walk(directory):
        for file in files:
            if not file.endswith(".java"): continue
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            if content.startswith("import lombok.Data;\npackage "):
                content = content.replace("import lombok.Data;\npackage ", "package ")
                content = content.replace("package com.agent.model.dto;", "package com.agent.model.dto;\n\nimport lombok.Data;")
                
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(content)
                  
if __name__ == "__main__":
    fix_imports(r"d:\code\agent\agent-server\src\main\java")
