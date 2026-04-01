package theblocklab.bbsmcp.mcp.tools.anchor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.anchor.Anchor;
import theblocklab.bbsmcp.anchor.AnchorManager;
import theblocklab.bbsmcp.anchor.AnchorServerNetwork;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/** 更新锚点的名称、描述和/或颜色 */
public class SetAnchorPropertyTool extends MCPTool {

    public SetAnchorPropertyTool() {
        super("set_anchor_property", "更新指定 ID 锚点的名称、描述或颜色（传入 null 表示不修改该字段）。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "id": {"type": "integer", "description": "锚点 ID"},
                    "name": {"type": "string", "description": "新名称（可选）"},
                    "description": {"type": "string", "description": "新描述（可选）"},
                    "color": {"type": "string", "description": "新颜色 hex（可选，例如 #FF8833）"}
                  },
                  "required": ["id"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        int id = requireInt(arguments, "id");
        String name = arguments.has("name") && !arguments.get("name").isJsonNull()
                ? arguments.get("name").getAsString() : null;
        String description = arguments.has("description") && !arguments.get("description").isJsonNull()
                ? arguments.get("description").getAsString() : null;
        String color = arguments.has("color") && !arguments.get("color").isJsonNull()
                ? arguments.get("color").getAsString() : null;

        boolean updated = AnchorManager.INSTANCE.update(id, name, description, color);
        if (!updated) {
            return MCPToolResponse.error(
                    String.format("ID 为 %d 的锚点不存在。", id),
                    "请先使用 get_anchors 确认 ID 是否正确。");
        }

        // 同步给在线玩家
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) 
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        
        Anchor anchor = AnchorManager.INSTANCE.get(id);
        AnchorServerNetwork.sendAnchorUpdatePacket(player, anchor);

        return MCPToolResponse.success(String.format("ID 为 %d 的锚点属性已更新。", id));
    }
}
