package theblocklab.bbsmcp.mcp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 提示词提供者的基类。
 */
public abstract class MCPPromptProvider {
    protected final Map<String, MCPPrompt> prompts = new HashMap<>();

    protected void registerPrompt(MCPPrompt prompt) {
        if (prompt != null) {
            prompts.put(prompt.getName(), prompt);
        }
    }

    /**
     * 获取此提供者拥有的所有提示词定义列表。
     */
    public JsonArray getPromptDefinitions() {
        JsonArray array = new JsonArray();
        for (MCPPrompt prompt : prompts.values()) {
            array.add(prompt.toJsonDefinition());
        }
        return array;
    }

    /**
     * 根据名称和参数获取提示词的完整消息内容。
     * 遵循 MCP prompts/get 响应格式。
     */
    public JsonObject getPrompt(String name, JsonObject arguments) {
        MCPPrompt prompt = prompts.get(name);
        if (prompt == null) {
            return null;
        }

        // 目前简单实现：固定返回一条用户角色的消息。
        // 未来若需支持参数替换 {{arg}}，可在此处对 template 进行处理。
        String contentText = prompt.getTemplate();

        JsonObject result = new JsonObject();
        JsonArray messages = new JsonArray();

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");

        JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", contentText);

        message.add("content", content);
        messages.add(message);

        result.add("messages", messages);
        return result;
    }
}
