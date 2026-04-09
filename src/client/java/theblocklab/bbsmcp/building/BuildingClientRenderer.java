package theblocklab.bbsmcp.building;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import theblocklab.bbsmcp.utils.RenderUtils;

import java.util.List;

/**
 * 客户端建筑虚影渲染器。
 * 在 WorldRenderEvents.LAST 阶段执行：
 * 当玩家持有建筑魔杖且有待放置蓝图时，在玩家前方渲染绿色线框虚影。
 */
@Environment(EnvType.CLIENT)
public class BuildingClientRenderer {

    private static final float EXPAND = 0.003f;
    // 线框颜色：绿色
    private static final float R = 0.3f, G = 1.0f, B = 0.3f, A = 1.0f;
    // 填充色：半透明绿色
    private static final float FR = 0.2f, FG = 0.9f, FB = 0.2f, FA = 0.15f;

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

        VertexConsumer lineConsumer = consumers.getBuffer(RenderLayer.getLines());
        VertexConsumer faceConsumer = consumers.getBuffer(RenderLayer.getGuiOverlay());

        for (var pb : placed) {
            BlockPos pos = pb.pos();
            double dx = pos.getX() - camPos.x;
            double dy = pos.getY() - camPos.y;
            double dz = pos.getZ() - camPos.z;

            matrices.push();
            matrices.translate(dx, dy, dz);

            // 半透明面填充
            RenderUtils.drawFilledBox(matrices, faceConsumer,
                -EXPAND, -EXPAND, -EXPAND,
                1 + EXPAND, 1 + EXPAND, 1 + EXPAND,
                FR, FG, FB, FA);

            // 线框
            RenderUtils.drawOutlineBox(matrices, lineConsumer,
                -EXPAND, -EXPAND, -EXPAND,
                1 + EXPAND, 1 + EXPAND, 1 + EXPAND,
                R, G, B, A);

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
