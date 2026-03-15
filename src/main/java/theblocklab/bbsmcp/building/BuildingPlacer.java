package theblocklab.bbsmcp.building;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.BBSMCP;
import theblocklab.bbsmcp.network.ServerNetwork;

/**
 * 建筑放置器 - 在世界中放置AI生成的建筑
 */
public class BuildingPlacer {

    /**
     * 在世界中放置建筑
     * 
     * @param world  服务端世界
     * @param origin 起始位置
     * @param data   建筑数据
     * @param player 玩家(用于发送进度反馈)
     */
    public static void placeBuilding(ServerWorld world, BlockPos origin, BuildingData data, ServerPlayerEntity player) {
        int totalBlocks = data.blocks.size();
        int placed = 0;

        BBSMCP.LOGGER.info("开始放置建筑,共{}个方块,起始位置:{}", totalBlocks, origin);
        player.sendMessage(Text.literal("§a开始生成建筑,共" + totalBlocks + "个方块..."));

        try {
            for (BuildingData.BlockEntry entry : data.blocks) {
                BlockPos pos = origin.add(entry.x, entry.y, entry.z);

                // 获取方块
                Identifier blockId = new Identifier("minecraft", entry.block);
                Block block = Registries.BLOCK.get(blockId);

                // 放置方块
                world.setBlockState(pos, block.getDefaultState());

                placed++;

                // 每10个方块发送一次进度更新
                if (placed % 10 == 0 || placed == totalBlocks) {
                    int progress = (placed * 100) / totalBlocks;
                    ServerNetwork.sendBuildingProgress(player, progress);

                    // 每100个方块输出一次日志
                    if (placed % 100 == 0) {
                        BBSMCP.LOGGER.info("建筑生成进度: {}/{} ({}%)", placed, totalBlocks, progress);
                    }
                }
            }

            BBSMCP.LOGGER.info("建筑生成完成!");
            player.sendMessage(Text.literal("§a建筑生成完成!"));

        } catch (Exception e) {
            BBSMCP.LOGGER.error("建筑生成失败", e);
            player.sendMessage(Text.literal("§c建筑生成失败: " + e.getMessage()));
        }
    }
}
