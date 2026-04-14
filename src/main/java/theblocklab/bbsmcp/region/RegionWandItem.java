package theblocklab.bbsmcp.region;

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
 * 区域选择魔杖。
 */
public class RegionWandItem extends Item {

    public static final Identifier ID = new Identifier("bbsmcp", "region_wand");

    public RegionWandItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.bbsmcp.region_wand.tooltip1"));
        tooltip.add(Text.translatable("item.bbsmcp.region_wand.tooltip2"));
        tooltip.add(Text.translatable("item.bbsmcp.region_wand.tooltip3"));
    }

    public static void register() {
        Registry.register(Registries.ITEM, ID, new RegionWandItem());
    }
}
