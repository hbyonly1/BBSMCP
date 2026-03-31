package theblocklab.bbsmcp.mcp.tools.replay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
import theblocklab.bbsmcp.film.replays.ReplayManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

import java.util.ArrayList;
import java.util.List;

public class GetKeyframesTool extends MCPTool {

    public GetKeyframesTool() {
        super("get_keyframes", "精确查询 Replay 关键帧数据，支持按通道列表与 tick 区间过滤。前置强制保存以获取最新数据。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId":      {"type": "string",  "description": "目标 Film ID"},
                    "replayIndex": {"type": "integer", "description": "Replay 索引"},
                    "channels": {
                      "type": "array",
                      "items": {"type": "string"},
                      "description": "（可选）要查询的通道 ID 列表，如 [x, yaw]，不传则返回全部通道"
                    },
                    "fromTick": {"type": "number", "description": "（可选）起始 tick（含）"},
                    "toTick":   {"type": "number", "description": "（可选）结束 tick（含）"}
                  },
                  "required": ["filmId", "replayIndex"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int replayIndex = requireInt(arguments, "replayIndex");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        try {
            ClipManagerAPI.requestSaveFilmAsync(player, filmId).join();

            List<String> channels = null;
            if (arguments.has("channels")) {
                channels = new ArrayList<>();
                for (var e : arguments.getAsJsonArray("channels")) {
                    channels.add(e.getAsString());
                }
            }
            float fromTick = arguments.has("fromTick") ? arguments.get("fromTick").getAsFloat() : -1;
            float toTick   = arguments.has("toTick")   ? arguments.get("toTick").getAsFloat()   : -1;

            String result = ReplayManagerAPI.getKeyframes(filmId, replayIndex, channels, fromTick, toTick);
            return MCPToolResponse.success(result);
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
