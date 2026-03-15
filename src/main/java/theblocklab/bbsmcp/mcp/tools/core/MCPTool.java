package theblocklab.bbsmcp.mcp.tools.core;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

/**
 * 代表一个独立的 MCP 工具
 * 将工具的名称、描述、JSON Schema 以及执行逻辑高度内聚在一个类中。
 */
public abstract class MCPTool {
    private final String name;
    private final String description;

    public MCPTool(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return 返回该工具的输入参数 Schema，格式为标准的 JSON Schema 对象
     */
    public abstract JsonObject getInputSchema();

    /**
     * 自动打包为遵守 MCP 规范的 Tool 定制 JSON
     * @return 注册到 tools/list 时的格式
     */
    public JsonObject toJsonDefinition() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", this.name);
        obj.addProperty("description", this.description);
        obj.add("inputSchema", this.getInputSchema());
        return obj;
    }

    /**
     * 执行具体的工具业务逻辑
     * @param arguments 调用工具时传入的参数
     * @param server Minecraft 服务端上下文
     * @return 统一格式的响应
     */
    public abstract MCPToolResponse execute(JsonObject arguments, MinecraftServer server) throws Exception;
}
