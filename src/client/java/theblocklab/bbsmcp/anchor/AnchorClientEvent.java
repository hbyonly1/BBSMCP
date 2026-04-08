package theblocklab.bbsmcp.anchor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
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
    static final Map<UUID, BlockPos> PENDING_DELETE = new HashMap<>();
    /** key=playerUUID, value=待创建确认的方块坐标 */
    static final Map<UUID, BlockPos> PENDING_CREATE = new HashMap<>();
    private static boolean lastPressed = false;

    public static void register() {
        // 客户端加入世界时，请求全量同步锚点
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                ClientPlayNetworking.send(ServerNetwork.C2S_ANCHOR_SYNC_REQUEST, PacketByteBufs.create());
            });
        });

        // 左键点击空气
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean nowPressed = client.options.attackKey.isPressed();

            if (nowPressed && !lastPressed) {
                if (isHoldingWand(client.player)) {
                    notifyToggleVisibility(client.player);
                }
            }
            lastPressed = nowPressed;
        });

        // 拦截左键点击方块事件
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isHoldingWand(player)) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // 右键点击方块
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            if (!isHoldingWand(player)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            handleInteraction(player, pos);
            return ActionResult.SUCCESS; // 拦截默认动作
        });

        // 右键（对着空气）使用物品
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient || hand != Hand.MAIN_HAND) return TypedActionResult.pass(ItemStack.EMPTY);
            if (!isHoldingWand(player)) return TypedActionResult.pass(ItemStack.EMPTY);

            double reachDistance = 5.0; 
            HitResult hitResult = player.raycast(reachDistance, 0, false);
            // 修复负坐标转换偏差
            BlockPos pos = BlockPos.ofFloored(hitResult.getPos());
            handleInteraction(player, pos);
            return TypedActionResult.success(ItemStack.EMPTY);
        });
    }

    private static void notifyToggleVisibility(PlayerEntity player){
        AnchorClientRepository.toggleVisibility();
        if(AnchorClientRepository.isVisible()) {
            player.sendMessage(Text.literal("§b[BBSMCP Anchor] 锚点显示已开启。"), true);
        } else {
            player.sendMessage(Text.literal("§b[BBSMCP Anchor] 锚点显示已关闭。"), true);
        }
    }

    private static Anchor getTargetedAnchor(PlayerEntity player) {
        net.minecraft.util.math.Vec3d cameraPos = player.getCameraPosVec(1.0F);
        net.minecraft.util.math.Vec3d rotation = player.getRotationVec(1.0F);
        net.minecraft.util.math.Vec3d end = cameraPos.add(rotation.multiply(10.0)); // 扩大检测距离为10

        Anchor closest = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (Anchor a : AnchorClientRepository.getAll()) {
            net.minecraft.util.math.Vec3d anchorPos = new net.minecraft.util.math.Vec3d(a.pos.getX() + 0.5, a.pos.getY() + 0.5, a.pos.getZ() + 0.5);
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(
                anchorPos.x - 0.5, anchorPos.y - 0.5, anchorPos.z - 0.5,
                anchorPos.x + 0.5, anchorPos.y + 0.5, anchorPos.z + 0.5
            );

            java.util.Optional<net.minecraft.util.math.Vec3d> hit = box.raycast(cameraPos, end);
            if (hit.isPresent()) {
                double distSq = cameraPos.squaredDistanceTo(hit.get());
                if (distSq < minDistanceSq) {
                    minDistanceSq = distSq;
                    closest = a;
                }
            }
        }
        return closest;
    }

    private static void handleInteraction(PlayerEntity player, BlockPos pos) {
        // 首先通过视线射线精确查找目标锚点
        Anchor anchor = getTargetedAnchor(player);
        
        // 兜底：如果射线没有击中，则回退到严格的坐标匹配
        if (anchor == null) {
            for (Anchor a : AnchorClientRepository.getAll()) {
                if (a.pos.equals(pos)) {
                    anchor = a;
                    break;
                }
            }
        }

        // 如果判定玩家点击的是某个已知锚点，同步操作的 BlockPos
        if (anchor != null) {
            pos = anchor.pos;
        }


        UUID uuid = player.getUuid();
        if (anchor != null) {
            // 已存在锚点 - 处理双击确认删除
            PENDING_CREATE.remove(uuid);
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
            // 无锚点 - 处理双击确认创建
            PENDING_DELETE.remove(uuid);
            BlockPos pending = PENDING_CREATE.get(uuid);
            if (pos.equals(pending)) {
                // 第二次点击 - 发送创建请求
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBlockPos(pos);
                ClientPlayNetworking.send(ServerNetwork.C2S_ANCHOR_CREATE, buf);
                
                PENDING_CREATE.remove(uuid);
                player.sendMessage(Text.literal("§a[BBSMCP Anchor] 正在请求创建锚点..."), true);
            } else {
                // 第一次点击 - 记录状态并警告
                PENDING_CREATE.put(uuid, pos);
                player.sendMessage(Text.literal("§e[BBSMCP Anchor] 再次右键点击确认在 (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") 创建锚点。"), true);
            }
        }
    }

    private static boolean isHoldingWand(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        return !stack.isEmpty() && stack.getItem() instanceof AnchorItem;
    }
}
