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
import theblocklab.bbsmcp.mcp.tools.film.CreateFilmTool;
import theblocklab.bbsmcp.mcp.tools.film.DeleteFilmTool;
import theblocklab.bbsmcp.mcp.tools.ui.OpenFilmTool;

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
            OpenFilmTool openFilmTool = new OpenFilmTool();
            JsonObject args = new JsonObject();
            args.addProperty("filmId", "test");
            
            return openFilmTool.executeAsync(args, server).thenApply(res -> {
                notifyPlayer(player, "已收到客户端成功打开面板回执！开始注入 Clip...");
                return res;
            });
        })
        .thenCompose(v -> delay1s())
        .thenCompose(v -> {
            // Step 4: 注入测试 Clips
            AddClipTool addClipTool = new AddClipTool();
            
            // 基础机位
            JsonObject args0 = new JsonObject();
            args0.addProperty("filmId", "test");
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

            // 推移
            JsonObject args1 = new JsonObject();
            args1.addProperty("filmId", "test");
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

            notifyPlayer(player, "成功注入了基础摄像与推移镜头！调试环境搭建完毕，请手搓体验！");
            
            return CompletableFuture.completedFuture(MCPToolResponse.success("环境搭建已成功完成"));
        })
        .exceptionally(e -> {
            notifyPlayer(player, "构造意外故障：" + e.getMessage());
            return MCPToolResponse.error("构造异常", e.getMessage());
        });
    }
}
