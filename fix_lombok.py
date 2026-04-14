import os
import re

def fix_lombok(directory):
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                file_path = os.path.join(root, file)
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                
                # Check if @Slf4j is in file
                if "@Slf4j" in content:
                    class_match = re.search(r'public class (\w+)', content)
                    if class_match:
                        class_name = class_match.group(1)
                        # Remove lombok import
                        content = re.sub(r'import lombok\.extern\.slf4j\.Slf4j;\n', '', content)
                        # Replace @Slf4j tag
                        content = re.sub(r'@Slf4j\n', '', content)
                        # Add SLF4J imports manually if not present
                        if 'import org.slf4j.Logger;' not in content:
                            # Add directly after package declaration if no imports, or after first import
                            content = re.sub(r'(package .*?;)', r'\1\n\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;', content, count=1)
                        # Add logger declaration
                        logger_decl = f'\n    private static final Logger log = LoggerFactory.getLogger({class_name}.class);\n'
                        content = re.sub(rf'public class {class_name} {{', f'public class {class_name} {{{logger_decl}', content)
                        
                        with open(file_path, "w", encoding="utf-8") as f:
                            f.write(content)
                        print(f"Fixed @Slf4j in {file}")

                # Also fix report.getError() to report.error() in EvaluationController
                if "EvaluationController.java" in file:
                    content = content.replace("report.getError()", "report.error()")
                    with open(file_path, "w", encoding="utf-8") as f:
                        f.write(content)

if __name__ == "__main__":
    fix_lombok(r"d:\code\agent\agent-server\src\main\java")
