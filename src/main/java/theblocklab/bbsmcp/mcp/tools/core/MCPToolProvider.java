package theblocklab.bbsmcp.mcp.tools.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MCP 工具提供者的基类。
 * 内部维护了一个工具字典。继承此类的 Provider（例如 FilmManagerMcpTools）
 * 只需要在构造函数中调用 registerTool 即可。
 */
public abstract class MCPToolProvider {

    protected final Map<String, MCPTool> tools = new HashMap<>();

    /**
     * 注册子工具
     */
    protected void registerTool(MCPTool tool) {
        if (tool != null) {
            tools.put(tool.getName(), tool);
        }
    }

    /**
     * 向 MCP 注册当前工具提供的可用工具列表
     * @return 包含该 Provider 所有可用工具 Schema 的 JSON 数组
     */
    public JsonArray getToolDefinitions() {
        JsonArray array = new JsonArray();
        for (MCPTool tool : tools.values()) {
            array.add(tool.toJsonDefinition());
        }
        return array;
    }
    
    /**
     * 执行具体的工具调用（异步）。
     * 直接通过字典查找到对应的 McpTool 对象并调用其 executeAsync。
     */
    public CompletableFuture<MCPToolResponse> executeToolAsync(String toolName, JsonObject arguments, MinecraftServer server) throws Exception {
        MCPTool tool = tools.get(toolName);
        if (tool != null) {
            return tool.executeAsync(arguments, server);
        }
        return null; // 交给 router 中的下一个 Provider
    }

}
