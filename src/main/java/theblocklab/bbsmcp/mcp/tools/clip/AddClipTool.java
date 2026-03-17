package theblocklab.bbsmcp.mcp.tools.clip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
import theblocklab.bbsmcp.mcp.tools.core.MCPTool;
import theblocklab.bbsmcp.mcp.tools.core.MCPToolResponse;

/**
 * MCP 工具：向指定 Film 添加一个 Clip 镜头
 */
public class AddClipTool extends MCPTool {

    public AddClipTool() {
        super("add_clip", "向指定 Film 添加或更新一个 Clip 镜头");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": {
                      "type": "string",
                      "description": "目标 Film 的 ID"
                    },
                    "json": {
                      "type": "string",
                      "description": "Clip 数据，JSON 对象格式。必须包含 'index' 字段且范围在 [0, current_size] 之间。可通过 GetClipSchemaTool 获取各类型所需字段及插值常量。"
                    }
                  },
                  "required": ["filmId", "json"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) throws Exception {
        String filmId = arguments.get("filmId").getAsString();
        String json = arguments.get("json").getAsString();

        // 检查 Film 是否存在
        if (!FilmManagerAPI.INSTANCE.getFilmsList().contains(filmId)) {
            return MCPToolResponse.error(
                    "添加 Clip 失败：Film '" + filmId + "' 不存在",
                    "请先使用 create_film 创建 Film，或用 get_film_list 查看已有 Film 列表。"
            );
        }

        // 找一个在线玩家作为同步目标
        ServerPlayerEntity player = server.getPlayerManager().getPlayerList().isEmpty()
                ? null
                : server.getPlayerManager().getPlayerList().get(0);

        if (player == null) {
            return MCPToolResponse.error("添加 Clip 失败：服务器内没有在线玩家", "同步 Film 需要至少一名在线玩家。");
        }

        // 解析 JSON 获取 index
        JsonObject parsedJson = JsonParser.parseString(json).getAsJsonObject();
        if (!parsedJson.has("index")) {
            return MCPToolResponse.error("添加 Clip 失败：JSON 缺少 'index' 字段", "请参考 get_clip_schema 获取 JSON 示例。");
        }
        int index = parsedJson.get("index").getAsInt();

        // 校验 index 范围
        mchorse.bbs_mod.film.Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        int currentSize = film.camera.get().size();
        if (index < 0 || index > currentSize) {
            return MCPToolResponse.error(
                    "添加 Clip 失败：索引 " + index + " 超出范围",
                    "当前 Film '" + filmId + "' 有 " + currentSize + " 个 Clip，有效的 index 范围是 0 到 " + currentSize + " (包含末尾添加)。"
            );
        }

        // 调用 ClipManagerAPI 添加 Clip
        ClipManagerAPI.addClipFromJSON(player, filmId, json);

        String type = parsedJson.has("type") ? parsedJson.get("type").getAsString() : "unknown";

        return MCPToolResponse.success(
                "成功将索引 " + index + " 处的 Clip 设置为类型 " + type + "，归属 Film '" + filmId + "'",
                "Film 已同步到客户端，且该 Clip 已被选中。"
        );
    }
}

