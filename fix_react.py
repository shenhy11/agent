import os

with open(r'd:\code\agent\agent-server\src\main\java\com\agent\service\agent\ReActEngine.java', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('public static class ToolCallEvent {', '''public static class ToolCallEvent {
    public void setTool(String tool) { this.tool = tool; }
    public void setInput(String input) { this.input = input; }
    public String getTool() { return tool; }
    public String getInput() { return input; }
    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
''')

text = text.replace('public static class ToolResultEvent {', '''public static class ToolResultEvent {
    public void setResult(String result) { this.result = result; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getResult() { return result; }
    public long getDurationMs() { return durationMs; }
    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
''')

text = text.replace('public static class ThinkResult {', '''public static class ThinkResult {
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getThought() { return thought; }
    public void setThought(String thought) { this.thought = thought; }
    public String getFinalAnswer() { return finalAnswer; }
    public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }
    public String getActionInput() { return actionInput; }
    public void setActionInput(String actionInput) { this.actionInput = actionInput; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
''')

with open(r'd:\code\agent\agent-server\src\main\java\com\agent\service\agent\ReActEngine.java', 'w', encoding='utf-8') as f:
    f.write(text)

print("Fixed ReActEngine.java.")
