package theblocklab.bbsmcp.mcp.tools.anchor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.anchor.Anchor;
import theblocklab.bbsmcp.anchor.AnchorManager;
import theblocklab.bbsmcp.anchor.AnchorServerNetwork;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/** 在指定坐标添加锚点 */
public class AddAnchorTool extends MCPTool {

    public AddAnchorTool() {
        super("add_anchor", "在指定世界坐标添加一个锚点，可附加名称、描述和颜色，并返回 id。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "x": {"type": "integer", "description": "X 坐标"},
                    "y": {"type": "integer", "description": "Y 坐标"},
                    "z": {"type": "integer", "description": "Z 坐标"},
                    "name": {"type": "string", "description": "锚点名称（可选）"},
                    "description": {"type": "string", "description": "锚点描述（可选）"},
                    "color": {"type": "string", "description": "颜色 hex（可选，例如 #FF8833）"}
                  },
                  "required": ["x", "y", "z"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        int x = requireInt(arguments, "x");
        int y = requireInt(arguments, "y");
        int z = requireInt(arguments, "z");
        String name = arguments.has("name") ? arguments.get("name").getAsString()
                : String.format("(%d,%d,%d)", x, y, z);
        String description = arguments.has("description") ? arguments.get("description").getAsString() : "";
        String color = arguments.has("color") ? arguments.get("color").getAsString() : "#4488FF";

        BlockPos pos = new BlockPos(x, y, z);
        Anchor anchor = AnchorManager.INSTANCE.create(pos, name, description, color);

        // 同步给在线玩家
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) 
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        
        AnchorServerNetwork.sendAnchorUpdatePacket(player, anchor);
        
        return MCPToolResponse.success(String.format("锚点「%s」已创建（ID: %d），位于 (%d,%d,%d)。", name, anchor.id, x, y, z));
    }
}
