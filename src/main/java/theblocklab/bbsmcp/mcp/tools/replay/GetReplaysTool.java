package theblocklab.bbsmcp.mcp.tools.replay;

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

/**
 * MCP 工具：查询 Film 中的 Replay 列表
 * 前置强制客户端落盘，防止读取脏数据
 */
public class GetReplaysTool extends MCPTool {

    public GetReplaysTool() {
        super("get_replays", "查询 Film 中的 Replay（演员轨道）列表。可按 index 查询单个，可选 includeKeyframes 加载关键帧统计。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "filmId": {
                      "type": "string",
                      "description": "目标 Film ID"
                    },
                    "index": {
                      "type": "integer",
                      "description": "（可选）Replay 索引，不传则返回全部"
                    }
                  },
                  "required": ["filmId"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        String filmId = requireString(arguments, "filmId");

        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return MCPToolResponse.error(BBSMCPError.PLAYER_NOT_ONLINE.format(),
                    BBSMCPError.PLAYER_NOT_ONLINE.getHint());
        }

        try {
            // 前置强制同步：确保读取到客户端最新数据
            FilmManagerAPI.requestClientSaveFilm(player, filmId).join();

            if (arguments.has("index")) {
                int index = arguments.get("index").getAsInt();
                return MCPToolResponse.success(ReplayManagerAPI.getReplayByIndex(filmId, index));
            }
            String result = ReplayManagerAPI.getReplays(filmId);
            if (result.equals("[]")) {
                return MCPToolResponse.success("Film '" + filmId + "' 中没有任何 Replay。");
            }
            return MCPToolResponse.success(result);
        } catch (BBSMCPException e) {
            return MCPToolResponse.error(e.getMessage(), e.getHint());
        }
    }
}
