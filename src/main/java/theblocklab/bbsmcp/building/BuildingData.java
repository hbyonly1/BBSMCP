package theblocklab.bbsmcp.building;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import theblocklab.bbsmcp.BBSMCP;

import java.util.ArrayList;
import java.util.List;

/**
 * AI生成的建筑数据
 */
public class BuildingData {

    /**
     * 单个方块条目
     */
    public static class BlockEntry {
        public int x;
        public int y;
        public int z;
        public String block; // 方块ID,无minecraft:前缀

        public BlockEntry() {
        }

        public BlockEntry(int x, int y, int z, String block) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
        }
    }

    public List<BlockEntry> blocks = new ArrayList<>();

    /**
     * 从JSON字符串解析建筑数据
     */
    public static BuildingData fromJson(String json) throws JsonSyntaxException {
        return new Gson().fromJson(json, BuildingData.class);
    }

    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * 验证建筑数据是否有效
     * 
     * @return 是否所有方块ID都有效
     */
    public boolean validate() {
        if (blocks == null || blocks.isEmpty()) {
            BBSMCP.LOGGER.warn("建筑数据为空");
            return false;
        }

        for (BlockEntry entry : blocks) {
            if (entry.block == null || entry.block.isEmpty()) {
                BBSMCP.LOGGER.warn("方块ID为空: x={}, y={}, z={}", entry.x, entry.y, entry.z);
                return false;
            }

            // 验证方块ID是否存在
            Identifier blockId = new Identifier("minecraft", entry.block);
            if (!Registries.BLOCK.containsId(blockId)) {
                BBSMCP.LOGGER.warn("无效的方块ID: {}", entry.block);
                return false;
            }
        }

        return true;
    }

    /**
     * 获取建筑的边界尺寸
     */
    public int[] getBounds() {
        if (blocks.isEmpty()) {
            return new int[] { 0, 0, 0 };
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockEntry entry : blocks) {
            minX = Math.min(minX, entry.x);
            maxX = Math.max(maxX, entry.x);
            minY = Math.min(minY, entry.y);
            maxY = Math.max(maxY, entry.y);
            minZ = Math.min(minZ, entry.z);
            maxZ = Math.max(maxZ, entry.z);
        }

        return new int[] { maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1 };
    }
}
