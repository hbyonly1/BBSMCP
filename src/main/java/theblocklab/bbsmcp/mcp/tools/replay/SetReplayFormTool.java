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

public class SetReplayFormTool extends MCPTool {

    public SetReplayFormTool() {
        super("set_replay_form", "设置 Replay 的外观形式 (Form)。支持通过 mobId 设置原版生物，或通过 formJson 设置复杂表单。两者至少提供其一。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId":   {"type": "string",  "description": "目标 Film ID"},
                    "index":    {"type": "integer", "description": "Replay 索引"},
                    "mobId":    {"type": "string",  "description": "（可选）Minecraft 实体 ID，例如 'minecraft:pig'。若提供此项，将按原版生物设置。"},
                    "formJson": {"type": "string",  "description": "（可选）Form 的 JSON 字符串（BaseType 映射格式）。"}
                  },
                  "required": ["filmId", "index"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int index = requireInt(arguments, "index");
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        try {
            ClipManagerAPI.requestSaveFilmAsync(player, filmId).join();

            if (arguments.has("mobId")) {
                String mobId = arguments.get("mobId").getAsString();
                ReplayManagerAPI.setReplayVanillaMob(player, filmId, index, mobId);
                return MCPToolResponse.success("Replay[" + index + "] 外观已设置为原版生物: " + mobId);
            } else if (arguments.has("formJson")) {
                String formJson = arguments.get("formJson").getAsString();
                ReplayManagerAPI.setReplayForm(player, filmId, index, formJson);
                return MCPToolResponse.success("Replay[" + index + "] 外观已根据 JSON 更新。");
            } else {
                return MCPToolResponse.error("参数缺失", "必须提供 'mobId' 或 'formJson' 其中之一。");
            }
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
