package theblocklab.bbsmcp.mcp.tools.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import theblocklab.bbsmcp.mcp.tools.clip.AddClipTool;
import theblocklab.bbsmcp.mcp.tools.replay.BatchAddKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.replay.SetReplayPropTool;

/**
 * 公共测试夹具构建工具。
 *
 * 提供标准测试数据的构建逻辑，供以下测试工具复用：
 *   - SetupEnvTestTool（构建并留下数据供调试）
 *   - FilmClipTestTool（构建后执行测试断言）
 *
 * 注意：此类只负责"写入数据"，不负责删除。
 * 删除是调用方自己的责任（测试工具在验证完毕后自行清理）。
 */
public class TestFixtures {

    private TestFixtures() {}

    /**
     * 向指定 Film 添加标准测试 Clip：
     *   - Clip[0] idle  @ tick 0,  duration 40, layer 0
     *   - Clip[1] dolly @ tick 40, duration 40, layer 1
     */
    public static void buildStandardClips(String filmId, MinecraftServer server) {
        AddClipTool addClipTool = new AddClipTool();

        // 基础机位
        JsonObject args0 = new JsonObject();
        args0.addProperty("filmId", filmId);
        args0.addProperty("json", """
                {
                  "type": "idle",
                  "index": 0,
                  "tick": 0,
                  "duration": 40,
                  "layer": 0,
                  "position": {
                    "point": {"x": 0.0, "y": 64.0, "z": 0.0},
                    "angle": {"yaw": 0.0, "pitch": -10.0, "roll": 0.0, "fov": 70.0}
                  }
                }
                """);
        addClipTool.execute(args0, server);

        // 推移镜头
        JsonObject args1 = new JsonObject();
        args1.addProperty("filmId", filmId);
        args1.addProperty("json", """
                {
                  "type": "dolly",
                  "index": 1,
                  "tick": 40,
                  "duration": 40,
                  "layer": 1,
                  "position": {
                    "point": {"x": 10.0, "y": 65.0, "z": -5.0},
                    "angle": {"yaw": 45.0, "pitch": -5.0, "roll": 0.0, "fov": 70.0}
                  },
                  "distance": 8.0,
                  "interp": "linear",
                  "yaw": 45.0,
                  "pitch": -5.0
                }
                """);
        addClipTool.execute(args1, server);
    }

    /**
     * 向指定 Film 新增一个标准 Replay，并写入属性和关键帧：
     *   - label = "DevFixture", actor = true
     *   - x/z/yaw 各 2 帧：tick=10(0.0) → tick=40(5.0/5.0/45.0)
     *
     * @return 新建 Replay 的列表索引
     */
    public static int buildStandardReplay(String filmId, MinecraftServer server, ServerPlayerEntity player) {
        // 1. 新增 Replay
        theblocklab.bbsmcp.mcp.tools.replay.AddReplayTool addTool =
                new theblocklab.bbsmcp.mcp.tools.replay.AddReplayTool();
        JsonObject addArgs = new JsonObject();
        addArgs.addProperty("filmId", filmId);
        var addRes = addTool.execute(addArgs, server);

        // 解析新增的 index
        int replayIndex = 0;
        try {
            String marker = "新索引为 ";
            String text = addRes.getMessage() != null ? addRes.getMessage() : "";
            int idx = text.indexOf(marker);
            if (idx >= 0) {
                replayIndex = Integer.parseInt(text.substring(idx + marker.length())
                        .replaceAll("[^0-9].*", ""));
            }
        } catch (Exception ignored) {}

        // 2. 设置属性
        SetReplayPropTool propTool = new SetReplayPropTool();
        JsonObject propArgs = new JsonObject();
        propArgs.addProperty("filmId", filmId);
        propArgs.addProperty("index", replayIndex);
        propArgs.addProperty("label", "DevFixture");
        propArgs.addProperty("actor", true);
        propTool.execute(propArgs, server);

        // 3. 批量写入关键帧：x/z/yaw 各 2 帧
        BatchAddKeyframesTool batchTool = new BatchAddKeyframesTool();
        JsonObject kfArgs = new JsonObject();
        kfArgs.addProperty("filmId", filmId);
        kfArgs.addProperty("replayIndex", replayIndex);
        kfArgs.addProperty("interpolation", "LINEAR");
        kfArgs.add("keyframes", JsonParser.parseString("""
                {
                  "x":   [{"tick": 10, "value": "0.0"}, {"tick": 40, "value": "5.0"}],
                  "z":   [{"tick": 10, "value": "0.0"}, {"tick": 40, "value": "5.0"}],
                  "yaw": [{"tick": 10, "value": "0.0"}, {"tick": 40, "value": "45.0"}]
                }
                """).getAsJsonObject());
        batchTool.execute(kfArgs, server);

        return replayIndex;
    }
}
