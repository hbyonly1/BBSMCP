package theblocklab.bbsmcp.building;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 建筑魔杖物品，交互逻辑由 BuildingClientEvent 处理。
 * 左键：旋转建筑朝向 (+90°)
 * 右键：确认放置当前预览建筑
 */
public class BuildingWandItem extends Item {

    public static final Identifier ID = new Identifier("bbsmcp", "building_wand");

    public BuildingWandItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.bbsmcp.building_wand.tooltip1"));
        tooltip.add(Text.translatable("item.bbsmcp.building_wand.tooltip2"));
        tooltip.add(Text.translatable("item.bbsmcp.building_wand.tooltip3"));
    }

    /** 注册物品到游戏注册表 */
    public static void register() {
        Registry.register(Registries.ITEM, ID, new BuildingWandItem());
    }
}
