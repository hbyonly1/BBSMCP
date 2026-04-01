package theblocklab.bbsmcp.anchor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端锚点仓库。
 * 由服务端 S2C 数据包驱动，不直接访问磁盘。
 */
@Environment(EnvType.CLIENT)
public class AnchorClientRepository {

    /** 主存储：ID -> Anchor */
    private static final Map<Integer, Anchor> anchors = new LinkedHashMap<>();

    /** 全局显示开关 */
    private static boolean visible = true;

    // ────────── CRUD ──────────

    public static void clear() {
        anchors.clear();
    }

    public static void put(Anchor anchor) {
        anchors.put(anchor.id, anchor);
    }

    public static void remove(int id) {
        anchors.remove(id);
    }

    // ────────── 查询 ──────────

    public static boolean isVisible() {
        return visible;
    }

    public static Collection<Anchor> getAll() {
        return Collections.unmodifiableCollection(anchors.values());
    }

    public static Anchor get(int id) {
        return anchors.get(id);
    }

    public static void toggleVisibility() {
        visible = !visible;
    }
}
