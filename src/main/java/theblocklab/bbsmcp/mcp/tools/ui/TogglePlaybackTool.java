package theblocklab.bbsmcp.mcp.tools.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import java.util.concurrent.CompletableFuture;

public class TogglePlaybackTool extends MCPTool {

    public TogglePlaybackTool() {
        super("toggle_playback", "通过命令客户端面板触发，令游戏客户端开始/暂停播放指定的影片");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": {
                      "type": "string",
                      "description": "要播放的影片 ID"
                    }
                  },
                  "required": ["filmId"]
                }
                """).getAsJsonObject();
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        
        ServerPlayerEntity targetPlayer = getFirstOnlinePlayer(server);
        if (targetPlayer == null) {
            return CompletableFuture.completedFuture(
                MCPToolResponse.error(
                    BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint()
                )
            );
        }

        if (!theblocklab.bbsmcp.film.FilmManagerAPI.INSTANCE.hasFilm(filmId)) {
            return CompletableFuture.completedFuture(
                MCPToolResponse.error(
                    BBSMCPError.FILM_NOT_FOUND.format(filmId),
                    BBSMCPError.FILM_NOT_FOUND.getHint()
                )
            );
        }

        // 完美使用专门支持事件驱动和轮询回执的 TogglePlayback 方法
        return theblocklab.bbsmcp.network.ServerNetwork.requestClientTogglePlaybackPacket(targetPlayer, filmId)
                .thenApply(success -> {
                    return MCPToolResponse.success("✅ 播放完毕！客户端已发回结束信号。影片: " + filmId);
                }).exceptionally(e -> {
                    return MCPToolResponse.error("播放指令发送异常 / 客户端回应超时", e.getMessage());
                });
    }
}
