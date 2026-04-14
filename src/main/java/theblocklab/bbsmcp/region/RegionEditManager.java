package theblocklab.bbsmcp.region;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 区域编辑的服务端状态仓库。
 */
public final class RegionEditManager {

    public static final RegionEditManager INSTANCE = new RegionEditManager();
    public static final int MAX_SELECTION_VOLUME = 8192;

    private final Map<UUID, RegionSession> sessions = new ConcurrentHashMap<>();

    private RegionEditManager() {}

    public RegionSession getOrCreate(ServerPlayerEntity player) {
        return sessions.computeIfAbsent(player.getUuid(), ignored -> new RegionSession());
    }

    public RegionSelection getSelection(ServerPlayerEntity player) {
        return getOrCreate(player).getSelection();
    }

    public void setPos1(ServerPlayerEntity player, BlockPos pos) {
        getOrCreate(player).setPos1(pos);
    }

    public void setPos2(ServerPlayerEntity player, BlockPos pos) {
        getOrCreate(player).setPos2(pos);
    }

    public PreviewState setPreview(ServerPlayerEntity player, PreviewState preview) {
        return getOrCreate(player).setPreview(preview);
    }

    public PreviewState getPreview(ServerPlayerEntity player) {
        return getOrCreate(player).getPreview();
    }

    public void clearPreview(ServerPlayerEntity player) {
        getOrCreate(player).clearPreview();
    }

    public void pushHistory(ServerPlayerEntity player, String regionKey, HistoryEntry entry) {
        getOrCreate(player).pushHistory(regionKey, entry);
    }

    public HistoryEntry popHistory(ServerPlayerEntity player, String regionKey) {
        return getOrCreate(player).popHistory(regionKey);
    }

    public record BlockSnapshot(BlockPos pos, String blockSpec) {}

    public record BlockChange(BlockPos pos, String beforeSpec, String afterSpec) {}

    public record PreviewState(String regionKey, List<BlockChange> changes) {}

    public record HistoryEntry(String regionKey, List<BlockChange> changes) {}

    public static final class RegionSession {
        private BlockPos pos1;
        private BlockPos pos2;
        private PreviewState preview;
        private final Map<String, Deque<HistoryEntry>> historyByRegion = new HashMap<>();

        public synchronized void setPos1(BlockPos pos) {
            this.pos1 = pos.toImmutable();
            clearPreview();
        }

        public synchronized void setPos2(BlockPos pos) {
            this.pos2 = pos.toImmutable();
            clearPreview();
        }

        public synchronized RegionSelection getSelection() {
            return new RegionSelection(pos1, pos2);
        }

        public synchronized PreviewState setPreview(PreviewState preview) {
            this.preview = preview;
            return preview;
        }

        public synchronized PreviewState getPreview() {
            return preview;
        }

        public synchronized void clearPreview() {
            this.preview = null;
        }

        public synchronized void pushHistory(String regionKey, HistoryEntry entry) {
            historyByRegion.computeIfAbsent(regionKey, ignored -> new ArrayDeque<>()).push(entry);
        }

        public synchronized HistoryEntry popHistory(String regionKey) {
            Deque<HistoryEntry> stack = historyByRegion.get(regionKey);
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            return stack.pop();
        }
    }

    public List<BlockSnapshot> captureSelection(World world, RegionSelection selection) {
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        List<BlockSnapshot> snapshots = new ArrayList<>(selection.volume());
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    snapshots.add(new BlockSnapshot(pos, RegionBlockStateCodec.serialize(world.getBlockState(pos))));
                }
            }
        }
        return snapshots;
    }

    public List<BlockChange> buildChanges(World world, RegionSelection selection, Collection<PatchEntry> patchEntries) {
        Map<BlockPos, String> patchByPos = new LinkedHashMap<>();
        BlockPos min = selection.min();
        BlockPos max = selection.max();

        for (PatchEntry entry : patchEntries) {
            BlockPos relative = entry.relativePos();
            if (relative.getX() < 0 || relative.getY() < 0 || relative.getZ() < 0
                || relative.getX() >= selection.sizeX()
                || relative.getY() >= selection.sizeY()
                || relative.getZ() >= selection.sizeZ()) {
                throw new IllegalArgumentException(String.format(
                    "补丁坐标 (%d,%d,%d) 超出当前选区尺寸 (%d,%d,%d)",
                    relative.getX(), relative.getY(), relative.getZ(),
                    selection.sizeX(), selection.sizeY(), selection.sizeZ()
                ));
            }

            BlockPos absolute = min.add(relative);
            if (absolute.getX() < min.getX() || absolute.getY() < min.getY() || absolute.getZ() < min.getZ()
                || absolute.getX() > max.getX() || absolute.getY() > max.getY() || absolute.getZ() > max.getZ()) {
                throw new IllegalArgumentException("补丁坐标超出当前选区");
            }

            patchByPos.put(absolute, RegionBlockStateCodec.normalizeSpec(entry.blockSpec()));
        }

        List<BlockChange> changes = new ArrayList<>(patchByPos.size());
        for (Map.Entry<BlockPos, String> entry : patchByPos.entrySet()) {
            String before = RegionBlockStateCodec.serialize(world.getBlockState(entry.getKey()));
            String after = entry.getValue();
            if (!before.equals(after)) {
                changes.add(new BlockChange(entry.getKey().toImmutable(), before, after));
            }
        }
        return changes;
    }

    public record PatchEntry(BlockPos relativePos, String blockSpec) {}
}
