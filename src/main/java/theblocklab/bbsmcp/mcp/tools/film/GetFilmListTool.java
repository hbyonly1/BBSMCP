package theblocklab.bbsmcp.mcp.tools.film;

import theblocklab.bbsmcp.mcp.tools.core.*;

import java.util.Collection;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;


public class GetFilmListTool extends MCPTool {

    public GetFilmListTool() {
        super("get_film_list", "获取所有 Film 的列表");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                  }
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        Collection<String> films = theblocklab.bbsmcp.film.FilmManagerAPI.INSTANCE.getFilmsList();
        if (films == null || films.isEmpty()) {
            return MCPToolResponse.success("当前暂无 Film 数据。");
        }
        String listString = String.join(", ", films);
        return MCPToolResponse.success("包含以下 Films: [" + listString + "]", "您可以根据以上 ID 进行相关操作。");
    }
}