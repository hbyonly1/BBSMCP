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

public class CloseGUITool extends MCPTool {

    public CloseGUITool() {
        super("close_gui", "关闭客户端当前打开的任何 UI 面板 (相当于玩家按下 ESC 键)");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {}
                }
                """).getAsJsonObject();
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity targetPlayer = getFirstOnlinePlayer(server);
        if (targetPlayer == null) {
            return CompletableFuture.completedFuture(
                    MCPToolResponse.error(
                            BBSMCPError.PLAYER_NOT_ONLINE.format(),
                            BBSMCPError.PLAYER_NOT_ONLINE.getHint()));
        }

        // 调用 ServerNetwork 封装好的方法向客户端发包
        return ServerNetwork.requestClientCloseUIPacket(targetPlayer)
                .thenApply(success -> {
                    return MCPToolResponse.success("成功发送关闭指令，客户端 UI 已退出！");
                }).exceptionally(e -> {
                    return MCPToolResponse.error("关闭 UI 异常 / 超时", e.getMessage());
                });
    }
}
