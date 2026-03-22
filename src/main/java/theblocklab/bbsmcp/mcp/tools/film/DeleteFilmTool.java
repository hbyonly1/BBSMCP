package theblocklab.bbsmcp.mcp.tools.film;

import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.mcp.core.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.film.FilmManagerAPI;

public class DeleteFilmTool extends MCPTool {

  public DeleteFilmTool() {
    super("delete_film", "在服务器中彻底删除指定的 Film 影片文件");
  }

  @Override
  public JsonObject getInputSchema() {
    return JsonParser.parseString("""
        {
          "type": "object",
          "properties": {
            "filmId": {
              "type": "string",
              "description": "要删除的目标影片 ID"
            }
          },
          "required": ["filmId"]
        }
        """).getAsJsonObject();
  }

  @Override
  public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
    // 利用我们在架构里写好的提取参数的方法，如果缺失会自动报错
    String filmId = requireString(arguments, "filmId");

    try {
      // 底层抛出 BBSMCPException 的设计在这里发挥作用，找不到的话会被 catch 抓走
      FilmManagerAPI.INSTANCE.deleteFilm(filmId);
      return MCPToolResponse.success("成功永久删除了 Film: '" + filmId + "'");
    } catch (BBSMCPException e) {
      return MCPToolResponse.error(e.getMessage(), e.getHint());
    }
  }
}
