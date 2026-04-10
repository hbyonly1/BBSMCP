package theblocklab.bbsmcp.mcp.core;

import com.google.gson.JsonObject;

/**
 * MCP 资源定义。
 * 包含 URI、名称、描述、MIME 类型和内容文本。
 */
public class MCPResource {
    private final String uri;
    private final String name;
    private final String description;
    private final String mimeType;
    private final String text;

    public MCPResource(String uri, String name, String description, String mimeType, String text) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType;
        this.text = text;
    }

    public String getUri() {
        return uri;
    }

    /**
     * 返回资源列表中的 JSON 定义（不含内容）。
     */
    public JsonObject toJsonDefinition() {
        JsonObject obj = new JsonObject();
        obj.addProperty("uri", uri);
        obj.addProperty("name", name);
        if (description != null) {
            obj.addProperty("description", description);
        }
        if (mimeType != null) {
            obj.addProperty("mimeType", mimeType);
        }
        return obj;
    }

    /**
     * 返回 resources/read 响应中的内容对象。
     */
    public JsonObject toJsonContent() {
        JsonObject content = new JsonObject();
        content.addProperty("uri", uri);
        content.addProperty("mimeType", mimeType != null ? mimeType : "text/plain");
        content.addProperty("text", text != null ? text : "");
        return content;
    }
}
