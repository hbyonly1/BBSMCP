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

public class SetCursorTool extends MCPTool {

    public SetCursorTool() {
        super("set_cursor", "设置当前正在编辑/播放的影片的游标 (Cursor/Tick) 位置，客户端画面将自动同步");
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
                    },
                    "tick": {
                      "type": "integer",
                      "description": "要跳转到的目标时间刻 (Tick/Cursor)"
                    }
                  },
                  "required": ["filmId", "tick"]
                }
                """).getAsJsonObject();
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int tick = requireInt(arguments, "tick");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return CompletableFuture.completedFuture(MCPToolResponse.error(
                    BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint()));
        }

        return ServerNetwork.requestClientSetCursorPacket(player, filmId, tick)
                .thenApply(success -> MCPToolResponse.success(
                        "客户端游标已成功跳转到 " + tick + " 帧", 
                        "画面应当已同步"))
                .exceptionally(e -> MCPToolResponse.error("设置游标失败", e.getMessage()));
    }
}
