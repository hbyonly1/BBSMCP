package theblocklab.bbsmcp.building;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * 客户端建筑虚影渲染器。
 * 在 WorldRenderEvents.LAST 阶段执行：
 * 当玩家持有建筑魔杖且有待放置蓝图时，在玩家前方渲染绿色线框虚影。
 */
@Environment(EnvType.CLIENT)
public class BuildingClientRenderer {

    public static void register() {
        WorldRenderEvents.LAST.register(BuildingClientRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || BuildingClientRepository.isEmpty()) return;

        // 仅在持有建筑魔杖时渲染
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty() || !(held.getItem() instanceof BuildingWandItem)) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = context.camera();
        Vec3d camPos  = camera.getPos();

        BuildingBlueprint blueprint = BuildingClientRepository.current;
        int rotation = BuildingClientRepository.rotation;
        BlockPos origin = BuildingClientRepository.getPreviewOrigin(client.player);

        // 展开所有方块坐标（fills + blocks，含旋转变换）
        List<BuildingBlueprint.PlacedBlock> placed = blueprint.getAbsoluteBlocks(origin, rotation);

        BlockRenderManager blockRenderManager = client.getBlockRenderManager();
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        for (var pb : placed) {
            BlockPos pos = pb.pos();
            double dx = pos.getX() - camPos.x;
            double dy = pos.getY() - camPos.y;
            double dz = pos.getZ() - camPos.z;

            // 获取真实的方块状态
            String blockId = BuildingBlueprint.normalizeBlockId(pb.blockId());
            Identifier id = Identifier.tryParse(blockId);
            if (id == null) continue;
            
            BlockState state = Registries.BLOCK.get(id).getDefaultState();

            matrices.push();
            matrices.translate(dx, dy, dz);

            // 渲染为实体方块（自带真实贴图、模型结构，如楼梯等将完美呈现）
            blockRenderManager.renderBlockAsEntity(state, matrices, consumers, light, OverlayTexture.DEFAULT_UV);

            matrices.pop();
        }

        // ActionBar 提示
        int totalBlocks = placed.size();
        String msg = String.format("§a[建筑] §f%s §7| 朝向: §f%s §7| 放置点: §f(%d,%d,%d) §7| 方块数: §f%d",
            blueprint.name,
            BuildingClientRepository.getRotationLabel(),
            origin.getX(), origin.getY(), origin.getZ(),
            totalBlocks
        );
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(msg), true);
        }
    }

}
