package theblocklab.bbsmcp.mcp.tools.anchor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.anchor.AnchorManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/** 查询所有锚点 */
public class GetAnchorsTool extends MCPTool {

    public GetAnchorsTool() {
        super("get_anchors", "查询世界中所有锚点（id + 位置 + 名称 + 描述 + 颜色）。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {"type":"object","properties":{}}
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String json = AnchorManagerAPI.INSTANCE.getAllAsJson();
        return MCPToolResponse.success(json, "可使用 add_anchor / remove_anchor / set_anchor_property 管理锚点。");
    }
}
