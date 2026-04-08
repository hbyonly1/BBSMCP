package theblocklab.bbsmcp.mcp.tools.film;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * 触发指定 Tick 的截图工具
 */
public class CaptureScreenshotTool extends MCPTool {

    public CaptureScreenshotTool() {
        super("capture_screenshot", "请求客户端在指定的 Tick 截图记录画面。截图将保存至客户端。注意：如果需要高精度的 Replay 动作，建议从头播放；若仅需快速核对静态场景或坐标，可配合 start_tick 使用。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "target_tick": {
                      "type": "integer",
                      "description": "要进行截图的特定 tick"
                    },
                    "start_tick": {
                      "type": "integer",
                      "description": "可选。跳转到的起始 tick 并播放。注意：使用此选项虽能快速截图，但由于 BBS 的 Replay 渲染特性，非自然播放可能导致演员动作（动作关键帧）不准确。除非是静态场景或快速核对位置，否则建议留空以保证画面表现。"
                    }
                  },
                  "required": ["target_tick"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        int targetTick = arguments.get("target_tick").getAsInt();
        int startTick = arguments.has("start_tick") ? arguments.get("start_tick").getAsInt() : -1;
        
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        try {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String filename = String.format("bbs_mcp_t%d_%s.png", targetTick, timestamp);
            FilmManagerAPI.captureScreenshot(player, filename, targetTick, startTick).get();
            String path = "config/bbsmcp/screenshot/" + filename;
            return MCPToolResponse.success("截图成功！", "文件已保存至: " + path);
        } catch (Exception e) {
            return MCPToolResponse.error("截图请求失败: " + e.getMessage(), "请确保客户端已打开 Film 面板并处于可点击状态。");
        }
    }
}
