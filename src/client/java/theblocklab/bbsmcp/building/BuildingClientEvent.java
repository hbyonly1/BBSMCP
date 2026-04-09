package theblocklab.bbsmcp.building;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.network.ServerNetwork;

/**
 * 客户端建筑魔杖交互事件。
 * 仿照 AnchorClientEvent 结构：
 * - 左键（tick 检测）：旋转建筑朝向 +90°
 * - 右键：确认放置，发送 C2S 包
 * - 拦截左键点击方块（阻止挖掘）
 */
@Environment(EnvType.CLIENT)
public class BuildingClientEvent {

    private static boolean lastPressed = false;

    public static void register() {
        // ── 左键 tick 检测（旋转）────────────────────────────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (!isHoldingWand(client.player)) return;
            if (BuildingClientRepository.isEmpty()) return;

            boolean nowPressed = client.options.attackKey.isPressed();
            if (nowPressed && !lastPressed) {
                BuildingClientRepository.rotateLeft();
                client.player.sendMessage(
                    Text.literal("§a[建筑] 朝向: " + BuildingClientRepository.getRotationLabel()),
                    true
                );
            }
            lastPressed = nowPressed;
        });

        // ── 拦截左键点击方块（阻止挖掘）─────────────────────────────────────
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isHoldingWand(player)) return ActionResult.PASS;
            if (BuildingClientRepository.isEmpty()) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // ── 右键点击方块（确认放置）──────────────────────────────────────────
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isHoldingWand(player)) return ActionResult.PASS;
            if (BuildingClientRepository.isEmpty()) return ActionResult.PASS;

            sendPlacePacket(player);
            return ActionResult.SUCCESS;
        });

        // ── 右键（对着空气）也触发放置 ───────────────────────────────────────
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND)
                return TypedActionResult.pass(ItemStack.EMPTY);
            if (!isHoldingWand(player))
                return TypedActionResult.pass(ItemStack.EMPTY);
            if (BuildingClientRepository.isEmpty())
                return TypedActionResult.pass(ItemStack.EMPTY);

            sendPlacePacket(player);
            return TypedActionResult.success(ItemStack.EMPTY);
        });
    }

    private static void sendPlacePacket(PlayerEntity player) {
        BlockPos origin   = BuildingClientRepository.getPreviewOrigin(player);
        int      rotation = BuildingClientRepository.rotation;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(origin);
        buf.writeInt(rotation);
        ClientPlayNetworking.send(ServerNetwork.C2S_BUILDING_PLACE, buf);

        // 乐观清除（服务端也会广播 S2C_BUILDING_CLEAR 作为确认）
        BuildingClientRepository.clear();
        player.sendMessage(Text.literal("§a[建筑] 放置请求已发送，请等待服务端确认..."), true);
    }

    private static boolean isHoldingWand(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        return !stack.isEmpty() && stack.getItem() instanceof BuildingWandItem;
    }
}
