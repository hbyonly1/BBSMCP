package theblocklab.bbsmcp.film.clips.utils;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.utils.clips.Clip;

public class ClipDataConverter {

    /**
     * 将 BBS Mod 原生的 Clip 转换为携带 index 和 type 的标准 JsonObject
     */
    public static JsonObject convertClipToJson(Clip clip, int index) {
        MapType mapData = (MapType) clip.toData();
        JsonObject originalObject = (JsonObject) convertBaseTypeToJson(mapData);

        JsonObject jsonObject = new JsonObject();

        // 显式注入缺失的标识符，放在最前面以高度贴合 schema 阅读习惯
        String name = clip.getClass().getSimpleName().replace("Clip", "").toLowerCase();
        jsonObject.addProperty("type", name);
        jsonObject.addProperty("index", index);

        // 将原生对象的全部其余属性按顺序延后追加
        for (Entry<String, JsonElement> entry : originalObject.entrySet()) {
            jsonObject.add(entry.getKey(), entry.getValue());
        }

        return jsonObject;
    }

    /**
     * 递归将 BBS NBT 底层的 BaseType 树完全转换为 Gson 的 JsonElement 树
     */
    public static JsonElement convertBaseTypeToJson(BaseType baseType) {
        if (baseType == null) {
            return null;
        }

        if (baseType.isMap()) {
            JsonObject jsonObject = new JsonObject();
            MapType mapType = baseType.asMap();
            for (String key : mapType.keys()) {
                jsonObject.add(key, convertBaseTypeToJson(mapType.get(key)));
            }
            return jsonObject;
        } else if (baseType.isList()) {
            JsonArray jsonArray = new JsonArray();
            ListType listType = baseType.asList();
            for (int i = 0; i < listType.size(); i++) {
                jsonArray.add(convertBaseTypeToJson(listType.get(i)));
            }
            return jsonArray;
        } else if (baseType instanceof StringType) {
            return new JsonPrimitive(baseType.asString());
        } else if (baseType instanceof mchorse.bbs_mod.data.types.ByteType ||
                baseType instanceof mchorse.bbs_mod.data.types.ShortType ||
                baseType instanceof mchorse.bbs_mod.data.types.IntType ||
                baseType instanceof mchorse.bbs_mod.data.types.LongType ||
                baseType instanceof mchorse.bbs_mod.data.types.FloatType ||
                baseType instanceof mchorse.bbs_mod.data.types.DoubleType) {
            String str = baseType.toString();
            // NBT 类型在 toString 时通常带有后缀，如 1b, 10.0f, 5d，掐掉结尾字母
            if (str.length() > 0 && Character.isLetter(str.charAt(str.length() - 1))) {
                str = str.substring(0, str.length() - 1);
            }
            try {
                if (baseType instanceof mchorse.bbs_mod.data.types.ByteType) {
                    byte v = Byte.parseByte(str);
                    if (v == 0 || v == 1)
                        return new JsonPrimitive(v == 1); // 智能布尔
                }
                if (str.contains(".")) {
                    return new JsonPrimitive(Double.parseDouble(str));
                } else {
                    return new JsonPrimitive(Long.parseLong(str));
                }
            } catch (Exception e) {
                return new JsonPrimitive(str);
            }
        }

        // 如果遇到未知类型，安全退回普通 toString()
        return new JsonPrimitive(baseType.toString());
    }
}
