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

public class OpenFilmTool extends MCPTool {

    public OpenFilmTool() {
        super("open_film", "打开目标 Film 的面板");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": {
                      "type": "string",
                      "description": "影片ID"
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
                            BBSMCPError.PLAYER_NOT_ONLINE.getHint()));
        }

        // 由 ServerNetwork 统一封装网络发包细节，Tool 层只关心语义
        return ServerNetwork.requestClientOpenFilmPanelPacket(targetPlayer, filmId)
                .thenApply(success -> {
                    return MCPToolResponse.success("成功打开了 " + filmId + " 的影片面板并得到客户端确认！");
                }).exceptionally(e -> {
                    return MCPToolResponse.error("打开面板异常 / 超时", e.getMessage());
                });
    }
}
