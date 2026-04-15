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

public class BatchAddFormKeyframesTool extends MCPTool {

    public BatchAddFormKeyframesTool() {
        super("batch_add_form_keyframes",
                "批量向 Replay 的 FormProperties 通道写入关键帧。适合一次性写入 pose / pose_overlay 多帧动画，并支持逐帧插值。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId":        {"type": "string",  "description": "目标 Film ID"},
                    "replayIndex":   {"type": "integer", "description": "Replay 索引"},
                    "interpolation": {"type": "string",  "description": "（可选）默认插值类型，如 linear、sine_inout、hermite"},
                    "keyframes": {
                      "type": "object",
                      "description": "各 FormProperties 通道的关键帧数组。key 为通道 ID（如 pose），value 为 [{tick,value,interpolation?,interpArgs?}...]",
                      "additionalProperties": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "tick":          {"type": "number"},
                            "value":         {"type": "string"},
                            "interpolation": {"type": "string"},
                            "interpArgs": {
                              "type": "array",
                              "items": {"type": "number"}
                            }
                          },
                          "required": ["tick", "value"]
                        }
                      }
                    }
                  },
                  "required": ["filmId", "replayIndex", "keyframes"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int replayIndex = requireInt(arguments, "replayIndex");
        JsonObject channelFrames = arguments.getAsJsonObject("keyframes");
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
            ReplayManagerAPI.batchAddFormKeyframes(player, filmId, replayIndex, channelFrames, interpolation);

            int totalFrames = channelFrames.keySet().stream()
                    .mapToInt(k -> channelFrames.getAsJsonArray(k).size())
                    .sum();
            return MCPToolResponse.success(
                    "成功向 Replay[" + replayIndex + "] 的 FormProperties 写入 " + channelFrames.size()
                            + " 个通道，共 " + totalFrames + " 个关键帧。",
                    "使用 get_form_properties 查询写入结果，或直接 play_film 预览骨骼动画。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
