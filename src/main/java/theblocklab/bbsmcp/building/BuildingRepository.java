package theblocklab.bbsmcp.building;

/**
 * 服务端全局建筑蓝图存储（单例）。
 * 同一时刻只有一个待放置的蓝图。
 */
public class BuildingRepository {

    public static BuildingBlueprint current = null;
    public static String currentName = "";
    /** 原始 JSON，用于 S2C 下发给客户端 */
    public static String rawJson = "";

    public static void set(BuildingBlueprint blueprint) {
        current     = blueprint;
        currentName = blueprint.name;
        rawJson     = blueprint.rawJson;
    }

    public static void clear() {
        current     = null;
        currentName = "";
        rawJson     = "";
    }

    public static boolean isEmpty() {
        return current == null;
    }
}
