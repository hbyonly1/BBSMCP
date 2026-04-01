package theblocklab.bbsmcp.film.clips.utils;
 
 import java.util.ArrayList;
 import java.util.List;
 import mchorse.bbs_mod.utils.clips.Clip;
 import theblocklab.bbsmcp.exception.BBSMCPError;
 import theblocklab.bbsmcp.exception.BBSMCPException;
 
 /**
  * Clip 批量平移防碰撞校验器
  */
 public class ClipShiftValidator {
 
     /**
      * 检查批量平移操作是否会导致与轨道上现有的（不参与平移的）Clip 发生重叠冲突。
      * 
      * @param allClips     当前轨道上的所有 Clip 列表
      * @param startTick    平移范围起始 tick
      * @param endTick      平移范围结束 tick（-1 表示无限）
      * @param offsetTick   平移偏移量
      * @throws BBSMCPException 如果偏移后发生重叠，则抛出异常
      */
     public static void validateShift(List<Clip> allClips, int startTick, int endTick, float offsetTick) {
         List<Clip> shiftedClips = new ArrayList<>();
         List<Clip> staticClips = new ArrayList<>();
 
         // 1. 分组：找出哪些在变，哪些没变
         for (Clip clip : allClips) {
             int currentTick = (Integer) clip.tick.get();
             int duration = (Integer) clip.duration.get();
             
             // 逻辑与 ClipManagerAPI 中的筛选条件保持一致
             if (currentTick + duration > startTick && (endTick < 0 || currentTick <= endTick)) {
                 shiftedClips.add(clip);
             } else {
                 staticClips.add(clip);
             }
         }
 
         // 2. 交叉对比：检查每一个“平移后的 Clip”是否撞上了“原地的 Clip”
         for (Clip sc : shiftedClips) {
             int newStart = Math.round((float)(Integer) sc.tick.get() + offsetTick);
             int duration = (Integer) sc.duration.get();
             int newEnd = newStart + duration;
             int layer = (Integer) sc.layer.get();
 
             for (int i = 0; i < staticClips.size(); i++) {
                 Clip st = staticClips.get(i);
                 // 只有同图层才会有物理排斥
                 if ((Integer) st.layer.get() == layer) {
                     int stStart = (Integer) st.tick.get();
                     int stEnd = stStart + (Integer) st.duration.get();
 
                     // 线段重叠逻辑：Math.max(start1, start2) < Math.min(end1, end2)
                     if (Math.max(newStart, stStart) < Math.min(newEnd, stEnd)) {
                         // 抛出异常，指明冲突
                         throw new BBSMCPException(
                             BBSMCPError.CLIP_OVERLAPS,
                             layer, newStart, newEnd,
                             -1, stStart, stEnd // 这里 -1 表示是一个批量操作导致的冲突
                         );
                     }
                 }
             }
         }
     }
 }
