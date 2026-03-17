package theblocklab.bbsmcp.mcp.tools.clip;

import theblocklab.bbsmcp.mcp.tools.core.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.film.clips.ClipFileLoader;

public class LoadClipsFromFileTool extends MCPTool {

    public LoadClipsFromFileTool() {
        super("load_clips", "从服务器配置目录的 JSON 文件加载一组 Clips 镜头到目标 Film 中");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "playerName": { "type": "string", "description": "用来接收广播和反馈信息的玩家 IGN" },
                    "filmId": { "type": "string", "description": "目标 Film 的 ID" },
                    "filepath": { "type": "string", "description": "相对于 bbsmcp/clips/ 目录的 json 文件路径，如 'example.json'" }
                  },
                  "required": ["playerName", "filmId", "filepath"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String playerName = arguments.get("playerName").getAsString();
        String filmId = arguments.get("filmId").getAsString();
        String filepath = arguments.get("filepath").getAsString();

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        if (player == null) {
            return MCPToolResponse.error("执行失败：找不到玩家 '" + playerName + "'", "请确认玩家是否在线并拼写正确。");
        }

        ClipFileLoader.loadClipsFromFile(player, filmId, filepath);
        return MCPToolResponse.success("成功触发 Clip 加载命令，目标文件: '" + filepath + "'", "请留意游戏内提示，如果 JSON 解析失败会有具体原因");
    }
}
