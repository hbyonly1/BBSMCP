package theblocklab.bbsmcp.mcp.tools.ui;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.network.ServerNetwork;

public class OpenReplayEditorTool extends MCPTool {
    public OpenReplayEditorTool() {
        super("open_replay_editor", "打开 Replay 编辑器");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": {
                      "type": "string",
                      "description": "影片 ID"
                    },
                    "replayIndex": {
                      "type": "integer",
                      "description": "Replay 索引"
                    }
                  },
                  "required": ["filmId", "replayIndex"]
                }
                """).getAsJsonObject();
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int replayIndex = requireInt(arguments, "replayIndex");

        ServerPlayerEntity targetPlayer = getFirstOnlinePlayer(server);
        if (targetPlayer == null) {
            return CompletableFuture.completedFuture(
                    MCPToolResponse.error(
                            BBSMCPError.PLAYER_NOT_ONLINE.format(),
                            BBSMCPError.PLAYER_NOT_ONLINE.getHint()));
        }

        if (!theblocklab.bbsmcp.film.FilmManagerAPI.INSTANCE.hasFilm(filmId)) {
            return CompletableFuture.completedFuture(
                    MCPToolResponse.error(
                            BBSMCPError.FILM_NOT_FOUND.format(filmId),
                            BBSMCPError.FILM_NOT_FOUND.getHint()));
        }

        // 由 ServerNetwork 统一封装网络发包细节，Tool 层只关心语义
        return ServerNetwork.requestClientOpenReplayEditorPacket(targetPlayer, filmId, replayIndex)
                .thenApply(success -> {
                    return MCPToolResponse.success("成功打开了 Replay 编辑器并得到客户端确认！");
                }).exceptionally(e -> {
                    return MCPToolResponse.error("打开 Replay 编辑器异常 / 超时", e.getMessage());
                });
    }
}
