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
  * MCP 工具：批量偏移指定范围内 Clip 的 3D 空间坐标
  */
 public class ShiftClipsPositionTool extends MCPTool {
 
     public ShiftClipsPositionTool() {
         super("shift_clips_position", "在 3D 空间中批量平移指定范围内 Clip 的坐标。");
     }
 
     @Override
     public JsonObject getInputSchema() {
         return JsonParser.parseString("""
                 {
                   "type": "object",
                   "properties": {
                     "filmId":      {"type": "string",  "description": "目标 Film ID"},
                     "startTick":  {"type": "integer", "description": "筛选范围起始 tick（含）"},
                     "endTick":    {"type": "integer", "description": "（可选）筛选范围结束 tick（含）。不传或传 -1 表示无限大"},
                     "dx": {"type": "number", "description": "X 轴偏移分量"},
                     "dy": {"type": "number", "description": "Y 轴偏移分量"},
                     "dz": {"type": "number", "description": "Z 轴偏移分量"}
                   },
                   "required": ["filmId", "startTick", "dx", "dy", "dz"]
                 }
                 """).getAsJsonObject();
     }
 
     @Override
     public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
         String filmId = requireString(arguments, "filmId");
         int startTick = requireInt(arguments, "startTick");
         double dx = arguments.get("dx").getAsDouble();
         double dy = arguments.get("dy").getAsDouble();
         double dz = arguments.get("dz").getAsDouble();
         int endTick = arguments.has("endTick") ? arguments.get("endTick").getAsInt() : -1;
 
         ServerPlayerEntity player = getFirstOnlinePlayer(server);
         if (player == null) {
             return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
         }
 
         try {
             // 写操作前同步客户端状态
             FilmManagerAPI.requestClientSaveFilm(player, filmId).join();
 
             ClipManagerAPI.shiftClipsPosition(player, filmId, startTick, endTick, dx, dy, dz);
             return MCPToolResponse.success("已成功执行 Clip 空间平移操作，并同步到客户端。");
         } catch (BBSMCPException e) {
             return MCPToolResponse.error(e.getMessage(), e.getHint());
         }
     }
 }
