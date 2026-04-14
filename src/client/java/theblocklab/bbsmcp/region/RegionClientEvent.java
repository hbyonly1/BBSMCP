package theblocklab.bbsmcp.region;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.network.ServerNetwork;

@Environment(EnvType.CLIENT)
public final class RegionClientEvent {

    private RegionClientEvent() {}

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isHoldingWand(player)) return ActionResult.PASS;

            RegionClientRepository.setPos1(pos);
            var buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            ClientPlayNetworking.send(ServerNetwork.C2S_REGION_SET_POS1, buf);
            player.sendMessage(Text.literal("§a[Region] 已设置 Pos1"), true);
            return ActionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isHoldingWand(player)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            setPos2(player, pos);
            return ActionResult.SUCCESS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return TypedActionResult.pass(ItemStack.EMPTY);
            if (!isHoldingWand(player)) return TypedActionResult.pass(ItemStack.EMPTY);

            HitResult hitResult = player.raycast(5.0, 0, false);
            BlockPos pos = BlockPos.ofFloored(hitResult.getPos());
            setPos2(player, pos);
            return TypedActionResult.success(ItemStack.EMPTY);
        });
    }

    private static void setPos2(PlayerEntity player, BlockPos pos) {
        RegionClientRepository.setPos2(pos);
        var buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(ServerNetwork.C2S_REGION_SET_POS2, buf);
        player.sendMessage(Text.literal("§c[Region] 已设置 Pos2"), true);
    }

    private static boolean isHoldingWand(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        return !stack.isEmpty() && stack.getItem() instanceof RegionWandItem;
    }
}
