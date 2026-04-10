package theblocklab.bbsmcp.building;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;


/**
 * 客户端建筑系统状态仓库（单例静态字段）。
 * 持有当前蓝图和旋转状态，供渲染器和交互事件使用。
 */
@Environment(EnvType.CLIENT)
public class BuildingClientRepository {

    /** 玩家前方预览距离（格），可动态调节 */
    public static double previewDistance = 10.0;

    public static BuildingBlueprint current = null;
    public static String currentName = "";
    public static int rotation = 0;   // 0/1/2/3 → 0°/90°/180°/270°

    /** 左键触发：顺时针旋转 +90° */
    public static void rotateLeft() {
        rotation = (rotation + 1) % 4;
    }

    /** 清除当前蓝图和旋转状态 */
    public static void clear() {
        current     = null;
        currentName = "";
        rotation    = 0;
    }

    public static boolean isEmpty() {
        return current == null;
    }

    /**
     * 进行视线追踪计算预览原点。最大距离 previewDistance。
     * 如果击中方块，则原点为被击中方块根据击中面偏移后的坐标（吸附在表面）。
     * 如果未击中，则原点为视线终点的整数坐标。
     */
    public static BlockPos getPreviewOrigin(PlayerEntity player) {
        net.minecraft.util.hit.HitResult hit = player.raycast(previewDistance, 1.0F, false);
        if (hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            net.minecraft.util.hit.BlockHitResult blockHit = (net.minecraft.util.hit.BlockHitResult) hit;
            return blockHit.getBlockPos().offset(blockHit.getSide());
        }
        return BlockPos.ofFloored(hit.getPos());
    }

    /** 将旋转值渲染为可读字符串 */
    public static String getRotationLabel() {
        return switch (rotation) {
            case 1 -> "+90°";
            case 2 -> "+180°";
            case 3 -> "+270°";
            default -> "原始";
        };
    }
}
