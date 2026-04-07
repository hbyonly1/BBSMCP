package theblocklab.bbsmcp.anchor;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * 锚点系统的高级 API。
 * 封装了底层数据操作与网络同步逻辑，是外部访问的推荐入口。
 */
public class AnchorManagerAPI {
    public static final AnchorManagerAPI INSTANCE = new AnchorManagerAPI();

    private AnchorManagerAPI() {}

    /** 获取全量锚点 JSON */
    public String getAllAsJson() {
        return AnchorManager.INSTANCE.toJsonString();
    }

    /** 创建并同步 */
    public Anchor create(ServerPlayerEntity player, BlockPos pos, String name, String description, String color) {
        Anchor anchor = AnchorManager.INSTANCE.create(pos, name, description, color);
        AnchorServerNetwork.sendAnchorUpdatePacket(player, anchor);
        return anchor;
    }

    /** 删除并同步 */
    public boolean remove(ServerPlayerEntity player, int id) {
        if (AnchorManager.INSTANCE.remove(id)) {
            AnchorServerNetwork.sendAnchorRemovePacket(player, id);
            return true;
        }
        return false;
    }

    /** 更新并同步 */
    public boolean update(ServerPlayerEntity player, int id, String name, String description, String color) {
        if (AnchorManager.INSTANCE.update(id, name, description, color)) {
            AnchorServerNetwork.sendAnchorUpdatePacket(player, AnchorManager.INSTANCE.get(id));
            return true;
        }
        return false;
    }

    /** 清空并同步全量列表 */
    public void removeAll(ServerPlayerEntity player) {
        AnchorManager.INSTANCE.removeAll();
        AnchorServerNetwork.sendAnchorListPacket(player);
    }

    // ────────── Camera Hints ──────────

    /** 添加一个 camera hint 到锚点 */
    public boolean addCameraHint(int anchorId, com.google.gson.JsonObject hint) {
        return AnchorManager.INSTANCE.addCameraHint(anchorId, hint);
    }

    /** 删除一个 camera hint */
    public boolean removeCameraHint(int anchorId, int hintId) {
        return AnchorManager.INSTANCE.removeCameraHint(anchorId, hintId);
    }

    /** 标记某 hint 为首选 */
    public boolean setPreferredHint(int anchorId, int hintId) {
        return AnchorManager.INSTANCE.setPreferredHint(anchorId, hintId);
    }

    /** 清空某锚点所有 hints */
    public boolean clearCameraHints(int anchorId) {
        return AnchorManager.INSTANCE.clearCameraHints(anchorId);
    }

    /** 获取指定锚点的详细数据（含 camera_hints） */
    public String getAnchorJson(int anchorId) {
        Anchor anchor = AnchorManager.INSTANCE.get(anchorId);
        if (anchor == null) return null;
        return anchor.toJson().toString();
    }
}
