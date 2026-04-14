package theblocklab.bbsmcp.region;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 玩家当前选中的轴对齐区域。
 */
public record RegionSelection(BlockPos pos1, BlockPos pos2) {

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public BlockPos min() {
        requireComplete();
        return new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
    }

    public BlockPos max() {
        requireComplete();
        return new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    public int sizeX() {
        return max().getX() - min().getX() + 1;
    }

    public int sizeY() {
        return max().getY() - min().getY() + 1;
    }

    public int sizeZ() {
        return max().getZ() - min().getZ() + 1;
    }

    public int volume() {
        return sizeX() * sizeY() * sizeZ();
    }

    public String regionKey(World world) {
        BlockPos min = min();
        BlockPos max = max();
        return world.getRegistryKey().getValue() + "|" +
            min.getX() + "," + min.getY() + "," + min.getZ() + "|" +
            max.getX() + "," + max.getY() + "," + max.getZ();
    }

    private void requireComplete() {
        if (!isComplete()) {
            throw new IllegalStateException("RegionSelection is incomplete");
        }
    }
}
