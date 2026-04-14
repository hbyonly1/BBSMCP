package theblocklab.bbsmcp.mcp.tools.region;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.region.RegionWandItem;

public class GiveRegionWandTool extends MCPTool {

    public GiveRegionWandTool() {
        super("give_region_wand", "给玩家一把区域选择魔杖，用于设置 Pos1 / Pos2 并驱动局部区域编辑。");
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

        var item = Registries.ITEM.get(RegionWandItem.ID);
        if (item == null) {
            return MCPToolResponse.error("未找到区域魔杖物品。", "请检查插件是否正确初始化。");
        }

        ItemStack stack = new ItemStack(item);
        if (player.getInventory().insertStack(stack)) {
            player.sendMessage(Text.literal("§a[BBSMCP Region] 已给予区域魔杖。"));
            return MCPToolResponse.success("区域魔杖已发放给玩家。");
        }

        return MCPToolResponse.error("背包已满，无法给予区域魔杖。", "请清理背包后重试。");
    }
}
