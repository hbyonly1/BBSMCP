package theblocklab.bbsmcp.mcp.tools.replay.keyframes;

import com.google.gson.JsonArray;
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

public class AddFormKeyframeTool extends MCPTool {

    public AddFormKeyframeTool() {
        super("add_form_keyframe",
                "向 Replay 的 FormProperties 通道插入单个关键帧。适用于 pose、pose_overlay、lighting 等 Form 专属动画属性。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId":        {"type": "string",  "description": "目标 Film ID"},
                    "replayIndex":   {"type": "integer", "description": "Replay 索引"},
                    "channel":       {"type": "string",  "description": "FormProperties 通道 ID，如 pose、pose_overlay、lighting"},
                    "tick":          {"type": "number",  "description": "关键帧所在时间刻"},
                    "value":         {"type": "string",  "description": "关键帧值。pose/pose_overlay 传 pose JSON 字符串"},
                    "interpolation": {"type": "string",  "description": "（可选）插值类型，如 linear、sine_inout、hermite、bezier"},
                    "interpArgs": {
                      "type": "array",
                      "items": {"type": "number"},
                      "description": "（可选）插值附加参数，最多 4 个数字"
                    }
                  },
                  "required": ["filmId", "replayIndex", "channel", "tick", "value"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int replayIndex = requireInt(arguments, "replayIndex");
        String channel = requireString(arguments, "channel");
        float tick = arguments.get("tick").getAsFloat();
        String value = requireString(arguments, "value");
        String interpolation = arguments.has("interpolation")
                ? arguments.get("interpolation").getAsString()
                : "linear";

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        try {
            FilmManagerAPI.requestClientSaveFilm(player, filmId).join();
            ReplayManagerAPI.addFormKeyframe(player, filmId, replayIndex, channel, tick, value,
                    interpolation, parseInterpArgs(arguments));
            return MCPToolResponse.success(
                    "成功在 Replay[" + replayIndex + "] 的 FormProperties 通道 '" + channel + "' 于 tick=" + tick + " 写入关键帧。",
                    "使用 get_form_properties 可确认插值和 pose 值是否符合预期。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }

    private List<Double> parseInterpArgs(JsonObject arguments) {
        if (!arguments.has("interpArgs")) {
            return null;
        }

        JsonArray array = arguments.getAsJsonArray("interpArgs");
        List<Double> values = new ArrayList<>();
        for (var item : array) {
            values.add(item.getAsDouble());
        }
        return values;
    }
}
