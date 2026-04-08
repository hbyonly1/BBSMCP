package theblocklab.bbsmcp.anchor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * 客户端锚点渲染器。
 * 在 WorldRenderEvents.LAST 阶段执行：
 * 1) 持有 anchor_wand 时，对准方块绘制蓝色半透明遮罩
 * 2) 渲染所有缓存的锚点（彩色线框 + 浮空文字）
 */
@Environment(EnvType.CLIENT)
public class AnchorClientRenderer {

    private static final float EXPAND = 0.006f; // 线框轻微扩大，防止 z-fighting

    public static void register() {
        WorldRenderEvents.LAST.register(AnchorClientRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null)
            return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();

        // ── 1. 持有 anchor_wand 时绘制目标方块高亮 ──
        ItemStack held = client.player.getMainHandStack();
        boolean holdingWand = !held.isEmpty() && held.getItem() instanceof AnchorItem;

        if (holdingWand && client.crosshairTarget instanceof BlockHitResult hit) {
            BlockPos target = hit.getBlockPos();

            matrices.push();
            matrices.translate(
                    target.getX() - camPos.x,
                    target.getY() - camPos.y,
                    target.getZ() - camPos.z);

            // 绘制半透明填充（6 个面）
            if (consumers != null) {
                float r = 0.2f, g = 0.5f, b = 1.0f, a = 0.3f; // 默认蓝色
                
                UUID uuid = client.player.getUuid();
                if (target.equals(AnchorClientEvent.PENDING_CREATE.get(uuid))) {
                    r = 0.2f; g = 1.0f; b = 0.2f; // 绿色 (确认创建)
                } else if (target.equals(AnchorClientEvent.PENDING_DELETE.get(uuid))) {
                    r = 1.0f; g = 0.2f; b = 0.2f; // 红色 (确认删除)
                }

                VertexConsumer faceConsumer = consumers.getBuffer(RenderLayer.getGuiOverlay());
                drawFilledBox(matrices, faceConsumer, -EXPAND, -EXPAND, -EXPAND,
                        1 + EXPAND, 1 + EXPAND, 1 + EXPAND, r, g, b, a);
            }

            // 绘制线框
            if (consumers != null) {
                float r = 0.3f, g = 0.6f, b = 1.0f, a = 1.0f; // 默认浅蓝
                
                UUID uuid = client.player.getUuid();
                if (target.equals(AnchorClientEvent.PENDING_CREATE.get(uuid))) {
                    r = 0.3f; g = 1.0f; b = 0.3f; // 浅绿
                } else if (target.equals(AnchorClientEvent.PENDING_DELETE.get(uuid))) {
                    r = 1.0f; g = 0.3f; b = 0.3f; // 浅红
                }

                VertexConsumer lineConsumer = consumers.getBuffer(RenderLayer.getLines());
                // 用于 debug 的可视坐标轴，和调试信息内的一致
                // Matrix4f m = matrices.peek().getPositionMatrix();
                // edge(lineConsumer, m, 0, 0, 0, 1.5f, 0, 0, 1, 0, 0, 1); // X
                // edge(lineConsumer, m, 0, 0, 0, 0, 1.5f, 0, 0, 1, 0, 1); // Y
                // edge(lineConsumer, m, 0, 0, 0, 0, 0, 1.5f, 0, 0, 1, 1); // Z
                drawOutlineBox(matrices, lineConsumer, -EXPAND, -EXPAND, -EXPAND,
                        1 + EXPAND, 1 + EXPAND, 1 + EXPAND, r, g, b, a);
            }
            matrices.pop();
        }

        // ── 2. 渲染所有锚点 ──
        if (!AnchorClientRepository.isVisible())
            return;
        if (consumers == null)
            return;

        TextRenderer textRenderer = client.textRenderer;

        for (Anchor anchor : AnchorClientRepository.getAll()) {
            BlockPos pos = anchor.pos;
            int argb = anchor.toARGB();
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;

            double dx = pos.getX() - camPos.x;
            double dy = pos.getY() - camPos.y;
            double dz = pos.getZ() - camPos.z;

            // 线框
            matrices.push();
            matrices.translate(dx, dy, dz);
            VertexConsumer lineConsumer = consumers.getBuffer(RenderLayer.getLines());
            drawOutlineBox(matrices, lineConsumer, -EXPAND, -EXPAND, -EXPAND,
                    1 + EXPAND, 1 + EXPAND, 1 + EXPAND, r, g, b, 1.0f);
            matrices.pop();

            // 浮空文字（方块顶部中心上方 0.5 格）
            matrices.push();
            double textX = dx + 0.5;
            double textY = dy + 1.5;
            double textZ = dz + 0.5;
            matrices.translate(textX, textY, textZ);

            // 面向玩家（billboard）
            matrices.multiply(camera.getRotation());
            float scale = 0.025f;
            matrices.scale(-scale, -scale, scale);

            Matrix4f posMatrix = matrices.peek().getPositionMatrix();

            // 第一行：name（锚点颜色）
            if (!anchor.name.isEmpty()) {
                int nameColor = argb | 0xFF000000;
                String nameText = anchor.name;
                float nameX = -textRenderer.getWidth(nameText) / 2f;
                textRenderer.draw(nameText, nameX, -textRenderer.fontHeight - 1,
                        nameColor, false, posMatrix,
                        consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            }

            // 第二行：description（灰色）
            if (!anchor.description.isEmpty()) {
                String descText = anchor.description;
                float descX = -textRenderer.getWidth(descText) / 2f;
                textRenderer.draw(descText, descX, 1,
                        0xFFAAAAAA, false, posMatrix,
                        consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            }
            matrices.pop();
        }
    }

    /**
     * 绘制半透明填充盒（使用 getDebugFilledBox 渲染层）。
     */
    private static void drawFilledBox(MatrixStack matrices, VertexConsumer consumer,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a) {
        Matrix4f m = matrices.peek().getPositionMatrix();

        // 底面 (-Y)
        face(consumer, m, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r, g, b, a);
        // 顶面 (+Y)
        face(consumer, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        // 前面 (-Z)
        face(consumer, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        // 后面 (+Z)
        face(consumer, m, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a);
        // 左面 (-X)
        face(consumer, m, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r, g, b, a);
        // 右面 (+X)
        face(consumer, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
    }

    private static void face(VertexConsumer consumer, Matrix4f m,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float r, float g, float b, float a) {
        // DEBUG_FILLED_BOX 使用 DrawMode.TRIANGLE_STRIP，绘制面要按照 Z 字形 (Zig-Zag)
        // 而 GUI_OVERLAY 使用 DrawMode.QUADS
        // 两者都默认启用 Culling，由于 Winding Order 的影响，导致面显示不全
        // 故需要绘制两次
        // 选择 GUI_OVERLAY 是因为其缓冲区较小，节省性能
        // 具体信息查看 RenderLayer.class
        consumer.vertex(m, x0, y0, z0).color(r, g, b, a).next();
        consumer.vertex(m, x1, y1, z1).color(r, g, b, a).next();
        consumer.vertex(m, x2, y2, z2).color(r, g, b, a).next();
        consumer.vertex(m, x3, y3, z3).color(r, g, b, a).next();

        consumer.vertex(m, x3, y3, z3).color(r, g, b, a).next();
        consumer.vertex(m, x2, y2, z2).color(r, g, b, a).next();
        consumer.vertex(m, x1, y1, z1).color(r, g, b, a).next();
        consumer.vertex(m, x0, y0, z0).color(r, g, b, a).next();
    }

    /** 绘制方块线框（12 条边） */
    private static void drawOutlineBox(MatrixStack matrices, VertexConsumer consumer,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        // 底面四条边
        edge(consumer, m, x0, y0, z0, x1, y0, z0, r, g, b, a);
        edge(consumer, m, x1, y0, z0, x1, y0, z1, r, g, b, a);
        edge(consumer, m, x1, y0, z1, x0, y0, z1, r, g, b, a);
        edge(consumer, m, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // 顶面四条边
        edge(consumer, m, x0, y1, z0, x1, y1, z0, r, g, b, a);
        edge(consumer, m, x1, y1, z0, x1, y1, z1, r, g, b, a);
        edge(consumer, m, x1, y1, z1, x0, y1, z1, r, g, b, a);
        edge(consumer, m, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // 竖向四条边
        edge(consumer, m, x0, y0, z0, x0, y1, z0, r, g, b, a);
        edge(consumer, m, x1, y0, z0, x1, y1, z0, r, g, b, a);
        edge(consumer, m, x1, y0, z1, x1, y1, z1, r, g, b, a);
        edge(consumer, m, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void edge(VertexConsumer consumer, Matrix4f m,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float r, float g, float b, float a) {
        // RenderLayer.getLines() 格式要求：Position, Color, Normal
        // 缺少 Normal 会导致 java.lang.IllegalStateException: Not filled all elements of the
        // vertex
        consumer.vertex(m, x0, y0, z0).color(r, g, b, a).normal(0, 1, 0).next();
        consumer.vertex(m, x1, y1, z1).color(r, g, b, a).normal(0, 1, 0).next();
    }
}
