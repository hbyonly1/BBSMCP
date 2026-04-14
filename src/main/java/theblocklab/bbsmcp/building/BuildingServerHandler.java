package theblocklab.bbsmcp.building;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import theblocklab.bbsmcp.anchor.AnchorManager;
import theblocklab.bbsmcp.anchor.AnchorManagerAPI;

import theblocklab.bbsmcp.network.ServerNetwork;

/**
 * 服务端建筑放置处理器。
 * 接收客户端发来的 C2S_BUILDING_PLACE 包，展开蓝图并在世界中写入方块。
 */
public class BuildingServerHandler {

    public static void setup() {
        ServerPlayNetworking.registerGlobalReceiver(
            ServerNetwork.C2S_BUILDING_PLACE,
            BuildingServerHandler::handlePlacePacket
        );
    }

    private static void handlePlacePacket(
        MinecraftServer server,
        ServerPlayerEntity player,
        net.minecraft.server.network.ServerPlayNetworkHandler handler,
        net.minecraft.network.PacketByteBuf buf,
        net.fabricmc.fabric.api.networking.v1.PacketSender responseSender
    ) {
        BlockPos origin   = buf.readBlockPos();
        int      rotation = buf.readInt();

        server.execute(() -> {
            BuildingBlueprint blueprint = BuildingRepository.current;
            if (blueprint == null) {
                player.sendMessage(Text.literal("§c[BBSMCP Building] 没有待放置的建筑蓝图！"));
                return;
            }

            World world = player.getWorld();

            // ── 放置方块 ──────────────────────────────────────────
            var placedBlocks = blueprint.getAbsoluteBlocks(origin, rotation);
            int placed = 0;
            int failed = 0;

            for (var pb : placedBlocks) {
                BlockState state = BuildingBlueprint.parseRotatedBlockState(pb.blockSpec(), rotation);
                if (state == null) {
                    failed++;
                    continue;
                }
                world.setBlockState(pb.pos(), state, Block.NOTIFY_ALL);
                placed++;
            }

            // ── 注册内嵌 Anchors ──────────────────────────────────
            var anchors = blueprint.getAbsoluteAnchors(origin, rotation);
            int anchorCreated = 0;
            for (var ap : anchors) {
                if (AnchorManager.INSTANCE.getAt(ap.pos()) == null) {
                    AnchorManagerAPI.INSTANCE.create(player, ap.pos(), ap.name(), ap.desc(), "#4488FF");
                    anchorCreated++;
                }
            }

            // ── 清理 ──────────────────────────────────────────────
            BuildingRepository.clear();
            ServerNetwork.sendBuildingClear(player);

            // ── 反馈 ──────────────────────────────────────────────
            player.sendMessage(Text.literal(String.format(
                "§a[BBSMCP Building] 建筑「%s」放置完成！放置方块: %d，失败: %d，创建锚点: %d",
                blueprint.name, placed, failed, anchorCreated
            )));
        });
    }
}
