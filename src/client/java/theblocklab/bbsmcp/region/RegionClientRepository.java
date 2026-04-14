package theblocklab.bbsmcp.region;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class RegionClientRepository {

    private static BlockPos pos1;
    private static BlockPos pos2;
    private static String previewRegionKey;
    private static List<PreviewChange> previewChanges = new ArrayList<>();

    private RegionClientRepository() {}

    public static BlockPos getPos1() {
        return pos1;
    }

    public static void setPos1(BlockPos pos) {
        pos1 = pos == null ? null : pos.toImmutable();
        clearPreview();
    }

    public static BlockPos getPos2() {
        return pos2;
    }

    public static void setPos2(BlockPos pos) {
        pos2 = pos == null ? null : pos.toImmutable();
        clearPreview();
    }

    public static boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public static BlockPos min() {
        if (!isComplete()) return null;
        return new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
    }

    public static BlockPos max() {
        if (!isComplete()) return null;
        return new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    public static void setPreview(String regionKey, List<PreviewChange> changes) {
        previewRegionKey = regionKey;
        previewChanges = List.copyOf(changes);
    }

    public static String getPreviewRegionKey() {
        return previewRegionKey;
    }

    public static List<PreviewChange> getPreviewChanges() {
        return previewChanges;
    }

    public static void clearPreview() {
        previewRegionKey = null;
        previewChanges = new ArrayList<>();
    }

    public record PreviewChange(BlockPos pos, String beforeSpec, String afterSpec) {}
}
