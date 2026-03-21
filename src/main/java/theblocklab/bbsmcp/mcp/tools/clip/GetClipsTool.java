package theblocklab.bbsmcp.mcp.tools.clip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * MCP 工具：查询 Film 中的 Clip 信息
 * 支持四种模式（通过可选参数区分）：
 * - 不传 index/tick/layer → 返回所有 Clip 列表
 * - 只传 index → 返回指定索引的 Clip
 * - 只传 tick → 返回覆盖该 tick 的所有 Clip
 * - 同时传 tick + layer → 返回该 tick、该 layer 上的单个 Clip
 */
public class GetClipsTool extends MCPTool {

  public GetClipsTool() {
    super("get_clips", "查询 Film 中的 Clip 信息；通过可选参数 index/tick/layer 选择查询模式");
  }

  @Override
  public JsonObject getInputSchema() {
    return JsonParser.parseString("""
        {
          "type": "object",
          "properties": {
            "filmId": {
              "type": "string",
              "description": "目标 Film 的 ID，只提供该项将返回所有 Clip"
            },
            "index": {
              "type": "integer",
              "description": "（可选）Clip 索引；提供后返回该索引的单个 Clip"
            },
            "tick": {
              "type": "integer",
              "description": "（可选）时间刻；提供后返回该 tick 的所有 Clip"
            },
            "layer": {
              "type": "integer",
              "description": "（可选）轨道层；需与 tick 同时提供，返回该 tick + layer 上的单个 Clip"
            }
          },
          "required": ["filmId"]
        }
        """).getAsJsonObject();
  }

  @Override
  public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
    String filmId = requireString(arguments, "filmId");

    boolean hasIndex = arguments.has("index");
    boolean hasTick = arguments.has("tick");
    boolean hasLayer = arguments.has("layer");

    try {
      // 模式 1：按索引查询单个 Clip
      if (hasIndex) {
        int index = arguments.get("index").getAsInt();
        String result = ClipManagerAPI.getClipsByIndex(filmId, index);
        return MCPToolResponse.success(result);
      }

      // 模式 2：按 tick + layer 查询单个 Clip
      if (hasTick && hasLayer) {
        int tick = arguments.get("tick").getAsInt();
        int layer = arguments.get("layer").getAsInt();
        String result = ClipManagerAPI.getClipByTickAndLayer(filmId, tick, layer);
        return MCPToolResponse.success(result);
      }

      // 模式 3：按 tick 查询该时刻所有 Clip
      if (hasTick) {
        int tick = arguments.get("tick").getAsInt();
        String result = ClipManagerAPI.getClipsByTick(filmId, tick);
        if (result == null || result.isEmpty() || result.equals("[]")) {
          return MCPToolResponse.success("tick=" + tick + " 处没有 Clip");
        }
        return MCPToolResponse.success(result);
      }

      // 模式 4（默认）：返回所有 Clip
      String result = ClipManagerAPI.getClips(filmId);
      if (result == null || result.isEmpty() || result.equals("[]")) {
        return MCPToolResponse.success("Film '" + filmId + "' 中没有任何 Clip。");
      }
      return MCPToolResponse.success(result);

    } catch (BBSMCPException e) {
      return MCPToolResponse.error(e.getMessage(), e.getHint());
    }
  }
}
