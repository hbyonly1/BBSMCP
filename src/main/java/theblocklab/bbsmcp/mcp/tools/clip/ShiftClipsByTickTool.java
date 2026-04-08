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
  * MCP 工具：批量偏移指定范围内 Clip 的时间轴位置
  */
 public class ShiftClipsByTickTool extends MCPTool {
 
     public ShiftClipsByTickTool() {
         super("shift_clips_by_tick", "批量偏移指定范围内 Clip 的时间轴起始位置。");
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
                     "offsetTick": {"type": "number",  "description": "偏移量（正数向后，负数向前）"}
                   },
                   "required": ["filmId", "startTick", "offsetTick"]
                 }
                 """).getAsJsonObject();
     }
 
     @Override
     public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
         String filmId = requireString(arguments, "filmId");
         int startTick = requireInt(arguments, "startTick");
         float offsetTick = arguments.get("offsetTick").getAsFloat();
         int endTick = arguments.has("endTick") ? arguments.get("endTick").getAsInt() : -1;
 
         ServerPlayerEntity player = getFirstOnlinePlayer(server);
         if (player == null) {
             return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
         }
 
         try {
             // 写操作前同步客户端状态
             FilmManagerAPI.requestClientSaveFilm(player, filmId).join();
 
             ClipManagerAPI.shiftClipsByTick(player, filmId, startTick, endTick, offsetTick);
             return MCPToolResponse.success("已成功执行 Clip 时间平移操作，并同步到客户端。");
         } catch (BBSMCPException e) {
             return MCPToolResponse.error(e.getMessage(), e.getHint());
         }
     }
 }
