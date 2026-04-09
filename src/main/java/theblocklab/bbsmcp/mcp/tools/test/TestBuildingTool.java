package theblocklab.bbsmcp.mcp.tools.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.mcp.tools.building.LoadBuildingTool;

/**
 * 供测试使用的 MCP 工具，自动生成一个小屋 JSON 并调用 LoadBuildingTool 进行加载。
 */
public class TestBuildingTool extends MCPTool {

    public TestBuildingTool() {
        super("test_building", "创建一个小木屋并调用 load_building 工具，供测试建筑系统使用。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
            {
              "type": "object",
              "properties": {}
            }
            """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String houseJson = """
            {
              "name": "测试小屋",
              "fills": [
                [0, 0, 0, 4, 0, 4, "oak_planks"],
                [0, 4, 0, 4, 4, 4, "oak_planks"],
                [0, 1, 0, 4, 3, 0, "stone_bricks"],
                [0, 1, 4, 4, 3, 4, "stone_bricks"],
                [0, 1, 1, 0, 3, 3, "stone_bricks"],
                [4, 1, 1, 4, 3, 3, "stone_bricks"]
              ],
              "blocks": [
                [2, 1, 0, "air"],
                [2, 2, 0, "air"],
                [2, 1, 4, "glass_pane"],
                [2, 2, 4, "glass_pane"]
              ],
              "anchors": [
                [2, 1, 2, "室内中心", "位于小屋中央的测试锚点"]
              ]
            }
            """;

        LoadBuildingTool loadTool = new LoadBuildingTool();
        JsonObject loadArgs = new JsonObject();
        loadArgs.addProperty("blueprint_json", houseJson);

        return loadTool.execute(loadArgs, server);
    }
}
