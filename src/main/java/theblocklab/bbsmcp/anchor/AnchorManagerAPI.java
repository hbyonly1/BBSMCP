package theblocklab.bbsmcp.anchor;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;

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
    public void remove(ServerPlayerEntity player, int id) {
        if (!AnchorManager.INSTANCE.remove(id)) {
            throw new BBSMCPException(BBSMCPError.ANCHOR_NOT_FOUND, id);
        }
        AnchorServerNetwork.sendAnchorRemovePacket(player, id);
    }

    /** 更新并同步 */
    public void update(ServerPlayerEntity player, int id, String name, String description, String color) {
        if (!AnchorManager.INSTANCE.update(id, name, description, color)) {
            throw new BBSMCPException(BBSMCPError.ANCHOR_NOT_FOUND, id);
        }
        AnchorServerNetwork.sendAnchorUpdatePacket(player, AnchorManager.INSTANCE.get(id));
    }

    /** 清空并同步全量列表 */
    public void removeAll(ServerPlayerEntity player) {
        AnchorManager.INSTANCE.removeAll();
        AnchorServerNetwork.sendAnchorListPacket(player);
    }

    // ────────── Camera Hints ──────────
    
    /** 添加一个 camera hint 到锚点 */
    public void addCameraHint(ServerPlayerEntity player, int anchorId, com.google.gson.JsonObject hint) {
        if (!AnchorManager.INSTANCE.addCameraHint(anchorId, hint)) {
            throw new BBSMCPException(BBSMCPError.ANCHOR_NOT_FOUND, anchorId);
        }
        AnchorServerNetwork.sendAnchorUpdatePacket(player, AnchorManager.INSTANCE.get(anchorId));
    }

    /** 删除一个 camera hint */
    public void removeCameraHint(ServerPlayerEntity player, int anchorId, int hintId) {
        if (!AnchorManager.INSTANCE.removeCameraHint(anchorId, hintId)) {
            // 这里可能是锚点不存在，也可能是 hintId 不存在，通常底层返回 false 统一抛错即可
            throw new BBSMCPException(BBSMCPError.ANCHOR_HINT_NOT_FOUND, anchorId, hintId);
        }
        AnchorServerNetwork.sendAnchorUpdatePacket(player, AnchorManager.INSTANCE.get(anchorId));
    }

    /** 标记某 hint 为首选 */
    public void setPreferredHint(ServerPlayerEntity player, int anchorId, int hintId) {
        if (!AnchorManager.INSTANCE.setPreferredHint(anchorId, hintId)) {
            throw new BBSMCPException(BBSMCPError.ANCHOR_HINT_NOT_FOUND, anchorId, hintId);
        }
        AnchorServerNetwork.sendAnchorUpdatePacket(player, AnchorManager.INSTANCE.get(anchorId));
    }

    /** 更新某个 hint 内容 */
    public void updateCameraHint(ServerPlayerEntity player, int anchorId, int hintId, com.google.gson.JsonObject updatedHint) {
        if (!AnchorManager.INSTANCE.updateCameraHint(anchorId, hintId, updatedHint)) {
            throw new BBSMCPException(BBSMCPError.ANCHOR_HINT_NOT_FOUND, anchorId, hintId);
        }
        AnchorServerNetwork.sendAnchorUpdatePacket(player, AnchorManager.INSTANCE.get(anchorId));
    }

    /** 清空某锚点所有 hints */
    public void clearCameraHints(ServerPlayerEntity player, int anchorId) {
        if (!AnchorManager.INSTANCE.clearCameraHints(anchorId)) {
            throw new BBSMCPException(BBSMCPError.ANCHOR_NOT_FOUND, anchorId);
        }
        AnchorServerNetwork.sendAnchorUpdatePacket(player, AnchorManager.INSTANCE.get(anchorId));
    }

    /** 获取指定锚点的详细数据（含 camera_hints） */
    public String getAnchorJson(int anchorId) {
        Anchor anchor = AnchorManager.INSTANCE.get(anchorId);
        if (anchor == null) return null;
        return anchor.toJson().toString();
    }
}
