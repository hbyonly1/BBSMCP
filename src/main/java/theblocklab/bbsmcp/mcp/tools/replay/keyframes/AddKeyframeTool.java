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

public class AddKeyframeTool extends MCPTool {

    public AddKeyframeTool() {
        super("add_keyframe", "向 Replay 的指定通道插入单个关键帧。如需批量写入请优先使用 batch_add_keyframes。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser
                .parseString(
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "filmId":      {"type": "string",  "description": "目标 Film ID"},
                                    "replayIndex": {"type": "integer", "description": "Replay 索引"},
                                    "channel":     {"type": "string",  "description": "通道 ID，如 x/yaw/item_main_hand，参考 get_replay_schema"},
                                    "tick":        {"type": "number",  "description": "关键帧所在时间刻"},
                                    "value":       {"type": "string",  "description": "关键帧值，数值通道填数字串，物品通道填物品 JSON 字符串"}
                                  },
                                  "required": ["filmId", "replayIndex", "channel", "tick", "value"]
                                }
                                """)
                .getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int replayIndex = requireInt(arguments, "replayIndex");
        String channel = requireString(arguments, "channel");
        float tick = arguments.get("tick").getAsFloat();
        String value = requireString(arguments, "value");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null)
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        try {
            FilmManagerAPI.requestClientSaveFilm(player, filmId).join();
            ReplayManagerAPI.addKeyframe(player, filmId, replayIndex, channel, tick, value);
            return MCPToolResponse
                    .success("成功在 Replay[" + replayIndex + "] 通道 '" + channel + "' 的 tick=" + tick + " 处插入关键帧。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
