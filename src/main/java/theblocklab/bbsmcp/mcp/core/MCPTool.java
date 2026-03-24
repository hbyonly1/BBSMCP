package theblocklab.bbsmcp.mcp.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;

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
     * 
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
     * 执行具体的工具业务逻辑（同步）
     * 默认被 executeAsync 调用
     * 
     * @param arguments 调用工具时传入的参数
     * @param server    Minecraft 服务端上下文
     * @return 统一格式的响应
     */
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) throws Exception {
        throw new UnsupportedOperationException("This tool must implement either execute or executeAsync");
    }

    /**
     * 执行具体的工具业务逻辑（异步）
     * 如果工具涉及到网络发包等待，请重写此方法
     * 
     * @param arguments 调用工具时传入的参数
     * @param server    Minecraft 服务端上下文
     * @return 包含统一响应格式的 CompletableFuture
     */
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server)
            throws Exception {
        // 默认实现将其包装在一个已完成的 CompletableFuture 中，保持向后兼容
        return CompletableFuture.completedFuture(this.execute(arguments, server));
    }

    // ── 基类工具方法 ──────────────────────────────────────────────────────────

    /**
     * 获取服务器内第一个在线玩家，若无玩家在线则返回 null。
     */
    protected ServerPlayerEntity getFirstOnlinePlayer(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        return players.isEmpty() ? null : players.get(0);
    }

    /**
     * 从参数 JSON 中取 String 类型必填字段。
     * 若字段缺失或为 null 则抛出 BbsException（MISSING_PARAM）。
     */
    protected String requireString(JsonObject args, String key) {
        if (!args.has(key) || args.get(key).isJsonNull()) {
            throw new BBSMCPException(BBSMCPError.MISSING_PARAM, key);
        }
        return args.get(key).getAsString();
    }

    /**
     * 从参数 JSON 中取 int 类型必填字段。
     * 若字段缺失或为 null 则抛出 BbsException（MISSING_PARAM）。
     */
    protected int requireInt(JsonObject args, String key) {
        if (!args.has(key) || args.get(key).isJsonNull()) {
            throw new BBSMCPException(BBSMCPError.MISSING_PARAM, key);
        }
        return args.get(key).getAsInt();
    }
}
