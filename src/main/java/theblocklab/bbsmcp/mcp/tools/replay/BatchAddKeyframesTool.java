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

public class BatchAddKeyframesTool extends MCPTool {

    public BatchAddKeyframesTool() {
        super("batch_add_keyframes",
                "【核心工具】一次调用向 Replay 多个通道写入多个关键帧。AI 使用此工具描述人物一段动作，效率远高于逐帧调用。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId":        {"type": "string",  "description": "目标 Film ID"},
                    "replayIndex":   {"type": "integer", "description": "Replay 索引"},
                    "interpolation": {"type": "string",  "description": "（可选）默认插值类型，如 LINEAR、HERMITE，默认 LINEAR"},
                    "keyframes": {
                      "type": "object",
                      "description": "各通道的关键帧数组，key 为 channel ID（如 x/z/yaw），value 为 [{tick, value}...] 数组。value 统一用字符串表示，数值通道填数字串，物品通道填物品 JSON 字符串",
                      "additionalProperties": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "properties": {
                            "tick":  {"type": "number"},
                            "value": {"type": "string"}
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
                ? arguments.get("interpolation").getAsString() : "LINEAR";

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        try {
            // 前置同步：写入前确保内存与客户端对齐
            ClipManagerAPI.requestSaveFilmAsync(player, filmId).join();

            ReplayManagerAPI.batchAddKeyframes(player, filmId, replayIndex, channelFrames, interpolation);

            int totalFrames = channelFrames.keySet().stream()
                    .mapToInt(k -> channelFrames.getAsJsonArray(k).size()).sum();
            return MCPToolResponse.success(
                    "成功向 Replay[" + replayIndex + "] 写入 " + channelFrames.size() + " 个通道，共 " + totalFrames + " 个关键帧。",
                    "使用 get_keyframes 可查询写入结果，使用 play_film 预览效果。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
