package theblocklab.bbsmcp.mcp.tools.ui;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.mcp.tools.core.MCPTool;
import theblocklab.bbsmcp.mcp.tools.core.MCPToolResponse;
import theblocklab.bbsmcp.network.ServerNetwork;
import theblocklab.bbsmcp.network.ServerRequestBridge;

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
                  }
                }
                """).getAsJsonObject();
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        String filmId = arguments.get("filmId").getAsString();
        
        // 假设我们现在随便找一个玩家（如果在多玩家服务端，可能需要指定 targetPlayer）
        // 这里作为演示，直接取第一个在线玩家
        ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayerList().isEmpty() ? null : server.getPlayerManager().getPlayerList().get(0);
        
        if (targetPlayer == null) {
            return CompletableFuture.completedFuture(MCPToolResponse.error("执行失败", "服务器内没有玩家"));
        }
        if (!FilmManagerAPI.INSTANCE.getFilmsList().contains(filmId)) {
            return CompletableFuture.completedFuture(MCPToolResponse.error("Film: '" + filmId + "' 不存在，打开失败。", "请检查 ID"));
        }

        // 调用刚刚封装的 ServerRequestBridge
        return ServerRequestBridge.request(targetPlayer, ServerNetwork.CLIENT_OPEN_FILM_PANEL, buf -> {
            buf.writeString(filmId);
        }).thenApply(success -> {
            return MCPToolResponse.success("成功打开了 " + filmId + " 的影片面板并得到客户端确认！");
        }).exceptionally(e -> {
            return MCPToolResponse.error("打开面板异常 / 超时", e.getMessage());
        });
    }
}
