package theblocklab.bbsmcp.mcp.tools.clip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * MCP 工具：向指定 Film 添加一个 Clip 镜头
 */
public class AddClipTool extends MCPTool {

  public AddClipTool() {
    super("add_clip", "向指定 Film 添加或更新一个 Clip 镜头");
  }

  @Override
  public JsonObject getInputSchema() {
    return JsonParser
        .parseString(
            """
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
                """)
        .getAsJsonObject();
  }

  @Override
  public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
    String filmId = requireString(arguments, "filmId");
    String json = requireString(arguments, "json");

    ServerPlayerEntity player = getFirstOnlinePlayer(server);
    if (player == null) {
      return MCPToolResponse.error(
          BBSMCPError.PLAYER_NOT_ONLINE.format(),
          BBSMCPError.PLAYER_NOT_ONLINE.getHint());
    }

    try {
      // 前置一致性拦截：由于添加镜头时可能会依赖正确的相邻图层或总时长，因此写操作前必须获取最新的客户端画布状态
      FilmManagerAPI.requestClientSaveFilm(player, filmId);

      // 这里的 ClipManagerAPI 底层会隐式调用 FilmManagerAPI.getInstance().getFilm(filmId)
      ClipManagerAPI.addClip(player, filmId, json);
      return MCPToolResponse.success("成功添加/更新 Clip", "Film 已同步到客户端，且该 Clip 已被选中。");
    } catch (BBSMCPException e) {
      return MCPToolResponse.error(e.getMessage(), e.getHint());
    }
  }
}
