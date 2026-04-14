import os
import re

def clean_file(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    # for ReActEngine
    if "ReActEngine.java" in filepath:
        # replace the messy ThinkResult
        content = re.sub(r'@Data\s*@lombok\.Data\s*public static class ThinkResult \{.*?public static class ToolCallEvent',
                         '@lombok.Data\n    public static class ThinkResult {\n        private String thought;\n    }\n\n    @lombok.Data\n    public static class ToolCallEvent',
                         content, flags=re.DOTALL)
        content = re.sub(r'@Data\s*@lombok\.Data\s*public static class ToolCallEvent \{.*?public static class ToolResultEvent',
                         '@lombok.Data\n    public static class ToolCallEvent {\n        private String tool;\n        private String input;\n    }\n\n    @lombok.Data\n    public static class ToolResultEvent',
                         content, flags=re.DOTALL)
        content = re.sub(r'@Data\s*@lombok\.Data\s*public static class ToolResultEvent \{.*?public Flux',
                         '@lombok.Data\n    public static class ToolResultEvent {\n        private String result;\n        private long durationMs;\n    }\n\n    /**\n     * 运行 ReAct 循环并作为 Flux 流返回 SSE 事件\n     */\n    public Flux',
                         content, flags=re.DOTALL)

    # for AiMetricsService
    if "AiMetricsService.java" in filepath:
        content = re.sub(r'@lombok\.Data\s*public static class MetricsReport \{.*?\}',
                         '@lombok.Data\n    public static class MetricsReport {\n        private int totalRequests;\n        private int totalTokens;\n        private long averageLatencyMs;\n        private int toolCallsSuccess;\n        private int toolCallsFailed;\n    }\n',
                         content, flags=re.DOTALL)

    # for UserProfileService
    if "UserProfileService.java" in filepath:
        content = re.sub(r'@lombok\.Data\s*public static class UserProfile \{.*?\}',
                         '@lombok.Data\n    public static class UserProfile {\n        private String userId;\n        private java.util.List<String> tags;\n    }\n',
                         content, flags=re.DOTALL)

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)

clean_file(r"d:\code\agent\agent-server\src\main\java\com\agent\service\agent\ReActEngine.java")
clean_file(r"d:\code\agent\agent-server\src\main\java\com\agent\service\agent\AiMetricsService.java")
clean_file(r"d:\code\agent\agent-server\src\main\java\com\agent\service\agent\UserProfileService.java")
