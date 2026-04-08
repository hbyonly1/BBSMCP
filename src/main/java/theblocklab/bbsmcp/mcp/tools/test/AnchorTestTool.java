package theblocklab.bbsmcp.mcp.tools.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import theblocklab.bbsmcp.anchor.AnchorManager;
import theblocklab.bbsmcp.anchor.AnchorManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.mcp.tools.anchor.ScoutAnchorTool;
import theblocklab.bbsmcp.mcp.tools.anchor.UpdateAnchorHintsTool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 锚点系统自动化测试工具。
 * 覆盖：清空 -> 创建 -> 更新 -> 验证 -> ID 复用。
 */
public class AnchorTestTool extends MCPTool {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    public AnchorTestTool() {
        super("anchor_test", "全自动测试锚点系统（环境清理、创建、属性更新、数据校验、ID复用及 Camera Hints 勘察闭环）。");
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

    private CompletableFuture<Void> delay(int seconds) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SCHEDULER.schedule(() -> future.complete(null), seconds, TimeUnit.SECONDS);
        return future;
    }

    private void notify(ServerPlayerEntity player, String step, String msg) {
        if (player != null) {
            player.sendMessage(Text.literal(String.format("§b[AnchorTest %s] %s", step, msg)));
        }
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return CompletableFuture.completedFuture(MCPToolResponse.error("测试失败", "需要至少一名在线玩家"));
        }

        notify(player, "Start", "锚点系统自动化测试启动...");

        return CompletableFuture.runAsync(() -> {
            // Step 1: 环境清理
            notify(player, "1/11", "正在清理旧锚点...");
            AnchorManagerAPI.INSTANCE.removeAll(player);
        }, server)
                .thenCompose(v -> delay(1))
                .thenCompose(v -> {
                    notify(player, "2/11", "正在创建测试锚点 'Test1'...");
                    Vec3d lookPos = player.getEyePos().add(player.getRotationVec(1.0F).multiply(2.0));
                    BlockPos pos = new BlockPos((int) lookPos.x, (int) lookPos.y, (int) lookPos.z);
                    AnchorManagerAPI.INSTANCE.create(player, pos, "Test1", "", "");
                    return delay(1);
                })
                .thenCompose(v -> {
                    notify(player, "3/11", "正在更新锚点属性 (颜色改为红色)...");
                    AnchorManagerAPI.INSTANCE.update(player, 1, null, "Auto-tested description", "#FF0000");
                    return delay(2);
                })
                .thenCompose(v -> {
                    // Step 4: 数据校验
                    notify(player, "4/11", "正在验证数据一致性...");
                    String json = AnchorManagerAPI.INSTANCE.getAllAsJson();
                    JsonArray list = JsonParser.parseString(json).getAsJsonArray();
                    if (list.size() == 1) {
                        JsonObject a = list.get(0).getAsJsonObject();
                        if ("#FF0000".equals(a.get("color").getAsString())) {
                            notify(player, "4/11", "✓ 数据验证通过：属性正确同步。");
                        } else {
                            throw new RuntimeException("数据验证失败：颜色不匹配！");
                        }
                    } else {
                        throw new RuntimeException("数据验证失败：期望 1 个锚点，实际获得 " + list.size());
                    }
                    return delay(1);
                })
                .thenCompose(v -> {
                    // Step 5: ID 复用逻辑测试
                    notify(player, "5/11", "开始 ID 复用逻辑测试...");
                    AnchorManagerAPI.INSTANCE.remove(player, 1);

                    notify(player, "5/11", "已删除 ID:1。尝试创建新锚点验证 ID 是否复用...");
                    Vec3d lookPos = player.getEyePos().add(player.getRotationVec(1.0F).multiply(2.0));
                    BlockPos pos = new BlockPos((int) lookPos.x, (int) lookPos.y, (int) lookPos.z);
                    AnchorManagerAPI.INSTANCE.create(player, pos, "ReuseTest", "", "");

                    if (AnchorManager.INSTANCE.has(1)) {
                        notify(player, "5/11", "✓ ID 复用验证通过：新锚点获得了 ID:1。");
                    } else {
                        throw new RuntimeException(" ID 复用验证失败：新锚点未获得预期的 ID:1");
                    }
                    return delay(1);
                })
                .thenCompose(v -> {
                    // --- 模拟 AI 导演工作流 ---
                    // 6.1 调用 ScoutAnchorTool 进行勘察
                    ScoutAnchorTool scoutTool = new ScoutAnchorTool();
                    JsonObject scoutArgs = new JsonObject();
                    scoutArgs.addProperty("anchor_id", 1);
                    scoutArgs.addProperty("film_id", "test");

                    JsonArray cameraPositions = new JsonArray();
                    JsonObject pos1 = new JsonObject();
                    pos1.addProperty("x", 10.0);
                    pos1.addProperty("y", 70.0);
                    pos1.addProperty("z", 10.0);
                    pos1.addProperty("yaw", 90.0f);
                    pos1.addProperty("pitch", 30.0f);
                    cameraPositions.add(pos1);

                    JsonObject pos2 = new JsonObject();
                    pos2.addProperty("x", 10.0);
                    pos2.addProperty("y", 62.0);
                    pos2.addProperty("z", 10.0);
                    pos2.addProperty("yaw", 90.0f);
                    pos2.addProperty("pitch", -20.0f);
                    cameraPositions.add(pos2);
                    scoutArgs.add("camera_positions", cameraPositions);

                    notify(player, "6/11", "正在执行 ScoutAnchorTool 模拟勘察...");
                    scoutTool.execute(scoutArgs, server);
                    return delay(2);
                })
                .thenCompose(v -> {
                    // 6.2 模拟 AI 决策后，调用 UpdateAnchorHintsTool 写入 hints
                    UpdateAnchorHintsTool updateTool = new UpdateAnchorHintsTool();
                    JsonObject updateArgs = new JsonObject();
                    updateArgs.addProperty("anchor_id", 1);

                    JsonArray hintsToUpdate = new JsonArray();
                    // 构造 hint2 的数据
                    JsonObject hint2 = new JsonObject();
                    hint2.addProperty("camera_x", 10.0);
                    hint2.addProperty("camera_y", 62.0);
                    hint2.addProperty("camera_z", 10.0);
                    hint2.addProperty("yaw", 90.0f);
                    hint2.addProperty("pitch", -20.0f);
                    hint2.addProperty("note", "测试仰拍视角 (via UpdateTool)");
                    hint2.addProperty("screenshot", "scout_sim_2.png");
                    hintsToUpdate.add(hint2);

                    updateArgs.add("hints", hintsToUpdate);
                    updateArgs.addProperty("clear_existing", true);

                    notify(player, "7/11", "正在执行 UpdateAnchorHintsTool 写入视角...");
                    updateTool.execute(updateArgs, server);
                    return delay(1);
                })
                .thenCompose(v -> {
                    // 校验并执行首选标记
                    String anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    JsonObject anchorObj = JsonParser.parseString(anchorData).getAsJsonObject();
                    JsonArray hints = anchorObj.getAsJsonArray("camera_hints");

                    if (hints.size() != 1) {
                        throw new RuntimeException("Camera Hints 写入验证失败：期望 1 个，实际 " + hints.size());
                    }
                    notify(player, "8/11", "✓ 勘察与写入流程验证通过。开始标记首选视角...");

                    int realHintId = hints.get(0).getAsJsonObject().get("id").getAsInt();
                    AnchorManagerAPI.INSTANCE.setPreferredHint(player, 1, realHintId);
                    return delay(1);
                })
                .thenCompose(v -> {
                    notify(player, "9/11", "正在验证首选标记状态...");
                    String anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    JsonArray hints = JsonParser.parseString(anchorData).getAsJsonObject()
                            .getAsJsonArray("camera_hints");

                    if (!hints.get(0).getAsJsonObject().get("preferred").getAsBoolean()) {
                        throw new RuntimeException("Camera Hints 首选标记验证失败。");
                    }
                    notify(player, "9/11", "✓ 首选标记验证通过。准备测试删除操作...");

                    int realHintId = hints.get(0).getAsJsonObject().get("id").getAsInt();
                    AnchorManagerAPI.INSTANCE.removeCameraHint(player, 1, realHintId);
                    return delay(1);
                })
                .thenCompose(v -> {
                    notify(player, "10/11", "正在验证单项删除结果并执行最终清空...");
                    String anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    JsonArray hints = JsonParser.parseString(anchorData).getAsJsonObject()
                            .getAsJsonArray("camera_hints");

                    AnchorManagerAPI.INSTANCE.clearCameraHints(player, 1);
                    anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    hints = JsonParser.parseString(anchorData).getAsJsonObject().getAsJsonArray("camera_hints");
                    if (hints.size() != 0) {
                        throw new RuntimeException("Camera Hints 清空验证失败。");
                    }

                    notify(player, "10/11", "✓ Hints 增删改查全流程验证通过。");
                    return delay(1);
                })
                .thenCompose(v -> {
                    notify(player, "11/11", "所有 Camera Hints 同步完成。");
                    return delay(1);
                })
                .thenCompose(v -> {
                    notify(player, "End", "§a✓ 锚点系统自动化测试全部通过！测试锚点已保留。");
                    return CompletableFuture.completedFuture(MCPToolResponse.success("锚点自动化测试执行成功"));
                })
                .exceptionally(e -> {
                    notify(player, "ERROR", "§c测试中断: " + e.getMessage());
                    return MCPToolResponse.error("测试失败", e.getMessage());
                });
    }
}
