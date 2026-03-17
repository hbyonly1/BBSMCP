package theblocklab.bbsmcp.mcp;

import io.javalin.Javalin;
import net.minecraft.server.MinecraftServer;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 Javalin 实现的 MCP HTTP 服务器
 */
public class MCPServerImpl {

    public static final MCPServerImpl INSTANCE = new MCPServerImpl();

    private Javalin app;
    private MCPRouter router;

    private MCPServerImpl() {
    }

    /**
     * 在 Minecraft 服务器启动时初始化
     */
    public void start(MinecraftServer server, int port) {
        if (app != null) {
            return;
        }

        this.router = new MCPRouter(server);

        // 避免类加载器冲突，将 Javalin 的 context class loader 设置为当前线程
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(MCPServerImpl.class.getClassLoader());

            this.app = Javalin.create(config -> {
                // MCP 要求跨域（虽然通常用于本地通信）
                config.plugins.enableCors(cors -> {
                    cors.add(it -> it.anyHost());
                });
            }).start(port);

            System.out.println("[BBS MCP] Server started on http://localhost:" + port);

            // 路由配置
            setupRoutes();

        } catch (Exception e) {
            System.err.println("[BBS MCP] Failed to start Javalin server.");
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * 在 Minecraft 服务器停止时关闭
     */
    public void stop() {
        if (app != null) {
            System.out.println("[BBS MCP] Stopping Server...");
            app.stop();
            app = null;
        }
    }

    public MCPRouter getRouter() {
        return router;
    }

    private void setupRoutes() {
        // HTTP 端点：处理单个 JSON-RPC 2.0 消息，支持挂起等待 (Async)
        app.post("/mcp", ctx -> {
            String body = ctx.body();
            
            // 返回一个可能会挂起的 CompletableFuture
            CompletableFuture<String> futureResponse = router.handleRequestAsync(body);
            
            // 将 Future 交给 Javalin，它会自动挂起并等待结果，且不阻塞当前服务器线程
            ctx.future(() -> futureResponse.thenApply(responseStr -> {
                if (responseStr != null) {
                    ctx.contentType("application/json");
                    ctx.status(200).result(responseStr);
                } else {
                    // notification 请求（如 initialized）本身没有响应
                    ctx.status(202); // Accepted
                }
                return responseStr;
            }));
        });
    }
}
