package theblocklab.bbsmcp.mcp.tools.replay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.replays.ReplayManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

public class RemoveReplayTool extends MCPTool {

    public RemoveReplayTool() {
        super("remove_replay", "按索引删除 Film 中的某个 Replay（演员轨道）。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": {"type": "string",  "description": "目标 Film ID"},
                    "index":  {"type": "integer", "description": "要删除的 Replay 索引"}
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
            ReplayManagerAPI.removeReplay(player, filmId, index);
            return MCPToolResponse.success("Replay[" + index + "] 已成功删除，Film 已同步保存。");
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
