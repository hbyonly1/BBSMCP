package theblocklab.bbsmcp.anchor;

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
 * 锚点魔杖物品，负责物品定义。
 */
public class AnchorItem extends Item {

    public static final Identifier ID = new Identifier("bbsmcp", "anchor_wand");

    public AnchorItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.bbsmcp.anchor_wand.tooltip1"));
        tooltip.add(Text.translatable("item.bbsmcp.anchor_wand.tooltip2"));
    }

    /** 注册物品（逻辑由 Event 类处理） */
    public static void register() {
        Registry.register(Registries.ITEM, ID, new AnchorItem());
    }
}
