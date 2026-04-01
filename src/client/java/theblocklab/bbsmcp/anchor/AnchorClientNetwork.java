package theblocklab.bbsmcp.anchor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;

/**
 * 客户端锚点网络处理。
 * 负责解析从服务端发来的 S2C 数据包，并更新 AnchorClientRepository。
 */
@Environment(EnvType.CLIENT)
public class AnchorClientNetwork {

    private static final Gson GSON = new Gson();

    /** 处理全量列表下发 */
    public static void handleAnchorList(PacketByteBuf buf) {
        String json = buf.readString();
        AnchorClientRepository.clear();
        try {
            JsonArray array = GSON.fromJson(json, JsonArray.class);
            if (array != null) {
                for (var elem : array) {
                    Anchor anchor = Anchor.fromJson(elem.getAsJsonObject());
                    AnchorClientRepository.put(anchor);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 处理增量更新（UPDATE 或 REMOVE） */
    public static void handleAnchorUpdate(PacketByteBuf buf) {
        String type = buf.readString();
        String json = buf.readString();
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if ("UPDATE".equals(type)) {
                Anchor anchor = Anchor.fromJson(obj);
                AnchorClientRepository.put(anchor);
            } else if ("REMOVE".equals(type)) {
                int id = obj.get("id").getAsInt();
                AnchorClientRepository.remove(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 切换全局可见性 */
    public static void handleToggleVisibility() {
        AnchorClientRepository.toggleVisibility();
    }
}
