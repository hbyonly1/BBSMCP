package theblocklab.bbsmcp.anchor;

import com.google.gson.JsonObject;
import net.minecraft.util.math.BlockPos;

/**
 * 锚点数据类。
 */
public class Anchor {
    public final int id;
    public final BlockPos pos;
    public String name;
    public String description;
    public String color; // Hex string: #RRGGBB

    public Anchor(int id, BlockPos pos, String name, String description, String color) {
        this.id = id;
        this.pos = pos;
        this.name = name;
        this.description = description;
        this.color = color;
    }

    public int toARGB() {
        try {
            return Integer.parseInt(color.replace("#", ""), 16) | 0xFF000000;
        } catch (Exception e) {
            return 0xFF4488FF; // 默认蓝色
        }
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("x", pos.getX());
        obj.addProperty("y", pos.getY());
        obj.addProperty("z", pos.getZ());
        obj.addProperty("name", name);
        obj.addProperty("description", description);
        obj.addProperty("color", color);
        return obj;
    }

    public static Anchor fromJson(JsonObject obj) {
        int id = obj.get("id").getAsInt();
        int x = obj.get("x").getAsInt();
        int y = obj.get("y").getAsInt();
        int z = obj.get("z").getAsInt();
        String name = obj.get("name").getAsString();
        String description = obj.get("description").getAsString();
        String color = obj.get("color").getAsString();
        return new Anchor(id, new BlockPos(x, y, z), name, description, color);
    }
}
