package theblocklab.bbsmcp.building;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 建筑蓝图数据模型。
 * 解析 AI 生成的极简 JSON 格式，支持 blocks/fills/anchors 三种区段，
 * 并提供旋转坐标变换（0/1/2/3 对应 0°/90°/180°/270°）。
 *
 * <p>JSON 格式：
 * <pre>
 * {
 *   "name": "小屋",
 *   "blocks": [[dx,dy,dz,"block_id"], ...],
 *   "fills":  [[x1,y1,z1,x2,y2,z2,"block_id"], ...],
 *   "anchors":[[dx,dy,dz,"name","desc"], ...]
 * }
 * </pre>
 *
 * <p>block_id 规则：不含 ':' 时自动追加 'minecraft:' 前缀。
 */
public class BuildingBlueprint {

    public final String name;
    public final int blockCount;      // single blocks
    public final int fillCount;       // fill regions
    public final int anchorCount;

    /** 原始 JSON 字符串，用于 S2C 下发给客户端 */
    public final String rawJson;

    // 建筑包围盒中心信息（用于旋转轴和放置点对齐）
    public final double centerX;
    public final double centerZ;
    public final int minY;

    // 内部存储：相对偏移
    private final List<int[]> blocks  = new ArrayList<>();   // [dx,dy,dz]
    private final List<String> blockIds = new ArrayList<>();

    private final List<int[]> fills   = new ArrayList<>();   // [x1,y1,z1,x2,y2,z2]
    private final List<String> fillIds = new ArrayList<>();

    private final List<int[]> anchorOffsets = new ArrayList<>();  // [dx,dy,dz]
    private final List<String> anchorNames  = new ArrayList<>();
    private final List<String> anchorDescs  = new ArrayList<>();

    // ────────────────────────────────────────────────────────────

    public BuildingBlueprint(String json) {
        this.rawJson = json;
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        this.name = root.has("name") ? root.get("name").getAsString() : "Building";

        // 解析 blocks
        if (root.has("blocks")) {
            for (JsonElement elem : root.getAsJsonArray("blocks")) {
                JsonArray arr = elem.getAsJsonArray();
                blocks.add(new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt()});
                blockIds.add(normalizeBlockId(arr.get(3).getAsString()));
            }
        }

        // 解析 fills
        if (root.has("fills")) {
            for (JsonElement elem : root.getAsJsonArray("fills")) {
                JsonArray arr = elem.getAsJsonArray();
                fills.add(new int[]{
                    arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt(),
                    arr.get(3).getAsInt(), arr.get(4).getAsInt(), arr.get(5).getAsInt()
                });
                fillIds.add(normalizeBlockId(arr.get(6).getAsString()));
            }
        }

        // 解析 anchors
        if (root.has("anchors")) {
            for (JsonElement elem : root.getAsJsonArray("anchors")) {
                JsonArray arr = elem.getAsJsonArray();
                anchorOffsets.add(new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt()});
                anchorNames.add(arr.get(3).getAsString());
                anchorDescs.add(arr.size() > 4 ? arr.get(4).getAsString() : "");
            }
        }

        this.blockCount  = blocks.size();
        this.fillCount   = fills.size();
        this.anchorCount = anchorOffsets.size();

        // 计算包围盒
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;

        for (int[] b : blocks) {
            minX = Math.min(minX, b[0]); maxX = Math.max(maxX, b[0]);
            minY = Math.min(minY, b[1]);
            minZ = Math.min(minZ, b[2]); maxZ = Math.max(maxZ, b[2]);
        }
        for (int[] f : fills) {
            minX = Math.min(minX, Math.min(f[0], f[3])); maxX = Math.max(maxX, Math.max(f[0], f[3]));
            minY = Math.min(minY, Math.min(f[1], f[4]));
            minZ = Math.min(minZ, Math.min(f[2], f[5])); maxZ = Math.max(maxZ, Math.max(f[2], f[5]));
        }

        if (minX == Integer.MAX_VALUE) {
            this.centerX = 0; this.centerZ = 0; this.minY = 0;
        } else {
            this.centerX = (minX + maxX) / 2.0;
            this.centerZ = (minZ + maxZ) / 2.0;
            this.minY = minY;
        }
    }

    // ── 坐标变换 ────────────────────────────────────────────────

    /**
     * 将蓝图内部坐标系 (x, y, z) 进行：平移到中心 -> 旋转 -> 平移到放置目标点 的复合变换。
     * 保证建筑绕自身中心旋转，且目标放置点恰好位于建筑的底层中心。
     * rotation: 0=原始, 1=+90°(顺时针), 2=+180°, 3=+270°
     */
    private BlockPos transform(int x, int y, int z, BlockPos origin, int rotation) {
        // 平移到以底层中心为原点 (-1.5 等于靠左 1.5 格)
        double rx = x - this.centerX;
        double rz = z - this.centerZ;

        // 旋转
        double rotX, rotZ;
        switch (rotation) {
            case 1 -> { rotX = rz; rotZ = -rx; }
            case 2 -> { rotX = -rx; rotZ = -rz; }
            case 3 -> { rotX = -rz; rotZ = rx; }
            default -> { rotX = rx; rotZ = rz; }
        }

        // 重新平移到目标点，并计算最终整数坐标
        int finalX = (int) Math.round(origin.getX() + rotX);
        int finalY = origin.getY() + (y - this.minY); // y=minY 时刚好在原点 Y 上
        int finalZ = (int) Math.round(origin.getZ() + rotZ);

        return new BlockPos(finalX, finalY, finalZ);
    }

    // ── 公开 API ────────────────────────────────────────────────

    /**
     * 展开所有方块（fills 先铺底，blocks 后覆盖）为绝对坐标列表。
     */
    public List<PlacedBlock> getAbsoluteBlocks(BlockPos origin, int rotation) {
        // 用 LinkedHashMap 保证 fills 先写、blocks 后覆盖
        Map<BlockPos, String> map = new LinkedHashMap<>();

        // 先处理 fills
        for (int i = 0; i < fills.size(); i++) {
            int[] r = fills.get(i);
            int x1 = Math.min(r[0], r[3]), x2 = Math.max(r[0], r[3]);
            int y1 = Math.min(r[1], r[4]), y2 = Math.max(r[1], r[4]);
            int z1 = Math.min(r[2], r[5]), z2 = Math.max(r[2], r[5]);
            String id = fillIds.get(i);
            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    for (int z = z1; z <= z2; z++) {
                        map.put(transform(x, y, z, origin, rotation), id);
                    }
                }
            }
        }

        // 再处理单块（覆盖 fills）
        for (int i = 0; i < blocks.size(); i++) {
            int[] b = blocks.get(i);
            map.put(transform(b[0], b[1], b[2], origin, rotation), blockIds.get(i));
        }

        List<PlacedBlock> result = new ArrayList<>(map.size());
        map.forEach((pos, id) -> result.add(new PlacedBlock(pos, id)));
        return result;
    }

    /**
     * 获取内嵌 Anchor 的绝对坐标和元数据。
     */
    public List<AnchorPlacement> getAbsoluteAnchors(BlockPos origin, int rotation) {
        List<AnchorPlacement> result = new ArrayList<>();
        for (int i = 0; i < anchorOffsets.size(); i++) {
            int[] a = anchorOffsets.get(i);
            result.add(new AnchorPlacement(
                transform(a[0], a[1], a[2], origin, rotation),
                anchorNames.get(i),
                anchorDescs.get(i)
            ));
        }
        return result;
    }

    // ── 工具方法 ────────────────────────────────────────────────

    /** 若 id 不含 ':' 则自动添加 'minecraft:' 前缀 */
    public static String normalizeBlockId(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }

    // ── 内部数据类 ───────────────────────────────────────────────
    // 数据载体类
    /** 单个已确定坐标和方块 ID 的放置项 */
    public record PlacedBlock(BlockPos pos, String blockId) {}

    /** 单个内嵌 Anchor 放置信息 */
    public record AnchorPlacement(BlockPos pos, String name, String desc) {}
}
