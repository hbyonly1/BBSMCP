package theblocklab.bbsmcp.region;

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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import theblocklab.bbsmcp.utils.RenderUtils;

@Environment(EnvType.CLIENT)
public final class RegionClientRenderer {

    private static final float EXPAND = 0.006f;

    private RegionClientRenderer() {}

    public static void register() {
        WorldRenderEvents.LAST.register(RegionClientRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty() || !(held.getItem() instanceof RegionWandItem)) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        if (client.crosshairTarget instanceof BlockHitResult hit && consumers != null) {
            drawSingleBox(matrices, consumers, camPos, hit.getBlockPos(), 0.25f, 0.75f, 1.0f, 0.18f, 0.4f, 0.85f, 1.0f);
        }

        if (RegionClientRepository.getPos1() != null && consumers != null) {
            drawSingleBox(matrices, consumers, camPos, RegionClientRepository.getPos1(), 0.25f, 1.0f, 0.35f, 0.20f, 0.35f, 1.0f, 0.45f);
        }

        if (RegionClientRepository.getPos2() != null && consumers != null) {
            drawSingleBox(matrices, consumers, camPos, RegionClientRepository.getPos2(), 1.0f, 0.25f, 0.35f, 0.20f, 1.0f, 0.45f, 0.45f);
        }

        if (RegionClientRepository.isComplete() && consumers != null) {
            BlockPos min = RegionClientRepository.min();
            BlockPos max = RegionClientRepository.max();
            matrices.push();
            matrices.translate(min.getX() - camPos.x, min.getY() - camPos.y, min.getZ() - camPos.z);
            VertexConsumer fill = consumers.getBuffer(RenderLayer.getGuiOverlay());
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
            RenderUtils.drawFilledBox(matrices, fill, -EXPAND, -EXPAND, -EXPAND,
                (max.getX() - min.getX()) + 1 + EXPAND,
                (max.getY() - min.getY()) + 1 + EXPAND,
                (max.getZ() - min.getZ()) + 1 + EXPAND,
                0.95f, 0.75f, 0.2f, 0.08f);
            RenderUtils.drawOutlineBox(matrices, lines, -EXPAND, -EXPAND, -EXPAND,
                (max.getX() - min.getX()) + 1 + EXPAND,
                (max.getY() - min.getY()) + 1 + EXPAND,
                (max.getZ() - min.getZ()) + 1 + EXPAND,
                1.0f, 0.8f, 0.25f, 1.0f);
            matrices.pop();
        }

        if (consumers != null) {
            for (RegionClientRepository.PreviewChange change : RegionClientRepository.getPreviewChanges()) {
                boolean delete = "minecraft:air".equals(change.afterSpec());
                if (delete) {
                    drawSingleBox(matrices, consumers, camPos, change.pos(), 1.0f, 0.2f, 0.2f, 0.22f, 1.0f, 0.35f, 0.35f);
                } else {
                    drawSingleBox(matrices, consumers, camPos, change.pos(), 0.2f, 0.9f, 1.0f, 0.18f, 0.3f, 1.0f, 1.0f);
                }
            }
        }
    }

    private static void drawSingleBox(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camPos, BlockPos pos,
                                      float fillR, float fillG, float fillB, float fillA,
                                      float lineR, float lineG, float lineB) {
        matrices.push();
        matrices.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
        VertexConsumer fill = consumers.getBuffer(RenderLayer.getGuiOverlay());
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        RenderUtils.drawFilledBox(matrices, fill, -EXPAND, -EXPAND, -EXPAND, 1 + EXPAND, 1 + EXPAND, 1 + EXPAND, fillR, fillG, fillB, fillA);
        RenderUtils.drawOutlineBox(matrices, lines, -EXPAND, -EXPAND, -EXPAND, 1 + EXPAND, 1 + EXPAND, 1 + EXPAND, lineR, lineG, lineB, 1.0f);
        matrices.pop();
    }
}
