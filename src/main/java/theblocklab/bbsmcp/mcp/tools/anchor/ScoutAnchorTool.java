package theblocklab.bbsmcp.mcp.tools.anchor;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.anchor.AnchorManager;
import theblocklab.bbsmcp.anchor.Anchor;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
import theblocklab.bbsmcp.network.ServerNetwork;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * AI 导演视角勘察工具。
 * AI 围绕指定 Anchor 的自主候选摄像机坐标，逐一创建临时 Clip 并截图，
 * 返回所有截图路径供 AI 分析后决策。
 */
public class ScoutAnchorTool extends MCPTool {

  // 勘察用的临时 Clip 使用高 Layer（20），覆盖其他轨道，tick 0-5
  private static final int SCOUT_LAYER = 20;
  private static final int SCOUT_TICK_START = 0;
  private static final int SCOUT_DURATION = 5;
  private static final int SCOUT_CAPTURE_TICK = 3;

  public ScoutAnchorTool() {
    super("scout_anchor",
        "AI 导演勘察工具：围绕指定 Anchor，在自主选定的候选摄像机坐标上临时创建 Clip 并截图，将会返回截图的绝对物理路径，不需要搜索环节！供 AI 分析并选定最佳视角。");
  }

  @Override
  public JsonObject getInputSchema() {
    return JsonParser.parseString("""
        {
          "type": "object",
          "properties": {
            "anchor_id": {
              "type": "integer",
              "description": "要勘察的 Anchor ID"
            },
            "film_id": {
              "type": "string",
              "description": "用于临时写入勘察 Clip 的 Film ID（与当前 Film 相同即可，勘察完成后会自动删除）"
            },
            "camera_positions": {
              "type": "array",
              "description": "AI 自主决定的候选摄像机坐标列表，每个元素包含 x/y/z（绝对坐标）和 yaw/pitch（朝向）",
              "items": {
                "type": "object",
                "properties": {
                  "x": { "type": "number" },
                  "y": { "type": "number" },
                  "z": { "type": "number" },
                  "yaw": { "type": "number", "description": "水平朝向" },
                  "pitch": { "type": "number", "description": "垂直角度（负值=仰拍，正值=俯拍）" }
                },
                "required": ["x", "y", "z", "yaw", "pitch"]
              }
            }
          },
          "required": ["anchor_id", "film_id", "camera_positions"]
        }
        """).getAsJsonObject();
  }

  @Override
  public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
    int anchorId = requireInt(arguments, "anchor_id");
    String filmId = requireString(arguments, "film_id");
    JsonArray positions = arguments.getAsJsonArray("camera_positions");

    ServerPlayerEntity player = getFirstOnlinePlayer(server);
    if (player == null) {
      return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
    }

    Anchor anchor = AnchorManager.INSTANCE.get(anchorId);
    if (anchor == null) {
      return MCPToolResponse.error("锚点 #" + anchorId + " 不存在", "请先通过 get_anchors 确认有效的锚点 ID。");
    }

    if (positions == null || positions.size() == 0) {
      return MCPToolResponse.error("camera_positions 不能为空", "请提供至少一个候选摄像机坐标。");
    }

    // 前置保存，确保 Film 状态最新
    try {
      ServerNetwork.requestClientOpenFilmPanelPacket(player, filmId).join();
      FilmManagerAPI.requestClientSaveFilm(player, filmId).join();
    } catch (Exception e) {
      return MCPToolResponse.error("打开并保存 Film 失败: " + e.getMessage(), "请确认 Film ID 正确。");
    }

    // 获取当前 Clip 总数量（用于后续删除临时 Clip 的索引）
    int clipCountBefore = FilmManagerAPI.INSTANCE.getFilm(filmId).camera.get().size();

    // 开始遍历候选位置，逐一创建临时 Clip -> 截图 -> 记录
    JsonArray results = new JsonArray();

    for (JsonElement elem : positions) {
      JsonObject pos = elem.getAsJsonObject();
      double cx = pos.get("x").getAsDouble();
      double cy = pos.get("y").getAsDouble();
      double cz = pos.get("z").getAsDouble();
      float yaw = pos.get("yaw").getAsFloat();
      float pitch = pos.get("pitch").getAsFloat();

      // 构造临时 Idle Clip JSON（Layer 10，tick 0，duration 3）
      String clipJson = String.format("""
          {
            "type": "idle",
            "index": %d,
            "tick": %d,
            "duration": %d,
            "layer": %d,
            "position": {
              "point": { "x": %f, "y": %f, "z": %f },
              "angle": { "yaw": %f, "pitch": %f, "roll": 0.0, "fov": 70.0 }
            }
          }
          """, clipCountBefore, SCOUT_TICK_START, SCOUT_DURATION, SCOUT_LAYER, cx, cy, cz, yaw, pitch);

      try {
        // 写入勘察 Clip
        ClipManagerAPI.addClip(player, filmId, clipJson);

        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String filename = String.format("scout_%d_%s_t%d.png", anchorId, timestamp, SCOUT_CAPTURE_TICK);

        // 快速截图（不等待自然播放，使用 start_tick=0 快速预览）
        FilmManagerAPI.captureScreenshot(player, filename, SCOUT_CAPTURE_TICK, SCOUT_TICK_START).get();
        File file = new File("config/bbsmcp/screenshot/" + filename);
        String screenshotPath = file.getAbsolutePath();

        // 删除刚写入的临时 Clip
        int currentSize = FilmManagerAPI.INSTANCE.getFilm(filmId).camera.get().size();
        ClipManagerAPI.removeClip(player, filmId, currentSize - 1);

        // 记录结果
        JsonObject result = new JsonObject();
        result.addProperty("screenshot", screenshotPath);
        result.addProperty("x", cx);
        result.addProperty("y", cy);
        result.addProperty("z", cz);
        result.addProperty("yaw", yaw);
        result.addProperty("pitch", pitch);
        results.add(result);

      } catch (Exception e) {
        // 记录失败信息但不中断循环
        JsonObject result = new JsonObject();
        result.addProperty("error", "该候选位置截图失败: " + e.getMessage());
        result.addProperty("x", cx);
        result.addProperty("y", cy);
        result.addProperty("z", cz);
        results.add(result);
      }
    }

    return MCPToolResponse.success(
        "勘察完成，共 " + positions.size() + " 个候选位置，返回截图结果：",
        results.toString());
  }
}
