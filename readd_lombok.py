import os
import re

def readd_lombok(directory):
    for root, _, files in os.walk(directory):
        for file in files:
            if not file.endswith(".java"): continue
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            original_content = content
            
            # Need to find any class or static class that doesn't have methods but has fields
            # Actually, the easiest way is checking if it had @Data or was a DTO
            # We know ReActEngine has inner classes: ToolCallEvent, ToolResultEvent, ThinkResult
            # UserProfileService has UserProfile
            # AiMetricsService has MetricsReport
            # All DTOs in model/dto
            
            if "model/dto" in file_path.replace("\\", "/"):
                if "@Data" not in content and "public record" not in content and "public class ApiResult" not in content:
                    content = "import lombok.Data;\n" + content
                    content = content.replace("public class", "@Data\npublic class")
            
            if "ReActEngine.java" in file:
                content = re.sub(r'public static class (ToolCallEvent|ToolResultEvent|ThinkResult) \{', r'@lombok.Data\n    public static class \1 {', content)
                
            if "UserProfileService.java" in file:
                content = re.sub(r'public static class UserProfile \{', r'@lombok.Data\n    public static class UserProfile {', content)
                
            if "AiMetricsService.java" in file:
                content = re.sub(r'public static class MetricsReport \{', r'@lombok.Data\n    public static class MetricsReport {', content)
                
            if content != original_content:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(content)

if __name__ == "__main__":
    readd_lombok(r"d:\code\agent\agent-server\src\main\java")
