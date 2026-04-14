package theblocklab.bbsmcp.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * 统一的 JSON 美化工具。
 * 只要传入的是合法 JSON 字符串，就返回 pretty-print 版本；否则原样返回。
 */
public class JsonFormatUtils {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonFormatUtils() {}

    public static String pretty(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }

        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return PRETTY_GSON.toJson(parsed);
        } catch (Exception ignored) {
            return raw;
        }
    }
}
