package theblocklab.bbsmcp.anchor;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 锚点魔杖物品，负责简单的物品定义。
 */
public class AnchorItem extends Item {

    public static final Identifier ID = new Identifier("bbsmcp", "anchor_wand");

    public AnchorItem() {
        super(new Item.Settings().maxCount(1));
    }

    /** 注册物品（逻辑由 Event 类处理） */
    public static void register() {
        Registry.register(Registries.ITEM, ID, new AnchorItem());
    }
}
