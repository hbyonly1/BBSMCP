package theblocklab.bbsmcp.mcp;

import io.javalin.Javalin;
import net.minecraft.server.MinecraftServer;



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
        // HTTP 端点：处理单个 JSON-RPC 2.0 消息
        app.post("/mcp", ctx -> {
            String body = ctx.body();
            // MCP Router 处理请求，并返回基于 JSON-RPC 的响应
            String responseStr = router.handleRequest(body);
            
            // notification 请求（如 initialized）会返回 null，不需要回传
            if (responseStr != null) {
                ctx.contentType("application/json");
                ctx.status(200).result(responseStr);
            } else {
                ctx.status(202); // Accepted
            }
        });
    }
}
