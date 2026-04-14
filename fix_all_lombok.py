"""
全局修复：把所有 @lombok.Data / @Data 内部类替换为手写 getter/setter
同时修复 SummarizingChatMemory 缺少的 get(String) 方法和 Message.getContent() -> getMessage().getText()
"""
import os
import re

SRC = r"d:\code\agent\agent-server\src\main\java"

# ============ 1. 全局删除 @lombok.Data 和 import lombok.Data ============
def strip_lombok_data(filepath, content):
    """删除 @lombok.Data, @Data (仅内部类注解), import lombok.Data"""
    content = content.replace("import lombok.Data;\n", "")
    content = content.replace("import lombok.Data;\r\n", "")
    # 去掉顶层 @Data（单独一行的）
    content = re.sub(r'\n\s*@Data\s*\n', '\n', content)
    content = re.sub(r'\n\s*@lombok\.Data\s*\n', '\n', content)
    return content

# ============ 2. 为所有内部 static class 生成 getter/setter ============
FIELD_PATTERN = re.compile(r'^\s+private\s+(\S+)\s+(\w+)\s*;', re.MULTILINE)

def generate_accessors(fields):
    """根据 [(type, name), ...] 生成 getter/setter 代码"""
    code = ""
    for ftype, fname in fields:
        getter_name = "get" + fname[0].upper() + fname[1:]
        setter_name = "set" + fname[0].upper() + fname[1:]
        if ftype == "boolean":
            getter_name = "is" + fname[0].upper() + fname[1:]
        code += f"""
        public {ftype} {getter_name}() {{ return {fname}; }}
        public void {setter_name}({ftype} {fname}) {{ this.{fname} = {fname}; }}
"""
    return code

def add_accessors_to_inner_classes(content):
    """找到所有 public static class ... { fields } 并在 fields 后面加 getter/setter"""
    # 匹配 public static class Xxx { ... } 
    pattern = re.compile(
        r'(public static class \w+ \{)(.*?)(^\s*\})',
        re.DOTALL | re.MULTILINE
    )
    
    def replacer(m):
        header = m.group(1)
        body = m.group(2)
        closing = m.group(3)
        
        fields = FIELD_PATTERN.findall(body)
        if not fields:
            return m.group(0)
        
        # 检查是否已经有 getter（避免重复）
        if "public " in body and "get" in body and "return " in body:
            return m.group(0)
        
        accessors = generate_accessors(fields)
        return header + body + accessors + closing
    
    return pattern.sub(replacer, content)

# ============ 3. 修复 SummarizingChatMemory ============
def fix_summarizing_memory(content):
    """
    修复:
    1. m.getContent() -> m.getText()  (Spring AI 1.0 Message 接口)
    2. 添加 get(String) 方法
    """
    content = content.replace("m.getContent()", "m.getText()")
    
    # 添加 get(String conversationId) 方法（如果不存在）
    if "public List<Message> get(String conversationId)" not in content and \
       "List<Message> get(String conversationId)" not in content:
        # 在 clear 方法前面插入
        insert = """
    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, windowSize);
    }

"""
        content = content.replace("    @Override\n    public void clear(", insert + "    @Override\n    public void clear(")
        content = content.replace("    @Override\r\n    public void clear(", insert + "    @Override\r\n    public void clear(")
    
    return content

# ============ 4. 修复 @RequiredArgsConstructor ============
def fix_required_args(filepath, content):
    """删除 @RequiredArgsConstructor 并添加手写构造函数"""
    if "@RequiredArgsConstructor" not in content:
        return content
    
    content = content.replace("import lombok.RequiredArgsConstructor;\n", "")
    content = content.replace("import lombok.RequiredArgsConstructor;\r\n", "")
    content = content.replace("@RequiredArgsConstructor\n", "")
    content = content.replace("@RequiredArgsConstructor\r\n", "")
    
    # 找到类名
    class_match = re.search(r'public class (\w+)', content)
    if not class_match:
        return content
    class_name = class_match.group(1)
    
    # 找所有 private final 字段
    final_fields = re.findall(r'^\s+private final (\S+(?:<[^;]+>)?)\s+(\w+);', content, re.MULTILINE)
    if not final_fields:
        return content
    
    # 检查是否已有构造函数
    if f"public {class_name}(" in content:
        return content
    
    # 生成构造函数
    params = ", ".join([f"{ftype} {fname}" for ftype, fname in final_fields])
    assigns = "\n".join([f"        this.{fname} = {fname};" for _, fname in final_fields])
    constructor = f"""
    public {class_name}({params}) {{
{assigns}
    }}
"""
    
    # 找到类体开头，在第一个字段前插入
    # 找到 class Xxx { 之后的位置
    class_body = re.search(rf'public class {class_name}\b[^{{]*\{{', content)
    if class_body:
        insert_pos = class_body.end()
        content = content[:insert_pos] + constructor + content[insert_pos:]
    
    return content

# ============ 主流程 ============
def process_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        original = f.read()
    
    content = original
    content = strip_lombok_data(filepath, content)
    content = add_accessors_to_inner_classes(content)
    content = fix_required_args(filepath, content)
    
    if "SummarizingChatMemory.java" in filepath:
        content = fix_summarizing_memory(content)
    
    if content != original:
        with open(filepath, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"  Fixed: {os.path.basename(filepath)}")

def walk_and_fix():
    count = 0
    for root, _, files in os.walk(SRC):
        for f in files:
            if f.endswith(".java"):
                process_file(os.path.join(root, f))
                count += 1
    print(f"\nProcessed {count} files.")

if __name__ == "__main__":
    walk_and_fix()
