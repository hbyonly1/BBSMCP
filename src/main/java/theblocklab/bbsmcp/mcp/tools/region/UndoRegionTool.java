package theblocklab.bbsmcp.mcp.tools.region;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.region.RegionBlockStateCodec;
import theblocklab.bbsmcp.region.RegionEditManager;
import theblocklab.bbsmcp.region.RegionSelection;
import theblocklab.bbsmcp.region.RegionServerNetwork;

public class UndoRegionTool extends MCPTool {

    public UndoRegionTool() {
        super("undo__region", "撤回当前选区最近一次已应用的局部区域修改。撤回历史按选区 regionKey 维护，而不是全局唯一上一条。");
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

        String regionKey = selection.regionKey(player.getWorld());
        RegionEditManager.HistoryEntry entry = RegionEditManager.INSTANCE.popHistory(player, regionKey);
        if (entry == null) {
            return MCPToolResponse.error("当前选区没有可撤回的历史记录。", "请先在该选区执行 apply。");
        }

        int reverted = 0;
        for (RegionEditManager.BlockChange change : entry.changes()) {
            BlockState state = RegionBlockStateCodec.parse(change.beforeSpec());
            if (state == null) {
                return MCPToolResponse.error("无法解析撤回目标方块: " + change.beforeSpec(), "请检查历史记录数据。");
            }
            player.getWorld().setBlockState(change.pos(), state, Block.NOTIFY_ALL);
            reverted++;
        }

        RegionEditManager.INSTANCE.clearPreview(player);
        RegionServerNetwork.sendRegionClearPreview(player);
        return MCPToolResponse.success("已撤回当前选区最近一次修改。", "本次回滚方块数: " + reverted);
    }
}
