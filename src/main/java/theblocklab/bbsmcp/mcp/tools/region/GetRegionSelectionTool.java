package theblocklab.bbsmcp.mcp.tools.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.region.RegionEditManager;
import theblocklab.bbsmcp.region.RegionSelection;

public class GetRegionSelectionTool extends MCPTool {

    public GetRegionSelectionTool() {
        super("get_region_selection",
            "获取当前玩家用区域魔杖选中的 3D 区域，并返回区域内所有方块的完整 BlockState。输出坐标基于选区最小点。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
            {"type":"object","properties":{}}
            """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        RegionSelection selection = RegionEditManager.INSTANCE.getSelection(player);
        if (!selection.isComplete()) {
            return MCPToolResponse.error("当前选区不完整。", "请先使用区域魔杖设置 Pos1 和 Pos2。");
        }
        if (selection.volume() > RegionEditManager.MAX_SELECTION_VOLUME) {
            return MCPToolResponse.error(
                "当前选区体积过大: " + selection.volume(),
                "请缩小选区。当前最大支持体积为 " + RegionEditManager.MAX_SELECTION_VOLUME + " 个方块。"
            );
        }

        BlockPos min = selection.min();
        BlockPos max = selection.max();
        JsonObject result = new JsonObject();
        result.addProperty("dimension", player.getWorld().getRegistryKey().getValue().toString());
        result.addProperty("regionKey", selection.regionKey(player.getWorld()));

        JsonObject selectionObj = new JsonObject();
        selectionObj.add("pos1", toPos(selection.pos1()));
        selectionObj.add("pos2", toPos(selection.pos2()));
        selectionObj.add("min", toPos(min));
        selectionObj.add("max", toPos(max));

        JsonObject sizeObj = new JsonObject();
        sizeObj.addProperty("x", selection.sizeX());
        sizeObj.addProperty("y", selection.sizeY());
        sizeObj.addProperty("z", selection.sizeZ());
        selectionObj.add("size", sizeObj);
        selectionObj.addProperty("volume", selection.volume());
        result.add("selection", selectionObj);

        JsonArray blocks = new JsonArray();
        for (RegionEditManager.BlockSnapshot snapshot : RegionEditManager.INSTANCE.captureSelection(player.getWorld(), selection)) {
            JsonArray item = new JsonArray();
            item.add(snapshot.pos().getX() - min.getX());
            item.add(snapshot.pos().getY() - min.getY());
            item.add(snapshot.pos().getZ() - min.getZ());
            item.add(snapshot.blockSpec());
            blocks.add(item);
        }
        result.add("blocks", blocks);

        return MCPToolResponse.success(
            result.toString(),
            """
            patch_json 使用局部数组格式：
            blocks=[[dx,dy,dz,"block_spec"],...], fills=[[x1,y1,z1,x2,y2,z2,"block_spec"],...]
            坐标均相对于当前选区的 min 点。
            未提供的坐标表示“不修改”；block_spec=air 表示“删除原方块”。
            """
        );
    }

    private JsonObject toPos(BlockPos pos) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", pos.getX());
        obj.addProperty("y", pos.getY());
        obj.addProperty("z", pos.getZ());
        return obj;
    }
}
