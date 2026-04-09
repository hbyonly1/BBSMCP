package theblocklab.bbsmcp.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class RenderUtils {

    /**
     * 绘制半透明填充盒（使用 getDebugFilledBox 渲染层）。
     */
    public static void drawFilledBox(MatrixStack matrices, VertexConsumer consumer,
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
    public static void drawOutlineBox(MatrixStack matrices, VertexConsumer consumer,
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
