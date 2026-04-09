package theblocklab.bbsmcp.mcp.tools.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.building.BuildingBlueprint;
import theblocklab.bbsmcp.building.BuildingRepository;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.network.ServerNetwork;

/**
 * MCP 工具：加载 AI 生成的建筑蓝图 JSON，暂存到内存并下发给客户端激活虚影预览。
 * 工具名：load_building
 */
public class LoadBuildingTool extends MCPTool {

    public LoadBuildingTool() {
        super("load_building",
            "加载 AI 生成的建筑蓝图 JSON，发送给客户端渲染绿色线框虚影。" +
            "玩家拿起建筑魔杖后可在前方预览建筑，左键旋转方向，右键确认放置。" +
            "blueprint_json 使用极简数组格式：" +
            "blocks=[[dx,dy,dz,\"id\"],...], fills=[[x1,y1,z1,x2,y2,z2,\"id\"],...], anchors=[[dx,dy,dz,\"name\",\"desc\"],...]。" +
            "方块 id 无需 minecraft: 前缀（系统自动添加）；包含 ':' 则原样使用。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
            {
              "type": "object",
              "properties": {
                "blueprint_json": {
                  "type": "string",
                  "description": "建筑蓝图 JSON 字符串（极简数组格式）"
                }
              },
              "required": ["blueprint_json"]
            }
            """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String json = requireString(arguments, "blueprint_json");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        BuildingBlueprint blueprint;
        try {
            blueprint = new BuildingBlueprint(json);
        } catch (Exception e) {
            return MCPToolResponse.error(
                "蓝图 JSON 解析失败: " + e.getMessage(),
                "请检查 blueprint_json 格式是否符合极简数组规范。"
            );
        }

        // 估算 fills 展开总方块数
        int fillEstimate = estimateFillBlocks(json);

        BuildingRepository.set(blueprint);
        ServerNetwork.sendBuildingPreview(player, json, blueprint.name);

        return MCPToolResponse.success(String.format("""
            Building '%s' loaded successfully.
            - Single blocks: %d
            - Fill regions: %d (~%d blocks estimated)
            - Embedded anchors: %d
            Player should now hold the Building Wand to see the preview.
            (Left-click=rotate +90°, Right-click=place)
            """,
            blueprint.name,
            blueprint.blockCount,
            blueprint.fillCount,
            fillEstimate,
            blueprint.anchorCount
        ));
    }

    /** 粗略估算 fills 展开的方块数（用于反馈信息，不影响实际放置） */
    private int estimateFillBlocks(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("fills")) return 0;
            int total = 0;
            for (var elem : root.getAsJsonArray("fills")) {
                var arr = elem.getAsJsonArray();
                int dx = Math.abs(arr.get(3).getAsInt() - arr.get(0).getAsInt()) + 1;
                int dy = Math.abs(arr.get(4).getAsInt() - arr.get(1).getAsInt()) + 1;
                int dz = Math.abs(arr.get(5).getAsInt() - arr.get(2).getAsInt()) + 1;
                total += dx * dy * dz;
            }
            return total;
        } catch (Exception e) {
            return -1;
        }
    }
}
