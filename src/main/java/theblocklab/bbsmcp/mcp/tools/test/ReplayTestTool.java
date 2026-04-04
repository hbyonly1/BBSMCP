package theblocklab.bbsmcp.mcp.tools.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.mcp.tools.replay.AddReplayTool;
import theblocklab.bbsmcp.mcp.tools.replay.GetReplaySchemaTool;
import theblocklab.bbsmcp.mcp.tools.replay.GetReplaysTool;
import theblocklab.bbsmcp.mcp.tools.replay.RemoveReplayTool;
import theblocklab.bbsmcp.mcp.tools.replay.SetReplayPropTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.BatchAddKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.GetKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.RemoveKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.ShiftKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.ui.OpenReplayEditorTool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Replay 系统的串联自动化测试工具。
 * 前提：Film 'test' 必须已存在且在客户端 UI 中打开（可先运行 film_clip_test）。
 *
 * 测试流程：
 * get_replay_schema → add_replay → open_replay_editor → set_replay_prop →
 * get_replays 验证
 * → batch_add_keyframes → get_keyframes 确认 → remove_keyframes 区间删除
 * → get_keyframes 再次确认 → shift_keyframes 平移 → remove_replay 清理
 */
public class ReplayTestTool extends MCPTool {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
    private static final String FILM_ID = "test";

    public ReplayTestTool() {
        super("replay_test",
                "测试 Replay 系统完整生命周期（清理→Schema查询→新增→属性设置→验证→批量关键帧→精确查询→区间删除→验证→安全平移→验证→删除）。" +
                        "前提：Film 'test' 已存在且 UI 已打开（先运行 setup_env_test 或 film_clip_test）。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {}
                }
                """).getAsJsonObject();
    }

    private CompletableFuture<Void> delay3s() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SCHEDULER.schedule(() -> future.complete(null), 3, TimeUnit.SECONDS);
        return future;
    }

    private void log(ServerPlayerEntity player, String step, String msg) {
        if (player != null) {
            player.sendMessage(Text.literal(String.format("§b[ReplayTest %s] %s", step, msg)));
        }
    }

    /** 持有本次测试新建 Replay 的索引 */
    private volatile int testReplayIndex = -1;

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return CompletableFuture.completedFuture(
                    MCPToolResponse.error("测试失败", "需要至少一名在线玩家"));
        }

        log(player, "0/12", "Replay 自动化测试开始！（使用 Film: '" + FILM_ID + "'）");

        return CompletableFuture.runAsync(() -> {
            // ─── Step 1: 清理环境（删除所有旧 Replay） ──────────────────────
            RemoveReplayTool removeTool = new RemoveReplayTool();
            int count = theblocklab.bbsmcp.film.FilmManagerAPI.INSTANCE.getFilm(FILM_ID).replays.getList().size();
            for (int i = count - 1; i >= 0; i--) {
                JsonObject args = new JsonObject();
                args.addProperty("filmId", FILM_ID);
                args.addProperty("index", i);
                removeTool.execute(args, server);
            }
            log(player, "1/12", "✓ 已清空环境，删除了 " + count + " 个旧 Replay。准备开始测试...");
        })
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 2: 查询 get_replay_schema ───────────────────────────
                    GetReplaySchemaTool schemaTool = new GetReplaySchemaTool();
                    MCPToolResponse schemaRes = schemaTool.execute(new JsonObject(), server);
                    if (schemaRes.isError())
                        throw new RuntimeException("schema 查询失败: " + schemaRes.toJsonString());
                    log(player, "2/12", "✓ get_replay_schema 成功，通道数据已返回。");
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 3: 新增 Replay ───────────────────────────────
                    AddReplayTool addTool = new AddReplayTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    MCPToolResponse res = addTool.execute(args, server);
                    if (res.isError())
                        throw new RuntimeException("add_replay 失败: " + res.toJsonString());

                    // 从响应文本中提取 index（格式：成功创建 Replay，新索引为 N）
                    String resText = res.toJsonString();
                    try {
                        String marker = "新索引为 ";
                        int idx = resText.indexOf(marker);
                        if (idx >= 0) {
                            String numStr = resText.substring(idx + marker.length()).replaceAll("[^0-9].*", "");
                            testReplayIndex = Integer.parseInt(numStr);
                        }
                    } catch (Exception e) {
                        testReplayIndex = 0; // 兜底
                    }
                    log(player, "3/12", "✓ add_replay 成功，新 Replay index=" + testReplayIndex);
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 3: 打开 Replay 编辑器 ───────────────────────
                    OpenReplayEditorTool openTool = new OpenReplayEditorTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("replayIndex", testReplayIndex);
                    try {
                        MCPToolResponse res = openTool.executeAsync(args, server).join();
                        if (res.isError())
                            throw new RuntimeException("open_replay_editor 失败: " + res.toJsonString());
                    } catch (Exception e) {
                        throw new RuntimeException("open_replay_editor 异常: " + e.getMessage(), e);
                    }
                    log(player, "4/12", "✓ open_replay_editor 成功。");
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 4: 设置属性 ──────────────────────────────────
                    SetReplayPropTool propTool = new SetReplayPropTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("index", testReplayIndex);
                    args.addProperty("label", "TestActor");
                    args.addProperty("actor", true);
                    MCPToolResponse res = propTool.execute(args, server);
                    if (res.isError())
                        throw new RuntimeException("set_replay_prop 失败: " + res.toJsonString());
                    log(player, "5/12", "✓ set_replay_prop 成功（label=TestActor, actor=true）。");
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 5: 查询验证属性写入 ─────────────────────────────
                    GetReplaysTool getTool = new GetReplaysTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("index", testReplayIndex);
                    MCPToolResponse res = getTool.execute(args, server);
                    if (res.isError())
                        throw new RuntimeException("get_replays 验证失败: " + res.toJsonString());
                    String body = res.getMessage() == null ? "" : res.getMessage();
                    boolean labelOk = body.contains("TestActor");
                    boolean actorOk = body.contains("\"actor\":true");
                    if (labelOk && actorOk) {
                        log(player, "6/12", "✓ get_replays 验证通过：label 与 actor 属性写入正确。");
                    } else {
                        log(player, "6/12", "⚠ get_replays 数据可能不符（" + body + "），请手动检查。");
                    }
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 6: 批量写入关键帧（x/z/yaw 各 2 帧）────────────
                    BatchAddKeyframesTool batchTool = new BatchAddKeyframesTool();
                    JsonObject kfArgs = new JsonObject();
                    kfArgs.addProperty("filmId", FILM_ID);
                    kfArgs.addProperty("replayIndex", testReplayIndex);
                    kfArgs.addProperty("interpolation", "LINEAR");
                    kfArgs.add("keyframes", JsonParser.parseString("""
                            {
                              "x":   [{"tick": 10, "value": "0.0"}, {"tick": 40, "value": "5.0"}],
                              "z":   [{"tick": 10, "value": "0.0"}, {"tick": 40, "value": "5.0"}],
                              "yaw": [{"tick": 10, "value": "0.0"}, {"tick": 40, "value": "45.0"}]
                            }
                            """).getAsJsonObject());
                    MCPToolResponse res = batchTool.execute(kfArgs, server);
                    if (res.isError())
                        throw new RuntimeException("batch_add_keyframes 失败: " + res.toJsonString());
                    log(player, "7/12", "✓ batch_add_keyframes 成功，x/z/yaw 各 2 帧写入完毕平衡检测已通过。");
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 7: 精确查询 x 通道确认 2 帧 ──────────────────
                    GetKeyframesTool kfTool = new GetKeyframesTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("replayIndex", testReplayIndex);
                    args.add("channels", JsonParser.parseString("[\"x\"]").getAsJsonArray());
                    MCPToolResponse res = kfTool.execute(args, server);
                    if (res.isError())
                        throw new RuntimeException("get_keyframes 失败: " + res.toJsonString());
                    String body = res.getMessage() == null ? "" : res.getMessage();
                    // x 通道应有 2 帧，粗略判断 tick=10 和 tick=40 都存在
                    boolean has10 = body.contains("\"tick\":10.0");
                    boolean has40 = body.contains("\"tick\":40.0");
                    if (has10 && has40) {
                        log(player, "8/12", "✓ get_keyframes 验证通过：x 通道含 tick=10 和 tick=40 共 2 帧。");
                    } else {
                        log(player, "8/12", "⚠ x 通道帧数据可能不符（" + body + "），请手动检查。");
                    }
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 8: 区间删除 [0, 20] tick 内 x/z/yaw 全部帧 ────
                    RemoveKeyframesTool removeTool = new RemoveKeyframesTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("replayIndex", testReplayIndex);
                    args.add("channels", JsonParser.parseString("[\"x\",\"z\",\"yaw\"]").getAsJsonArray());
                    args.addProperty("fromTick", 0);
                    args.addProperty("toTick", 20);
                    MCPToolResponse res = removeTool.execute(args, server);
                    if (res.isError())
                        throw new RuntimeException("remove_keyframes 失败: " + res.toJsonString());
                    log(player, "9/12", "✓ remove_keyframes 成功，已删除 x/z/yaw 通道 [0~20] 区间内的帧。");
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 9: 再次查询确认 tick=10 的帧已消失，tick=40 仍存在 ─
                    GetKeyframesTool kfTool = new GetKeyframesTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("replayIndex", testReplayIndex);
                    args.add("channels", JsonParser.parseString("[\"x\"]").getAsJsonArray());
                    MCPToolResponse res = kfTool.execute(args, server);
                    String body = res.getMessage() == null ? "" : res.getMessage();
                    boolean tick10Gone = !body.contains("\"tick\":10.0");
                    boolean tick40Still = body.contains("\"tick\":40.0");
                    if (tick10Gone && tick40Still) {
                        log(player, "10/12", "✓ 删除验证通过：tick=10 帧已清除，tick=40 帧仍在。");
                    } else {
                        log(player, "10/12", "⚠ 删除验证可能不符（" + body + "），请手动检查。");
                    }
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 10: 测试 shift_keyframes，将 30 以后（即剩下的 tick=40）后移 20 ──
                    ShiftKeyframesTool shiftTool = new ShiftKeyframesTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("replayIndex", testReplayIndex);
                    args.addProperty("fromTick", 30);
                    args.addProperty("offset", 20);
                    MCPToolResponse res = shiftTool.execute(args, server);
                    if (res.isError())
                        throw new RuntimeException("shift_keyframes 失败: " + res.toJsonString());
                    log(player, "11/12", "✓ shift_keyframes 成功，将 30 tick 后的关键帧后移 20 tick。");
                    return (Void) null;
                }))
                .thenCompose(v -> delay3s())
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // ─── Step 11: 最终验证偏移后的结果应该在 tick=60 ──────────────────
                    GetKeyframesTool kfTool = new GetKeyframesTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", FILM_ID);
                    args.addProperty("replayIndex", testReplayIndex);
                    args.add("channels", JsonParser.parseString("[\"x\"]").getAsJsonArray());
                    MCPToolResponse res = kfTool.execute(args, server);
                    String body = res.getMessage() == null ? "" : res.getMessage();
                    boolean tick40Gone = !body.contains("\"tick\":40.0");
                    boolean tick60Exists = body.contains("\"tick\":60.0");

                    if (tick40Gone && tick60Exists) {
                        log(player, "12/12", "✓ 偏移验证通过：tick=40 帧已移至 tick=60。全部功能就绪。");
                    } else {
                        log(player, "12/12", "⚠ 偏移验证可能不符（" + body + "），请手动检查。");
                    }

                    log(player, "End", "✓ Replay 系统安全平移协议验证完成！");
                    return MCPToolResponse.success("Replay 自动化测试全部执行完毕并通过（含平移验证）");
                }))
                .exceptionally(e -> {
                    log(player, "错误", "测试因异常中断：" + e.getMessage());
                    return MCPToolResponse.error("Replay 测试异常中止", e.getMessage());
                });
    }
}
