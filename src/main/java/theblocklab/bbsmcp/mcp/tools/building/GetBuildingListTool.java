package theblocklab.bbsmcp.mcp.tools.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.building.BuildingFileLoader;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

import java.util.List;
import java.util.stream.Collectors;

public class GetBuildingListTool extends MCPTool {

    public GetBuildingListTool() {
        super("get_building_list", "获取 config/bbsmcp/buildings 目录下所有已保存建筑蓝图的名称和绝对路径列表。");
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
        try {
            List<BuildingFileLoader.BuildingFileInfo> buildings = BuildingFileLoader.listBuildings();
            if (buildings.isEmpty()) {
                return MCPToolResponse.success("当前暂无已保存的建筑蓝图。");
            }

            String listString = buildings.stream()
                .map(info -> String.format("%s -> %s", info.buildingName(), info.fullPath()))
                .collect(Collectors.joining("\n"));

            return MCPToolResponse.success(
                "包含以下建筑蓝图:\n" + listString,
                "AI 可直接使用这些绝对路径读取JSON，也可将名称传给 load_building 的 buildingName 参数进行重载。"
            );
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
