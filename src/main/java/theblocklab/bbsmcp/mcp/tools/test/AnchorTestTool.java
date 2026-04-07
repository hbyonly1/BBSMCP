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
import theblocklab.bbsmcp.anchor.AnchorServerNetwork;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

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
        super("anchor_test", "全自动测试锚点系统（环境清理、创建、属性更新、数据校验、ID复用逻辑）。");
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
            notify(player, "1/7", "正在清理旧锚点...");
            AnchorManagerAPI.INSTANCE.removeAll(player);
        }, server)
                .thenCompose(v -> delay(1))
                .thenCompose(v -> {
                    notify(player, "2/7", "正在创建测试锚点 'Test1'...");
                    Vec3d lookPos = player.getEyePos().add(player.getRotationVec(1.0F).multiply(2.0));
                    BlockPos pos = new BlockPos((int)lookPos.x, (int)lookPos.y, (int)lookPos.z);
                    AnchorManagerAPI.INSTANCE.create(player, pos, "Test1", "", "");
                    return delay(1);
                })
                .thenCompose(v -> {
                    notify(player, "3/7", "正在更新锚点属性 (颜色改为红色)...");
                    AnchorManagerAPI.INSTANCE.update(player, 1, null, "Auto-tested description", "#FF0000");
                    return delay(2);
                })
                .thenCompose(v -> {
                    // Step 4: 数据校验
                    notify(player, "4/7", "正在验证数据一致性...");
                    String json = AnchorManagerAPI.INSTANCE.getAllAsJson();
                    JsonArray list = JsonParser.parseString(json).getAsJsonArray();
                    if (list.size() == 1) {
                        JsonObject a = list.get(0).getAsJsonObject();
                        if ("#FF0000".equals(a.get("color").getAsString())) {
                            notify(player, "4/6", "✓ 数据验证通过：属性正确同步。");
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
                    notify(player, "5/7", "开始 ID 复用逻辑测试...");
                    AnchorManagerAPI.INSTANCE.remove(player, 1);
                    
                    notify(player, "5/7", "已删除 ID:1。尝试创建新锚点验证 ID 是否复用...");
                    Vec3d lookPos = player.getEyePos().add(player.getRotationVec(1.0F).multiply(2.0));
                    BlockPos pos = new BlockPos((int)lookPos.x, (int)lookPos.y, (int)lookPos.z);
                    AnchorManagerAPI.INSTANCE.create(player, pos, "ReuseTest", "", "");
                    
                    if (AnchorManager.INSTANCE.has(1)) {
                        notify(player, "5/7", "✓ ID 复用验证通过：新锚点获得了 ID:1。");
                    } else {
                        throw new RuntimeException(" ID 复用验证失败：新锚点未获得预期的 ID:1");
                    }
                    return delay(1);
                })
                .thenCompose(v -> {
                    // Step 6: Camera Hints 勘察功能测试
                    notify(player, "6/7", "开始测试 Camera Hints 勘察系统...");
                    
                    // 构造两个模拟的 Hint
                    JsonObject hint1 = new JsonObject();
                    hint1.addProperty("camera_x", 10.0);
                    hint1.addProperty("camera_y", 70.0);
                    hint1.addProperty("camera_z", 10.0);
                    hint1.addProperty("yaw", 90.0f);
                    hint1.addProperty("pitch", 30.0f);
                    hint1.addProperty("note", "测试俯拍视角");
                    hint1.addProperty("screenshot", "test_scout_1.png");

                    JsonObject hint2 = new JsonObject();
                    hint2.addProperty("camera_x", 10.0);
                    hint2.addProperty("camera_y", 62.0);
                    hint2.addProperty("camera_z", 10.0);
                    hint2.addProperty("yaw", 90.0f);
                    hint2.addProperty("pitch", -20.0f);
                    hint2.addProperty("note", "测试仰拍视角");
                    hint2.addProperty("screenshot", "test_scout_2.png");

                    // 调用 API 添加
                    AnchorManagerAPI.INSTANCE.addCameraHint(1, hint1);
                    AnchorManagerAPI.INSTANCE.addCameraHint(1, hint2);
                    
                    // 校验是否添加成功（应有 2 个 hints）
                    String anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    JsonObject anchorObj = JsonParser.parseString(anchorData).getAsJsonObject();
                    JsonArray hints = anchorObj.getAsJsonArray("camera_hints");
                    
                    if (hints.size() != 2) {
                        throw new RuntimeException("Camera Hints 添加验证失败：期望 2 个，实际 " + hints.size());
                    }
                    notify(player, "6/7", "✓ Hints 添加验证通过（2个候选视角）。");

                    // 测试首选标记 (Hint ID 自动从 1 开始分配，第 2 个应为 ID:2)
                    AnchorManagerAPI.INSTANCE.setPreferredHint(1, 2);
                    anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    hints = JsonParser.parseString(anchorData).getAsJsonObject().getAsJsonArray("camera_hints");
                    
                    boolean h1Pref = hints.get(0).getAsJsonObject().get("preferred").getAsBoolean();
                    boolean h2Pref = hints.get(1).getAsJsonObject().get("preferred").getAsBoolean();
                    
                    if (!h2Pref || h1Pref) {
                        throw new RuntimeException("Camera Hints 首选标记验证失败：Hint 2 应为首选，而 Hint 1 不应是。");
                    }
                    notify(player, "6/7", "✓ 首选标记验证通过。");

                    // 测试删除与清空
                    AnchorManagerAPI.INSTANCE.removeCameraHint(1, 1);
                    anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    hints = JsonParser.parseString(anchorData).getAsJsonObject().getAsJsonArray("camera_hints");
                    if (hints.size() != 1) {
                         throw new RuntimeException("Camera Hints 删除单项验证失败。");
                    }
                    
                    AnchorManagerAPI.INSTANCE.clearCameraHints(1);
                    anchorData = AnchorManagerAPI.INSTANCE.getAnchorJson(1);
                    hints = JsonParser.parseString(anchorData).getAsJsonObject().getAsJsonArray("camera_hints");
                    if (hints.size() != 0) {
                         throw new RuntimeException("Camera Hints 清空验证失败。");
                    }
                    
                    notify(player, "6/7", "✓ Hints 增删改查全流程验证通过。");
                    return delay(1);
                })
                .thenCompose(v -> {
                    notify(player, "7/7", "正在同步最终数据...");
                    AnchorServerNetwork.sendAnchorListPacket(player); // 强制同步确保 UI 更新
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
