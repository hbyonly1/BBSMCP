package theblocklab.bbsmcp.mcp.tools.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;

import theblocklab.bbsmcp.building.BuildingBlueprint;
import theblocklab.bbsmcp.building.BuildingFileLoader;
import theblocklab.bbsmcp.building.BuildingRepository;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
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
            "按建筑名称加载建筑蓝图，发送给客户端渲染绿色线框虚影。" +
            "传入 blueprint_json 时会先保存到 config/bbsmcp/buildings/<buildingName>.json，再加载预览；" +
            "不传 blueprint_json 时则直接从该文件读取并预览。" +
            "玩家拿起建筑魔杖后可在前方预览建筑，左键旋转方向，右键确认放置。" +
            "blueprint_json 使用极简数组格式：" +
            "blocks=[[dx,dy,dz,\"id\"],...], fills=[[x1,y1,z1,x2,y2,z2,\"id\"],...], anchors=[[dx,dy,dz,\"name\",\"desc\"],...]。" +
            "方块字符串支持可选 BlockState，例如 oak_stairs[facing=north,half=bottom]。overwrite 默认为 false，避免意外覆盖。" +
            "方块 id 无需 minecraft: 前缀（系统自动添加）；包含 ':' 则原样使用。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
            {
              "type": "object",
              "properties": {
                "buildingName": {
                  "type": "string",
                  "description": "建筑名称，用于保存/读取 config/bbsmcp/buildings/<buildingName>.json"
                },
                "blueprint_json": {
                  "type": "string",
                  "description": "建筑蓝图 JSON 字符串（极简数组格式）。传入时将尝试保存并加载；不传则从本地同名文件读取"
                },
                "overwrite": {
                  "type": "boolean",
                  "description": "当 blueprint_json 被传入且目标文件已存在时，是否允许覆盖。默认 false"
                }
              },
              "required": ["buildingName"]
            }
            """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String buildingName = requireString(arguments, "buildingName");
        boolean hasBlueprintJson = arguments.has("blueprint_json") && !arguments.get("blueprint_json").isJsonNull();
        boolean overwrite = arguments.has("overwrite") && !arguments.get("overwrite").isJsonNull()
            && arguments.get("overwrite").getAsBoolean();

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        String json;
        String sanitizedName;
        Path buildingFilePath;
        String sourceDescription;
        try {
            sanitizedName = BuildingFileLoader.sanitizeBuildingName(buildingName);
            buildingFilePath = BuildingFileLoader.resolveBuildingPath(sanitizedName);
            if (hasBlueprintJson) {
                json = requireString(arguments, "blueprint_json");
                var saveResult = BuildingFileLoader.saveBlueprint(sanitizedName, json, overwrite);
                sourceDescription = saveResult.existedBeforeSave()
                    ? "saved and overwritten"
                    : "saved";
            } else {
                json = BuildingFileLoader.readBlueprint(sanitizedName);
                sourceDescription = "loaded from existing file";
            }
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
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
            - Building name: %s
            - Building file action: %s
            - Building file path: %s
            - Single blocks: %d
            - Fill regions: %d (~%d blocks estimated)
            - Embedded anchors: %d
            Player should now hold the Building Wand to see the preview.
            (Left-click=rotate +90°, Right-click=place)
            """,
            blueprint.name,
            sanitizedName,
            sourceDescription,
            buildingFilePath,
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
