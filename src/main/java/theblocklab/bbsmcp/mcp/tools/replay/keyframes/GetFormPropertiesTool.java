package theblocklab.bbsmcp.mcp.tools.replay.keyframes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.replays.ReplayManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询 Replay 的 FormProperties 动画通道（例如 pose、pose_overlay、lighting 等）。
 *
 * 说明：
 * - Replay 有两套独立的关键帧系统：
 *   1. replay.keyframes（ReplayKeyframes）—— 固定 31 个通道，如 x/y/z/yaw/item_main_hand 等，用 get_keyframes 查询。
 *   2. replay.properties（FormProperties）—— 动态 Form 属性通道，如 pose/pose_overlay/lighting，用本工具查询。
 *
 * - pose 关键帧的 value 是一个 JSON 对象，格式：
 *   {"pose":{"body":{"t":[tx,ty,tz],"s":[sx,sy,sz],"r":[rx,ry,rz],"r2":[r2x,r2y,r2z]}, "head":{...}, ...}}
 *   其中 t=translate, s=scale, r=rotate, r2=rotate2（第二次旋转顺序）
 */
public class GetFormPropertiesTool extends MCPTool {

    public GetFormPropertiesTool() {
        super("get_form_properties",
                "查询 Replay 的 Form 属性动画通道关键帧（如 pose/pose_overlay/lighting 等）。" +
                "这些通道与 get_keyframes 查询的基础通道（x/y/z/yaw 等）是两套独立数据。" +
                "pose value 格式：{\"pose\":{\"骨骼名\":{\"t\":[tx,ty,tz],\"s\":[sx,sy,sz],\"r\":[rx,ry,rz],\"r2\":[...]},...}}");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId":      {"type": "string",  "description": "目标 Film ID"},
                    "replayIndex": {"type": "integer", "description": "Replay 索引"},
                    "channels": {
                      "type": "array",
                      "items": {"type": "string"},
                      "description": "（可选）要查询的通道 ID 列表，如 ['pose', 'pose_overlay']，不传则返回全部"
                    },
                    "fromTick": {"type": "number", "description": "（可选）起始 tick（含）"},
                    "toTick":   {"type": "number", "description": "（可选）结束 tick（含）"}
                  },
                  "required": ["filmId", "replayIndex"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");
        int replayIndex = requireInt(arguments, "replayIndex");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null)
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint());

        try {
            // 强制保存，确保拿到最新数据
            FilmManagerAPI.requestClientSaveFilm(player, filmId).join();

            List<String> channels = null;
            if (arguments.has("channels")) {
                channels = new ArrayList<>();
                for (var e : arguments.getAsJsonArray("channels")) {
                    channels.add(e.getAsString());
                }
            }
            float fromTick = arguments.has("fromTick") ? arguments.get("fromTick").getAsFloat() : -1;
            float toTick   = arguments.has("toTick")   ? arguments.get("toTick").getAsFloat()   : -1;

            String result = ReplayManagerAPI.getFormProperties(filmId, replayIndex, channels, fromTick, toTick);
            return MCPToolResponse.success(result);
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
