package theblocklab.bbsmcp.mcp.tools.anchor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import theblocklab.bbsmcp.anchor.AnchorItem;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * 给予玩家锚点魔杖。
 */
public class GiveAnchorWandTool extends MCPTool {

    public GiveAnchorWandTool() {
        super("give_anchor_wand", "将锚点魔杖 (Anchor Wand) 放入当前玩家的背包。");
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
        if (player == null) 
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        // 从注册表中获取物品实例
        var item = Registries.ITEM.get(AnchorItem.ID);
        if (item == null) {
            return MCPToolResponse.error("未找到锚点魔杖项目。", "请检查插件是否正确初始化。");
        }

        ItemStack stack = new ItemStack(item);
        boolean success = player.getInventory().insertStack(stack);

        if (success) {
            player.sendMessage(Text.literal("§a[BBSMCP] 已给予锚点魔杖。"));
            return MCPToolResponse.success("已成功授予锚点魔杖。");
        } else {
            return MCPToolResponse.error("背包已满，无法给予物品。", "请清理背包后重试。");
        }
    }
}
