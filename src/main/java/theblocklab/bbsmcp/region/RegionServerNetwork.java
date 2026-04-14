package theblocklab.bbsmcp.region;

import com.google.gson.JsonArray;
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

/**
 * 区域编辑的服务端网络层。
 */
public final class RegionServerNetwork {

    private RegionServerNetwork() {}

    public static void setup() {
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.C2S_REGION_SET_POS1, RegionServerNetwork::onSetPos1);
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.C2S_REGION_SET_POS2, RegionServerNetwork::onSetPos2);
    }

    private static void onSetPos1(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                                  PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        server.execute(() -> {
            RegionEditManager.INSTANCE.setPos1(player, pos);
            sendRegionClearPreview(player);
            player.sendMessage(Text.literal(String.format("§a[Region] Pos1 已设置为 (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())), true);
        });
    }

    private static void onSetPos2(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler,
                                  PacketByteBuf buf, net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        BlockPos pos = buf.readBlockPos();
        server.execute(() -> {
            RegionEditManager.INSTANCE.setPos2(player, pos);
            sendRegionClearPreview(player);
            player.sendMessage(Text.literal(String.format("§c[Region] Pos2 已设置为 (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ())), true);
        });
    }

    public static void sendRegionPreview(ServerPlayerEntity player, RegionEditManager.PreviewState preview) {
        JsonArray array = new JsonArray();
        for (RegionEditManager.BlockChange change : preview.changes()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", change.pos().getX());
            obj.addProperty("y", change.pos().getY());
            obj.addProperty("z", change.pos().getZ());
            obj.addProperty("before", change.beforeSpec());
            obj.addProperty("after", change.afterSpec());
            array.add(obj);
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(preview.regionKey());
        buf.writeString(array.toString());
        ServerPlayNetworking.send(player, ServerNetwork.S2C_REGION_PREVIEW, buf);
    }

    public static void sendRegionClearPreview(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, ServerNetwork.S2C_REGION_CLEAR_PREVIEW, PacketByteBufs.create());
    }
}
