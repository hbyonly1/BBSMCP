package theblocklab.bbsmcp.film.clips.utils;

import java.util.List;

import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.clips.Clip;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;

public class ClipValidationChecker {

    /**
     * 检查目标 Clip 是否与轨道上的其他 Clip 发生了时间轴重叠冲突。
     * 
     * @param film        当前胶片工程
     * @param targetClip  待检查的 Clip 数据（无论它是准备新加入的，还是修改后的数据）
     * @param targetIndex 该 Clip 在数组中占据的唯一号（如果是尾部追加则是当前列表大小），用于自身豁免比对
     * @throws BBSMCPException 如果存在同图层的时间片重叠，则抛出异常
     */
    public static void validateNoOverlap(Film film, Clip targetClip, int targetIndex) {
        int targetStart = targetClip.tick.get();
        int targetDuration = targetClip.duration.get();

        if (targetDuration <= 0) {
            throw new BBSMCPException(BBSMCPError.CLIP_DURATION_INVALID, targetDuration);
        }

        int targetEnd = targetStart + targetDuration;
        int targetLayer = targetClip.layer.get();

        List<Clip> clips = film.camera.get();
        for (int i = 0; i < clips.size(); i++) {
            if (i == targetIndex) {
                // 如果是覆盖更新自己所在的插槽，则跳过自己与自己的重合检测
                continue;
            }

            Clip other = clips.get(i);
            int otherLayer = other.layer.get();
            // 只有同一图层的片段才存在相互排斥
            if (otherLayer == targetLayer) {
                int otherStart = other.tick.get();
                int otherEnd = otherStart + other.duration.get();

                // 核心碰撞算法：两条线段有交集 ⟺ (最大起点 < 最小终点)
                if (Math.max(targetStart, otherStart) < Math.min(targetEnd, otherEnd)) {
                    throw new BBSMCPException(
                            BBSMCPError.CLIP_OVERLAPS,
                            targetLayer, targetStart, targetEnd,
                            i, otherStart, otherEnd);
                }
            }
        }
    }
}
