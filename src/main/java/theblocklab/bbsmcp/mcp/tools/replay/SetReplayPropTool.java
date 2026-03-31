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

public class SetReplayPropTool extends MCPTool {

    public SetReplayPropTool() {
        super("set_replay_prop", "修改 Replay 的基本属性，所有属性均为可选，只有传入的才会被修改。设置 fp=true 时会自动清除其他 Replay 的 fp 标志（全局唯一）。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId":  {"type": "string",  "description": "目标 Film ID"},
                    "index":   {"type": "integer", "description": "Replay 索引"},
                    "enabled": {"type": "boolean", "description": "（可选）是否启用此轨道"},
                    "label":   {"type": "string",  "description": "（可选）UI 中显示的标签名"},
                    "nameTag": {"type": "string",  "description": "（可选）角色头顶显示的名字"},
                    "actor":   {"type": "boolean", "description": "（可选）true=在世界中生成真实实体"},
                    "fp":      {"type": "boolean", "description": "（可选）true=映射到第一人称玩家，全局唯一"},
                    "looping": {"type": "integer", "description": "（可选）循环周期 tick，0 表示不循环"}
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

            Boolean enabled  = arguments.has("enabled")  ? arguments.get("enabled").getAsBoolean()  : null;
            String  label    = arguments.has("label")    ? arguments.get("label").getAsString()    : null;
            String  nameTag  = arguments.has("nameTag")  ? arguments.get("nameTag").getAsString()  : null;
            Boolean actor    = arguments.has("actor")    ? arguments.get("actor").getAsBoolean()    : null;
            Boolean fp       = arguments.has("fp")       ? arguments.get("fp").getAsBoolean()       : null;
            Integer looping  = arguments.has("looping")  ? arguments.get("looping").getAsInt()      : null;

            ReplayManagerAPI.setReplayProps(player, filmId, index, enabled, label, nameTag, actor, fp, looping);
            return MCPToolResponse.success("Replay[" + index + "] 属性已更新并同步。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
