package theblocklab.bbsmcp.anchor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.network.ServerNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端锚点系统事件监听。
 * 驱动所有交互逻辑（判定、确认、本地效果控制）。
 */
@Environment(EnvType.CLIENT)
public class AnchorClientEvent {

    /** key=playerUUID, value=待删除确认的方块坐标 */
    private static final Map<UUID, BlockPos> PENDING_DELETE = new HashMap<>();

    public static void register() {
        // 1. 左键点击：切换全局可见性（纯本地操作）
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (client.options.attackKey.wasPressed()) {
                if (isHoldingWand(client.player)) {
                    AnchorClientRepository.toggleVisibility();
                    client.player.sendMessage(Text.literal("§b[BBSMCP Anchor] 锚点显示已切换。"), true);
                }
            }
        });

        // 2. 右键点击方块：创建或删除（本地逻辑判定 + 发包）
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isHoldingWand(player)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            handleInteraction(player, pos);
            return ActionResult.SUCCESS; // 拦截默认动作
        });
    }

    private static void handleInteraction(net.minecraft.entity.player.PlayerEntity player, BlockPos pos) {
        // 查找本地缓存
        Anchor anchor = null;
        for (Anchor a : AnchorClientRepository.getAll()) {
            if (a.pos.equals(pos)) {
                anchor = a;
                break;
            }
        }

        if (anchor != null) {
            // 已存在锚点 - 处理双击确认删除
            UUID uuid = player.getUuid();
            BlockPos pending = PENDING_DELETE.get(uuid);
            if (pos.equals(pending)) {
                // 第二次点击 - 发送删除请求
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(anchor.id);
                ClientPlayNetworking.send(ServerNetwork.C2S_ANCHOR_REMOVE, buf);
                
                PENDING_DELETE.remove(uuid);
                player.sendMessage(Text.literal("§c[BBSMCP Anchor] 正在请求删除锚点..."), true);
            } else {
                // 第一次点击 - 记录状态并警告
                PENDING_DELETE.put(uuid, pos);
                player.sendMessage(Text.literal("§e[BBSMCP Anchor] 再次右键点击确认删除锚点「" + anchor.name + "」。"), true);
            }
        } else {
            // 无锚点 - 发送创建请求
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBlockPos(pos);
            ClientPlayNetworking.send(ServerNetwork.C2S_ANCHOR_CREATE, buf);
            
            PENDING_DELETE.remove(player.getUuid());
            player.sendMessage(Text.literal("§a[BBSMCP Anchor] 正在请求创建锚点..."), true);
        }
    }

    private static boolean isHoldingWand(net.minecraft.entity.player.PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        return !stack.isEmpty() && stack.getItem() instanceof AnchorItem;
    }
}
