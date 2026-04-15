package theblocklab.bbsmcp.mcp.tools.replay.keyframes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.replays.ReplayManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

import java.util.ArrayList;
import java.util.List;

public class RemoveFormKeyframesTool extends MCPTool {

    public RemoveFormKeyframesTool() {
        super("remove_form_keyframes",
                "删除 Replay 的 FormProperties 通道在指定 tick 区间或单点的关键帧。不传 channels 则操作全部 FormProperties 通道。");
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
                      "description": "（可选）要操作的 FormProperties 通道 ID 列表，不传则操作全部"
                    },
                    "fromTick": {"type": "number", "description": "（可选）区间起始 tick（含）"},
                    "toTick":   {"type": "number", "description": "（可选）区间结束 tick（含）"},
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
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        try {
            FilmManagerAPI.requestClientSaveFilm(player, filmId).join();

            List<String> channels = null;
            if (arguments.has("channels")) {
                channels = new ArrayList<>();
                for (var e : arguments.getAsJsonArray("channels")) {
                    channels.add(e.getAsString());
                }
            }

            float fromTick = arguments.has("fromTick") ? arguments.get("fromTick").getAsFloat() : -1;
            float toTick = arguments.has("toTick") ? arguments.get("toTick").getAsFloat() : -1;
            float exactTick = arguments.has("tick") ? arguments.get("tick").getAsFloat() : -1;

            ReplayManagerAPI.removeFormKeyframes(player, filmId, replayIndex, channels, fromTick, toTick, exactTick);
            return MCPToolResponse.success("FormProperties 关键帧删除成功，已同步到客户端。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
