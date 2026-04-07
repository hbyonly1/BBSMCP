package theblocklab.bbsmcp.mcp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * MCP 提示词定义。
 * 包含名称、描述、参数定义以及核心模板文本。
 */
public class MCPPrompt {
    private final String name;
    private final String description;
    private final JsonArray arguments;
    private final String template;

    public MCPPrompt(String name, String description, String template) {
        this(name, description, null, template);
    }

    public MCPPrompt(String name, String description, JsonArray arguments, String template) {
        this.name = name;
        this.description = description;
        this.arguments = arguments;
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }

    public JsonObject toJsonDefinition() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        if (description != null) {
            obj.addProperty("description", description);
        }
        if (arguments != null) {
            obj.add("arguments", arguments);
        }
        return obj;
    }
}
