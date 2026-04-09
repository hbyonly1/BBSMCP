package theblocklab.bbsmcp.mcp.tools.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import theblocklab.bbsmcp.building.BuildingWandItem;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * MCP 工具：向玩家发放建筑魔杖。
 * 工具名：give_building_wand
 */
public class GiveBuildingWandTool extends MCPTool {

    public GiveBuildingWandTool() {
        super("give_building_wand",
            "给玩家一把建筑魔杖（building_wand）。" +
            "持有魔杖时可预览并放置当前加载的建筑蓝图：左键旋转 +90°，右键确认放置。");
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
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        // 检查玩家是否已有魔杖，避免重复发放
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof BuildingWandItem) {
                return MCPToolResponse.success("玩家已持有建筑魔杖，无需重复发放。");
            }
        }

        ItemStack wand = new ItemStack(net.minecraft.registry.Registries.ITEM.get(BuildingWandItem.ID));
        player.getInventory().insertStack(wand);
        player.sendMessage(Text.literal("§a[BBSMCP Building] 建筑魔杖已发放！加载蓝图后持有魔杖即可预览。"));

        return MCPToolResponse.success("建筑魔杖已发放给玩家。");
    }
}
