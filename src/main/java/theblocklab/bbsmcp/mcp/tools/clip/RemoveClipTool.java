package theblocklab.bbsmcp.mcp.tools.clip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

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
  public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
    String filmId = requireString(arguments, "filmId");
    int index = requireInt(arguments, "index");

    ServerPlayerEntity player = getFirstOnlinePlayer(server);
    if (player == null) {
      return MCPToolResponse.error(
          BBSMCPError.PLAYER_NOT_ONLINE.format(),
          BBSMCPError.PLAYER_NOT_ONLINE.getHint());
    }

    try {
      // 前置操作：在执行写操作前，强制将客户端 UI 层面可能的未保存调整抢占落盘
      ClipManagerAPI.requestSaveFilmAsync(player, filmId).join();

      // 注解：ClipManagerAPI.removeClip 内部会自动调用 FilmManagerAPI.getInstance().getFilm(filmId)
      // 若电影不存在，它会直接抛出包含明确错误信息的 BBSMCPException
      // 所以在这里外层不需要，也不应该手动写 hasFilm(filmId) 存在性校验代码
      ClipManagerAPI.removeClip(player, filmId, index);
      return MCPToolResponse.success(
          "成功删除 Film '" + filmId + "' 中索引 " + index + " 的 Clip",
          "Film 已保存并同步到客户端");
    } catch (BBSMCPException e) {
      return MCPToolResponse.error(e.getMessage(), e.getHint());
    }
  }
}
