package theblocklab.bbsmcp.mcp.tools.film;

import theblocklab.bbsmcp.mcp.tools.core.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.film.FilmManagerAPI;

public class CreateFilmTool extends MCPTool {

    public CreateFilmTool() {
        super("create_film", "在服务器中创建一个全新的 Film。若不指定名称，默认为 'default'");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": { "type": "string", "description": "可选，影片 ID，如果为空则设为 'default'" }
                  }
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = (arguments.has("filmId") && !arguments.get("filmId").isJsonNull())
                ? arguments.get("filmId").getAsString()
                : FilmManagerAPI.DEFAULT_FILM_ID; 
        if (FilmManagerAPI.INSTANCE.getFilmsList().contains(filmId)) {
            return MCPToolResponse.error("Film: '" + filmId + "' 已存在，创建失败。", "请提示用户并指引用户更换新 ID");
        }
        FilmManagerAPI.INSTANCE.createFilm(filmId);
        return MCPToolResponse.success("成功创建了 Film: '" + filmId + "'");
    }
}
