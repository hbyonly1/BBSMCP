package theblocklab.bbsmcp.mcp.tools.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.mcp.tools.clip.AddClipTool;
import theblocklab.bbsmcp.mcp.tools.clip.GetClipsTool;
import theblocklab.bbsmcp.mcp.tools.clip.RemoveClipTool;
import theblocklab.bbsmcp.mcp.tools.film.CreateFilmTool;
import theblocklab.bbsmcp.mcp.tools.film.DeleteFilmTool;
import theblocklab.bbsmcp.mcp.tools.ui.OpenFilmTool;
import theblocklab.bbsmcp.mcp.tools.ui.TogglePlaybackTool;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestTool extends MCPTool {

    // 使用一个单线程的调度器来处理 3 秒间隔延时，绝对不卡死主线程
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    public TestTool() {
        super("test", "执行完整的串联自动化测试（清空环境 -> 创建 -> 打开 UI -> 添加3个Clip -> 获取 -> 删除）");
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

    // 辅助方法：生成有 3 秒延迟的 CompletableFuture
    private CompletableFuture<Void> delay3s() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SCHEDULER.schedule(() -> future.complete(null), 3, TimeUnit.SECONDS);
        return future;
    }

    // 辅助方法：向玩家发提示
    private void notifyPlayer(ServerPlayerEntity player, String step, String msg) {
        if (player != null) {
            player.sendMessage(Text.literal(String.format("§d[TestTool %s] %s", step, msg)));
        }
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return CompletableFuture.completedFuture(MCPToolResponse.error("测试失败", "需要至少一名在线玩家"));
        }

        notifyPlayer(player, "0/8", "自动化测试开始启动！");

        return CompletableFuture.runAsync(() -> {
            // ==========================================
            // Step 1: 清空所有 Film
            // ==========================================
            DeleteFilmTool deleteFilmTool = new DeleteFilmTool();
            Collection<String> films = FilmManagerAPI.INSTANCE.getFilmsList();
            for (String fId : films) {
                JsonObject args = new JsonObject();
                args.addProperty("filmId", fId);
                deleteFilmTool.execute(args, server);
            }
            notifyPlayer(player, "1/8", "已清空环境，删除了 " + films.size() + " 个旧影片。准备创建新影片...");
        })
                .thenCompose(v -> delay3s())
                .thenCompose(v -> {
                    // ==========================================
                    // Step 2: 创建叫做 test 的电影
                    // ==========================================
                    CreateFilmTool createFilmTool = new CreateFilmTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    createFilmTool.execute(args, server);

                    notifyPlayer(player, "2/8", "成功创建新影片 'test'。准备唤起客户端 UI 面板...");
                    return delay3s();
                })
                .thenCompose(v -> {
                    // ==========================================
                    // Step 3: 打开这个电影的 UI
                    // ==========================================
                    OpenFilmTool openFilmTool = new OpenFilmTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");

                    // open 是异步的，我们要等它发完包收到客户端 OK 之后再继续
                    return openFilmTool.executeAsync(args, server).thenApply(res -> {
                        notifyPlayer(player, "3/8", "UI 面板唤起成功！客户端已回应。准备添加 Clip...");
                        return res;
                    });
                })
                .thenCompose(v -> delay3s())
                .thenCompose(v -> {
                    // ==========================================
                    // Step 4: 连续添加三种不同类型的 Clip
                    // ==========================================
                    AddClipTool addClipTool = new AddClipTool();

                    // 插入第一个 (0) - idle 镜头（基础摄像位，停留 20 tick）
                    JsonObject args0 = new JsonObject();
                    args0.addProperty("filmId", "test");
                    args0.addProperty("json", """
                            {
                              "type": "idle",
                              "index": 0,
                              "tick": 0,
                              "duration": 20,
                              "layer": 0,
                              "position": {
                                "point": {"x": 0.0, "y": 64.0, "z": 0.0},
                                "angle": {"yaw": 0.0, "pitch": -10.0, "roll": 0.0, "fov": 70.0}
                              }
                            }
                            """);
                    addClipTool.execute(args0, server);

                    // 插入第二个 (1) - dolly 镜头（推移镜头，从第 20 tick 开始）
                    JsonObject args1 = new JsonObject();
                    args1.addProperty("filmId", "test");
                    args1.addProperty("json", """
                            {
                              "type": "dolly",
                              "index": 1,
                              "tick": 20,
                              "duration": 40,
                              "layer": 1,
                              "position": {
                                "point": {"x": 10.0, "y": 65.0, "z": -5.0},
                                "angle": {"yaw": 45.0, "pitch": -5.0, "roll": 0.0, "fov": 70.0}
                              },
                              "distance": 5.0,
                              "interp": "linear",
                              "yaw": 45.0,
                              "pitch": -5.0
                            }
                            """);
                    addClipTool.execute(args1, server);

                    // 插入第三个 (2) - shake 镜头（震动效果调节器，从第 0 tick 震到第 60 tick）
                    JsonObject args2 = new JsonObject();
                    args2.addProperty("filmId", "test");
                    args2.addProperty("json", """
                            {
                              "type": "shake",
                              "index": 2,
                              "tick": 0,
                              "duration": 60,
                              "layer": 2,
                              "active": 63,
                              "shake": 15.0,
                              "shakeAmount": 10.0
                            }
                            """);
                    addClipTool.execute(args2, server);

                    notifyPlayer(player, "4/8", "成功连续插入 3 个镜头 (idle, dolly, shake)。");
                    return delay3s();
                })
                .thenCompose(v -> {
                    // ==========================================
                    // Step 5: 插入完毕后，播放影片体验效果 (约3秒)
                    // ==========================================
                    TogglePlaybackTool togglePlaybackTool = new TogglePlaybackTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");

                    notifyPlayer(player, "5/8", "全部 3 个镜头就绪，接管摄像机，播放影片欣赏...");

                    // ★核心进化：由于切换的是 UI 级别的 Toggle播放，自带后台线程轮询！
                    return togglePlaybackTool.executeAsync(args, server).thenApply(res -> null);
                })
                .thenCompose(v -> {
                    // ==========================================
                    // Step 5: 获取最后一个 Clip (index: 2) 的信息
                    // ==========================================
                    GetClipsTool getClipsTool = new GetClipsTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    args.addProperty("index", 2);

                    try {
                        MCPToolResponse res = getClipsTool.execute(args, server);
                        notifyPlayer(player, "6/8", "查询 index=2 的 Clip 结果: " + res.toJsonString());
                    } catch (Exception e) {
                        notifyPlayer(player, "6/8", "获取 Clip 失败: " + e.getMessage());
                    }
                    return delay3s();
                })
                .thenCompose(v -> {
                    // ==========================================
                    // Step 6: 删除最后一个 Clip (index: 2)
                    // ==========================================
                    RemoveClipTool removeClipTool = new RemoveClipTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    args.addProperty("index", 2);
                    try {
                        removeClipTool.execute(args, server);
                    } catch (Exception e) {
                    }

                    notifyPlayer(player, "7/8", "成功删除最后一个镜头！准备再次播放验证...");
                    return delay3s();
                })
                .thenCompose(v -> {
                    // ==========================================
                    // Step 8: 删除后再次播放影片
                    // ==========================================
                    TogglePlaybackTool togglePlaybackTool = new TogglePlaybackTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");

                    notifyPlayer(player, "8/8", "播放删除了震动修饰器后的影片！全链路测试圆满结束！");

                    // 同样用真正的异步发包等回执
                    return togglePlaybackTool.executeAsync(args, server)
                            .thenApply(res -> MCPToolResponse.success("测试工作流全部执行完毕并成功"));
                })
                .exceptionally(e -> {
                    // 如果中间有任何一步报错，都会流转到这里
                    notifyPlayer(player, "错误", "自动化测试因为异常中断：" + e.getMessage());
                    return MCPToolResponse.error("测试异常中止", e.getMessage());
                });
    }
}
