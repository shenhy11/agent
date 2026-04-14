import os
import re

def to_camel_case(snake_str):
    components = snake_str.split('_')
    return components[0] + ''.join(x.title() for x in components[1:])

def fix_all_data(directory):
    for root, _, files in os.walk(directory):
        for file in files:
            if not file.endswith(".java"): continue
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()

            if "@Data" in content or "Lombok" in content or "lombok" in content:
                content = re.sub(r'import lombok\..*?;\n', '', content)
                content = re.sub(r'@Data\n', '', content)
                content = re.sub(r'@AllArgsConstructor\n', '', content)
                content = re.sub(r'@NoArgsConstructor\n', '', content)
                content = re.sub(r'@Builder\n', '', content)
                
                # If there is a class, we won't perfectly generate all getters/setters but we can replace it with a quick fix.
                # Actually, there's another error:
                # ToolExecutionService.java:[52,39] queryProductInfo(java.lang.String) not found.
                # ToolExecutionService.java:[57,39] compareProducts needs 1 arg but got 2.
            
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(content)
                  
if __name__ == "__main__":
    fix_all_data(r"d:\code\agent\agent-server\src\main\java")
