package theblocklab.bbsmcp.mcp.tools.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.network.ServerNetwork;

import java.util.concurrent.CompletableFuture;

public class GetCursorTool extends MCPTool {

    public GetCursorTool() {
        super("get_cursor", "获取客户端当前影片界面正在停靠的游标 (Cursor/Tick) 位置");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": {
                      "type": "string",
                      "description": "目标 Film 的 ID，这必须是客户端当前已打开在界面上的电影"
                    }
                  },
                  "required": ["filmId"]
                }
                """).getAsJsonObject();
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return CompletableFuture.completedFuture(MCPToolResponse.error(
                    BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint()));
        }

        // 调用刚才新发明的含有通配载荷的 requestClientGetCursorPacket
        return ServerNetwork.requestClientGetCursorPacket(player, filmId)
                .thenApply(jsonString -> MCPToolResponse.success(jsonString))
                .exceptionally(e -> MCPToolResponse.error("获取游标失败", e.getMessage()));
    }
}
