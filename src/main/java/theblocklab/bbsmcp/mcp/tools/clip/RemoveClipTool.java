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
 * MCP 工具：从指定 Film 中删除一个 Clip，并同步到客户端
 */
public class RemoveClipTool extends MCPTool {

    public RemoveClipTool() {
        super("remove_clip", "从指定 Film 中删除指定索引的 Clip，并同步到客户端");
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
                    "index": {
                      "type": "integer",
                      "description": "要删除的 Clip 在 Camera 轨道中的索引"
                    }
                  },
                  "required": ["filmId", "index"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) throws Exception {
        String filmId = arguments.get("filmId").getAsString();
        int index = arguments.get("index").getAsInt();

        // 校验 Film 存在
        if (!FilmManagerAPI.INSTANCE.getFilmsList().contains(filmId)) {
            return MCPToolResponse.error(
                    "删除失败：Film '" + filmId + "' 不存在",
                    "请用 get_film_list 确认 ID。"
            );
        }

        // 找一个在线玩家作为同步目标
        ServerPlayerEntity player = server.getPlayerManager().getPlayerList().isEmpty()
                ? null
                : server.getPlayerManager().getPlayerList().get(0);

        if (player == null) {
            return MCPToolResponse.error("删除 Clip 失败：服务器内没有在线玩家", "同步 Film 需要至少一名在线玩家。");
        }

        // 执行删除
        ClipManagerAPI.removeClip(player, filmId, index);

        // // 保存到文件
        // mchorse.bbs_mod.data.types.MapType filmData =
        //         (mchorse.bbs_mod.data.types.MapType) FilmManagerAPI.INSTANCE.getFilm(filmId).toData();
        // FilmManagerAPI.INSTANCE.saveFilm(filmId, filmData);

        return MCPToolResponse.success(
                "成功删除 Film '" + filmId + "' 中索引 " + index + " 的 Clip",
                "Film 已保存并同步到客户端"
        );
    }
}
