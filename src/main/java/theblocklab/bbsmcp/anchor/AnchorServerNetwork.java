package theblocklab.bbsmcp.anchor;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.network.ServerNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 服务端锚点网络处理。
 * 负责响应客户端请求 (C2S) 以及向客户端发送更新 (S2C)。
 */
public class AnchorServerNetwork {

    /** key=playerUUID, value=等待聊天描述输入的方块坐标 */
    private static final Map<UUID, BlockPos> PENDING_INPUT = new HashMap<>();

    public static void setup() {
        // 注册 C2S 接收器
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.C2S_ANCHOR_CREATE, AnchorServerNetwork::onC2SCreate);
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.C2S_ANCHOR_REMOVE, AnchorServerNetwork::onC2SRemove);
    }

    // ────────── C2S 处理函数 ──────────

    private static void onC2SCreate(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        server.execute(() -> {
            AnchorManager manager = AnchorManager.INSTANCE;
            if (!manager.hasAt(pos)) {
                String defaultName = String.format("(%d,%d,%d)", pos.getX(), pos.getY(), pos.getZ());
                Anchor anchor = manager.create(pos, defaultName, "", "#4488FF");
                
                PENDING_INPUT.put(player.getUuid(), pos);
                player.sendMessage(Text.literal("§a[BBSMCP Anchor] 锚点「" + defaultName + "」(ID:" + anchor.id + ") 已创建。"));
                player.sendMessage(Text.literal("§b[BBSMCP Anchor] 请在聊天栏输入锚点描述，或输入 §fs§b 跳过："));
                
                sendAnchorUpdatePacket(player, anchor);
            }
        });
    }

    private static void onC2SRemove(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        int id = buf.readInt();
        server.execute(() -> {
            AnchorManager manager = AnchorManager.INSTANCE;
            Anchor anchor = manager.get(id);
            if (anchor != null) {
                String name = anchor.name;
                manager.remove(id);
                player.sendMessage(Text.literal("§c[BBSMCP Anchor] 锚点「" + name + "」(ID:" + id + ") 已删除。"));
                sendAnchorRemovePacket(player, id);
            }
        });
    }

    // ────────── 状态协助 ──────────

    public static boolean handleChatInput(ServerPlayerEntity sender, String content) {
        UUID uuid = sender.getUuid();
        if (!PENDING_INPUT.containsKey(uuid)) return false;

        BlockPos pos = PENDING_INPUT.remove(uuid);
        if ("s".equals(content.trim())) {
            sender.sendMessage(Text.literal("§7[BBSMCP Anchor] 已跳过描述输入。"));
        } else {
            AnchorManager.INSTANCE.updateAt(pos, null, content, null);
            sender.sendMessage(Text.literal("§a[BBSMCP Anchor] 描述已保存：" + content));
            sendAnchorUpdatePacket(sender, AnchorManager.INSTANCE.getAt(pos));
        }
        return true;
    }

    // ────────── S2C 数据包发送 ──────────

    /** 为指定玩家更新锚点 */
    public static void sendAnchorUpdatePacket(ServerPlayerEntity player, Anchor anchor) {
        if (anchor == null) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("UPDATE");
        buf.writeString(anchor.toJson().toString());
        ServerPlayNetworking.send(player, ServerNetwork.S2C_ANCHOR_UPDATE, buf);
    }

    /** 为指定玩家删除锚点 */
    public static void sendAnchorRemovePacket(ServerPlayerEntity player, int id) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("REMOVE");
        buf.writeString(obj.toString());
        ServerPlayNetworking.send(player, ServerNetwork.S2C_ANCHOR_UPDATE, buf);
    }

    /** 为指定玩家发送全量锚点列表 */
    public static void sendAnchorListPacket(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(AnchorManager.INSTANCE.toJsonString());
        ServerPlayNetworking.send(player, ServerNetwork.S2C_ANCHOR_LIST, buf);
    }
}
