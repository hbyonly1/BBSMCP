package theblocklab.bbsmcp.mcp.tools.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;
import theblocklab.bbsmcp.mcp.tools.film.CreateFilmTool;
import theblocklab.bbsmcp.mcp.tools.film.DeleteFilmTool;
import theblocklab.bbsmcp.mcp.tools.ui.OpenFilmEditorTool;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SetupEnvTestTool extends MCPTool {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

    public SetupEnvTestTool() {
        super("setup_test_env", "模拟环境构建测试（清空环境 -> 创建叫test的电影 -> 打开面板 -> 注入基础测试 Clips 便于调试）");
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

    private CompletableFuture<Void> delay1s() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SCHEDULER.schedule(() -> future.complete(null), 1, TimeUnit.SECONDS);
        return future;
    }

    private void notifyPlayer(ServerPlayerEntity player, String msg) {
        if (player != null) {
            player.sendMessage(Text.literal("§e[SetupEnv] " + msg));
        }
    }

    @Override
    public CompletableFuture<MCPToolResponse> executeAsync(JsonObject arguments, MinecraftServer server) {
        ServerPlayerEntity player = getFirstOnlinePlayer(server);
        if (player == null) {
            return CompletableFuture.completedFuture(MCPToolResponse.error("构造环境失败", "需要至少一名在线玩家"));
        }

        notifyPlayer(player, "开始构造调试环境...");

        return CompletableFuture.runAsync(() -> {
            // Step 1: 清空
            DeleteFilmTool deleteFilmTool = new DeleteFilmTool();
            Collection<String> films = FilmManagerAPI.INSTANCE.getFilmsList();
            for (String fId : films) {
                JsonObject args = new JsonObject();
                args.addProperty("filmId", fId);
                deleteFilmTool.execute(args, server);
            }
            notifyPlayer(player, "已肃清 " + films.size() + " 个旧电影...");
        })
                .thenCompose(v -> delay1s())
                .thenCompose(v -> {
                    // Step 2: 创建
                    CreateFilmTool createFilmTool = new CreateFilmTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");
                    createFilmTool.execute(args, server);

                    notifyPlayer(player, "影片 'test' 创建完毕，准备打开面板...");
                    return delay1s();
                })
                .thenCompose(v -> {
                    // Step 3: 打开 UI
                    OpenFilmEditorTool openFilmTool = new OpenFilmEditorTool();
                    JsonObject args = new JsonObject();
                    args.addProperty("filmId", "test");

                    return openFilmTool.executeAsync(args, server).thenApply(res -> {
                        notifyPlayer(player, "已收到客户端成功打开面板回执！开始注入 Clip...");
                        return res;
                    });
                })
                .thenCompose(v -> delay1s())
                .thenCompose(v -> {
                    // Step 4: 注入标准测试 Clips（由 TestFixtures 统一管理）
                    TestFixtures.buildStandardClips("test", server);
                    notifyPlayer(player, "成功注入基础摄像与推移镜头！正在构建 Replay...");
                    return delay1s();
                })
                .thenCompose(v -> CompletableFuture.supplyAsync(() -> {
                    // Step 5: 注入标准 Replay（留下不删除，供手动调试）
                    int replayIdx = TestFixtures.buildStandardReplay("test", server, player);
                    notifyPlayer(player,
                            "成功注入 Replay[" + replayIdx + "] \"DevFixture\"（actor, x/z/yaw 各2帧）。" +
                            "调试环境搭建完毕，请手搓体验！");
                    return MCPToolResponse.success(
                            "调试环境已完整构建：Film 'test' + 2 Clips + 1 Replay（DevFixture）");
                }))
                .exceptionally(e -> {
                    notifyPlayer(player, "构造意外故障：" + e.getMessage());
                    return MCPToolResponse.error("构造异常", e.getMessage());
                });
    }
}
