package theblocklab.bbsmcp.mcp.tools.replay.keyframes;

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

public class RemoveKeyframesTool extends MCPTool {

    public RemoveKeyframesTool() {
        super("remove_keyframes", "删除 Replay 中指定通道在指定 tick 区间（或单点）的关键帧数据。不传 channels 则操作全部通道。");
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
                      "description": "（可选）要操作的通道 ID 列表，不传则操作全部通道"
                    },
                    "fromTick": {"type": "number", "description": "（可选）区间起始 tick（含）"},
                    "toTick":   {"type": "number", "description": "（可选）区间结束 tick（含）。不传则删除 fromTick 之后全部帧"},
                    "tick":     {"type": "number", "description": "（可选）精确删除单点 tick（与 fromTick/toTick 互斥）"}
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
                for (var e : arguments.getAsJsonArray("channels")) channels.add(e.getAsString());
            }

            float fromTick  = arguments.has("fromTick") ? arguments.get("fromTick").getAsFloat() : -1;
            float toTick    = arguments.has("toTick")   ? arguments.get("toTick").getAsFloat()   : -1;
            float exactTick = arguments.has("tick")     ? arguments.get("tick").getAsFloat()     : -1;

            ReplayManagerAPI.removeKeyframes(player, filmId, replayIndex, channels, fromTick, toTick, exactTick);
            return MCPToolResponse.success("关键帧删除成功，已同步到客户端。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
