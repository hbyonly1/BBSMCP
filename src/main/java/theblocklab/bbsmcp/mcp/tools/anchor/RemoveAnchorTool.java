package theblocklab.bbsmcp.mcp.tools.anchor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.anchor.AnchorManagerAPI;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/** 删除指定 ID 的锚点 */
public class RemoveAnchorTool extends MCPTool {

    public RemoveAnchorTool() {
        super("remove_anchor", "根据 ID 删除锚点。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "id": {"type": "integer", "description": "锚点 ID"}
                  },
                  "required": ["id"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        int id = requireInt(arguments, "id");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) 
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        try {
            AnchorManagerAPI.INSTANCE.remove(player, id);
            return MCPToolResponse.success(String.format("ID 为 %d 的锚点已删除。", id));
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
