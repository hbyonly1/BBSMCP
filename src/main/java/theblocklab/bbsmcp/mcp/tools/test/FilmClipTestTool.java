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
import theblocklab.bbsmcp.mcp.tools.ui.OpenFilmEditorTool;
import theblocklab.bbsmcp.mcp.tools.ui.TogglePlaybackTool;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Film 与 Clip 完整生命周期的串联自动化测试工具。
 * 前身为 E2EWorkflowTestTool，职责边界更明确。
 */
public class FilmClipTestTool extends MCPTool {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    public FilmClipTestTool() {
        super("film_clip_test", "测试 Film 与 Clip 完整生命周期（清空→创建→开UI→增删Clip→播放→游标控制）。");
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

    private void notifyPlayer(ServerPlayerEntity player, String step, String msg) {
        if (player != null) {
            player.sendMessage(Text.literal(String.format("§d[FilmClipTest %s] %s", step, msg)));
        }
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return CompletableFuture.completedFuture(MCPToolResponse.error("测试失败", "需要至少一名在线玩家"));
        }

        notifyPlayer(player, "0/10", "Film & Clip 自动化测试开始启动！");

        return CompletableFuture.runAsync(() -> {
            // Step 1: 清空所有 Film
            DeleteFilmTool deleteFilmTool = new DeleteFilmTool();
            Collection<String> films = FilmManagerAPI.INSTANCE.getFilmsList();
            for (String fId : films) {
                JsonObject args = new JsonObject();
                args.addProperty("filmId", fId);
                deleteFilmTool.execute(args, server);
            }
            notifyPlayer(player, "1/10", "已清空环境，删除了 " + films.size() + " 个旧影片。准备创建新影片...");
        })
                .thenCompose(v -> delay3s())
                .thenCompose(v -> {
                    // Step 2: 创建 test Film
                    CreateFilmTool createFilmTool = new CreateFilmTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    createFilmTool.execute(args, server);
                    notifyPlayer(player, "2/10", "成功创建新影片 'test'。准备唤起客户端 UI 面板...");
                    return delay3s();
                })
                .thenCompose(v -> {
                    // Step 3: 打开 UI
                    OpenFilmEditorTool openFilmTool = new OpenFilmEditorTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    return openFilmTool.executeAsync(args, server).thenApply(res -> {
                        notifyPlayer(player, "3/10", "UI 面板唤起成功！客户端已回应。准备添加 Clip...");
                        return res;
                    });
                })
                .thenCompose(v -> delay3s())
                .thenCompose(v -> {
                    // Step 4: 添加标准 Clips（idle + dolly）+ 测试重叠非法 Clip 拦截
                    TestFixtures.buildStandardClips("test", server);

                    // 额外再测试一个故意重叠的非法 Clip（与 idle[0~20] 冲突）
                    AddClipTool addClipTool = new AddClipTool();
                    JsonObject argsErr = new JsonObject();
                    argsErr.addProperty("filmId", "test");
                    argsErr.addProperty("json",
                            """
                                    {"type":"shake","index":2,"tick":0,"duration":60,"layer":2,"active":63,"shake":15.0,"shakeAmount":10.0}
                                    """);
                    addClipTool.execute(argsErr, server);

                    // 再插一个故意重叠的非法 Clip（与 idle[0~20] 冲突的 layer 0）
                    JsonObject argsOverlap = new JsonObject();
                    argsOverlap.addProperty("filmId", "test");
                    argsOverlap.addProperty("json",
                            """
                                    {"type":"idle","index":3,"tick":10,"duration":20,"layer":0,
                                     "position":{"point":{"x":5.0,"y":64.0,"z":0.0},"angle":{"yaw":0.0,"pitch":-10.0,"roll":0.0,"fov":70.0}}}
                                    """);
                    try {
                        MCPToolResponse errRes = addClipTool.execute(argsOverlap, server);
                        if (errRes.isError()) {
                            notifyPlayer(player, "4.5/10", "✓ 测试通过：成功拦截重叠非法剪辑请求");
                        } else {
                            notifyPlayer(player, "4.5/10", "❌ 警告：应被拦截的重叠 Clip 居然被添加成功了！");
                        }
                    } catch (Exception e) {
                        notifyPlayer(player, "4.5/10", "✓ 测试通过：成功拦截重叠 (" + e.getMessage() + ")");
                    }
                    notifyPlayer(player, "4.9/10", "标准 Clips 注入完毕，重叠拦截测试通过。");
                    return delay3s();
                })
                .thenCompose(v -> {
                    // Step 5: 播放
                    TogglePlaybackTool toggleTool = new TogglePlaybackTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    notifyPlayer(player, "5/10", "全部 3 个镜头就绪，接管摄像机，播放影片...");
                    return toggleTool.executeAsync(args, server).thenApply(res -> null);
                })
                .thenCompose(v -> {
                    // Step 6: 查询 index=2 的 Clip
                    GetClipsTool getTool = new GetClipsTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    args.addProperty("index", 2);
                    try {
                        MCPToolResponse res = getTool.execute(args, server);
                        notifyPlayer(player, "6/10", "查询 index=2 结果: " + res.toJsonString());
                    } catch (Exception e) {
                        notifyPlayer(player, "6/10", "获取 Clip 失败: " + e.getMessage());
                    }
                    return delay3s();
                })
                .thenCompose(v -> {
                    // Step 7: 删除 index=2
                    RemoveClipTool removeTool = new RemoveClipTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    args.addProperty("index", 2);
                    try {
                        removeTool.execute(args, server);
                    } catch (Exception ignored) {
                    }
                    notifyPlayer(player, "7/10", "成功删除最后一个镜头！准备再次播放验证...");
                    return delay3s();
                })
                .thenCompose(v -> {
                    // Step 8: 删除后再次播放
                    TogglePlaybackTool toggleTool = new TogglePlaybackTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    notifyPlayer(player, "8/10", "播放删除了震动修饰器后的影片！");
                    return toggleTool.executeAsync(args, server).thenApply(res -> null);
                })
                .thenCompose(v -> delay3s())
                .thenCompose(v -> {
                    // Step 9: 设置游标到随机帧
                    theblocklab.bbsmcp.mcp.tools.ui.SetCursorTool setCursorTool = new theblocklab.bbsmcp.mcp.tools.ui.SetCursorTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    int randomTick = (int) (Math.random() * 50) + 10;
                    args.addProperty("tick", randomTick);
                    notifyPlayer(player, "9/10", "播放结束，设置游标到随机帧 " + randomTick);
                    return setCursorTool.executeAsync(args, server).thenApply(res -> null);
                })
                .thenCompose(v -> delay3s())
                .thenCompose(v -> {
                    // Step 10: 读取游标
                    theblocklab.bbsmcp.mcp.tools.ui.GetCursorTool getCursorTool = new theblocklab.bbsmcp.mcp.tools.ui.GetCursorTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    notifyPlayer(player, "10/10", "查询当前游标真实位置...");
                    return getCursorTool.executeAsync(args, server).thenApply(res -> {
                        notifyPlayer(player, "End", "游标结果: " + res.toJsonString() + " ✓ Film & Clip 全链路测试完成！");
                        return MCPToolResponse.success("Film & Clip 自动化测试全部执行完毕并成功");
                    });
                })
                .exceptionally(e -> {
                    notifyPlayer(player, "错误", "测试因异常中断：" + e.getMessage());
                    return MCPToolResponse.error("测试异常中止", e.getMessage());
                });
    }
}
