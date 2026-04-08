package theblocklab.bbsmcp.mcp.tools.replay.keyframes;
 
 import com.google.gson.JsonArray;
 import com.google.gson.JsonObject;
 import com.google.gson.JsonParser;
 import net.minecraft.server.MinecraftServer;
 import net.minecraft.server.network.ServerPlayerEntity;
 import theblocklab.bbsmcp.exception.BBSMCPError;
 import theblocklab.bbsmcp.exception.BBSMCPException;
 import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
 import theblocklab.bbsmcp.film.replays.ReplayManagerAPI;
 import theblocklab.bbsmcp.mcp.core.MCPTool;
 import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
 
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * 关键帧批量平移工具
  */
 public class ShiftKeyframesTool extends MCPTool {
 
     public ShiftKeyframesTool() {
         super("shift_keyframes", "批量偏移 Replay 中指定范围的关键帧时间（Tick）。支持插入空间或微调局部动作。");
     }
 
     @Override
     public JsonObject getInputSchema() {
         return JsonParser.parseString("""
                 {
                   "type": "object",
                   "properties": {
                     "filmId":      {"type": "string",  "description": "目标 Film ID"},
                     "replayIndex": {"type": "integer", "description": "Replay 索引"},
                     "fromTick":    {"type": "number",  "description": "平移起始时间（含）"},
                     "endTick":     {"type": "number",  "description": "可选：平移结束时间（含），-1 表示不限上限"},
                     "offset":      {"type": "number",  "description": "偏移量（正数向后，负数向前）"},
                     "channels": {
                       "type": "array",
                       "items": {"type": "string"},
                       "description": "可选：要操作的通道 ID 列表（如 [\\"x\\", \\"y\\"]），不传则操作该 Replay 下的所有通道"
                     }
                   },
                   "required": ["filmId", "replayIndex", "fromTick", "offset"]
                 }
                 """).getAsJsonObject();
     }
 
     @Override
     public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
         String filmId = requireString(arguments, "filmId");
         int replayIndex = requireInt(arguments, "replayIndex");
         float fromTick = arguments.get("fromTick").getAsFloat();
         float endTick = arguments.has("endTick") ? arguments.get("endTick").getAsFloat() : -1f;
         float offset = arguments.get("offset").getAsFloat();
 
         List<String> channels = null;
         if (arguments.has("channels") && arguments.get("channels").isJsonArray()) {
             channels = new ArrayList<>();
             JsonArray array = arguments.getAsJsonArray("channels");
             for (int i = 0; i < array.size(); i++) {
                 channels.add(array.get(i).getAsString());
             }
         }
 
         ServerPlayerEntity player = getFirstOnlinePlayer(server);
         if (player == null) return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(), BBSMCPError.PLAYER_NOT_ONLINE.getHint());
 
         try {
             // 前置同步：操作前确保最新内存状态
             FilmManagerAPI.requestClientSaveFilm(player, filmId).join();
 
             ReplayManagerAPI.shiftKeyframes(player, filmId, replayIndex, channels, fromTick, endTick, offset);
             
             return MCPToolResponse.success("成功偏移了 Replay[" + replayIndex + "] 的关键帧。");
         } catch (BBSMCPException e) {
             return MCPToolResponse.error(e.getMessage(), e.getHint());
         }
     }
 }
