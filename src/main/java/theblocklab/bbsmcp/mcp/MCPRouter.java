package theblocklab.bbsmcp.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.mcp.core.MCPPromptProvider;
import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP 路由器与工具注册中心
 */
public class MCPRouter {

    private final List<MCPToolProvider> providers = new ArrayList<>();
    private final List<MCPPromptProvider> promptProviders = new ArrayList<>();
    private final MinecraftServer server;

    public MCPRouter(MinecraftServer server) {
        this.server = server;
    }

    /**
     * 注册一个新的工具提供者
     */
    public void registerProvider(MCPToolProvider provider) {
        this.providers.add(provider);
    }
 
    /**
     * 注册一个新的提示词提供者
     */
    public void registerPromptProvider(MCPPromptProvider provider) {
        this.promptProviders.add(provider);
    }

    /**
     * 处理 MCP JSON-RPC 请求 (支持异步)
     *
     * @param requestBody 客户端发来的 JSON 字符串
     * @return 挂起响应的 CompletableFuture
     */
    public CompletableFuture<String> handleRequestAsync(String requestBody) {
        try {
            JsonObject request = JsonParser.parseString(requestBody).getAsJsonObject();

            // 简单的 JSON-RPC 2.0 校验
            if (!request.has("jsonrpc") || !request.get("jsonrpc").getAsString().equals("2.0")) {
                return CompletableFuture.completedFuture(buildErrorResponse(null, -32600, "Invalid Request"));
            }
            if (!request.has("method")) {
                return CompletableFuture.completedFuture(
                        buildErrorResponse(request.get("id"), -32600, "Invalid Request: missing method"));
            }

            String method = request.get("method").getAsString();
            Object id = null;
            if (request.has("id")) {
                id = request.get("id").getAsNumber() != null ? request.get("id").getAsNumber()
                        : request.get("id").getAsString();
            }

            if ("initialize".equals(method)) {
                return CompletableFuture.completedFuture(handleInitialize(id));
            } else if ("notifications/initialized".equals(method)) {
                return CompletableFuture.completedFuture(null);
            } else if ("tools/list".equals(method)) {
                return CompletableFuture.completedFuture(handleToolsList(id));
            } else if ("tools/call".equals(method)) {
                return handleToolsCallAsync(id, request.getAsJsonObject("params"));
            } else if ("prompts/list".equals(method)) {
                return CompletableFuture.completedFuture(handlePromptsList(id));
            } else if ("prompts/get".equals(method)) {
                return CompletableFuture.completedFuture(handlePromptsGet(id, request.getAsJsonObject("params")));
            } else {
                return CompletableFuture.completedFuture(buildErrorResponse(id, -32601, "Method not found: " + method));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture
                    .completedFuture(buildErrorResponse(null, -32700, "Parse error: " + e.getMessage()));
        }
    }

    private String handleInitialize(Object id) {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2024-11-05");

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "BBS-MCP-Server");
        serverInfo.addProperty("version", "1.0.0");
        result.add("serverInfo", serverInfo);

        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        capabilities.add("prompts", new JsonObject());
        result.add("capabilities", capabilities);

        return buildSuccessResponse(id, result);
    }

    private String handleToolsList(Object id) {
        JsonArray toolsArray = new JsonArray();
        for (MCPToolProvider provider : providers) {
            JsonArray defs = provider.getToolDefinitions();
            if (defs != null) {
                for (int i = 0; i < defs.size(); i++) {
                    toolsArray.add(defs.get(i));
                }
            }
        }

        JsonObject result = new JsonObject();
        result.add("tools", toolsArray);

        return buildSuccessResponse(id, result);
    }

    private CompletableFuture<String> handleToolsCallAsync(Object id, JsonObject params) {
        if (params == null || !params.has("name")) {
            return CompletableFuture
                    .completedFuture(buildErrorResponse(id, -32602, "Invalid params: missing tool name"));
        }

        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();

        for (MCPToolProvider provider : providers) {
            try {
                // 尝试执行，如果返回 null 说明 Provider 不认识这个工具
                CompletableFuture<theblocklab.bbsmcp.mcp.core.MCPToolResponse> future = provider
                        .executeToolAsync(toolName, arguments, server);
                if (future != null) {
                    return future.thenApply(response -> {
                        JsonArray contentArray = new JsonArray();
                        JsonObject textObj = new JsonObject();
                        textObj.addProperty("type", "text");
                        textObj.addProperty("text", response.toJsonString());
                        contentArray.add(textObj);

                        JsonObject result = new JsonObject();
                        result.add("content", contentArray);
                        if (response.isError()) {
                            result.addProperty("isError", true);
                        }
                        return buildSuccessResponse(id, result);
                    }).exceptionally(e -> {
                        e.printStackTrace();
                        JsonArray contentArray = new JsonArray();
                        JsonObject textObj = new JsonObject();
                        textObj.addProperty("type", "text");
                        textObj.addProperty("text", theblocklab.bbsmcp.mcp.core.MCPToolResponse
                                .error("执行内部异常: " + e.getMessage(), "请检查工具参数，或提醒服务器开发者查看后台报错堆栈。").toJsonString());
                        contentArray.add(textObj);

                        JsonObject result = new JsonObject();
                        result.add("content", contentArray);
                        result.addProperty("isError", true);
                        return buildSuccessResponse(id, result);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                JsonArray contentArray = new JsonArray();
                JsonObject textObj = new JsonObject();
                textObj.addProperty("type", "text");
                textObj.addProperty("text", theblocklab.bbsmcp.mcp.core.MCPToolResponse
                        .error("解析工具调用异常: " + e.getMessage(), "请检查工具参数。").toJsonString());
                contentArray.add(textObj);

                JsonObject result = new JsonObject();
                result.add("content", contentArray);
                result.addProperty("isError", true);
                return CompletableFuture.completedFuture(buildSuccessResponse(id, result));
            }
        }

        // 如果没有 Provider 处理
        return CompletableFuture
                .completedFuture(buildErrorResponse(id, -32601, "Tool not found or unsupported: " + toolName));
    }

    private String buildSuccessResponse(Object id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");

        if (id instanceof Number) {
            response.addProperty("id", (Number) id);
        } else if (id instanceof String) {
            response.addProperty("id", (String) id);
        } else {
            response.add("id", null);
        }

        response.add("result", result);
        return response.toString();
    }

    private String buildErrorResponse(Object id, int code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");

        if (id instanceof Number) {
            response.addProperty("id", (Number) id);
        } else if (id instanceof String) {
            response.addProperty("id", (String) id);
        } else {
            response.add("id", null);
        }

        JsonObject errorInfo = new JsonObject();
        errorInfo.addProperty("code", code);
        errorInfo.addProperty("message", message);
        response.add("error", errorInfo);

        return response.toString();
    }

    private String handlePromptsList(Object id) {
        JsonArray promptsArray = new JsonArray();
        for (MCPPromptProvider provider : promptProviders) {
            JsonArray defs = provider.getPromptDefinitions();
            if (defs != null) {
                for (int i = 0; i < defs.size(); i++) {
                    promptsArray.add(defs.get(i));
                }
            }
        }

        JsonObject result = new JsonObject();
        result.add("prompts", promptsArray);

        return buildSuccessResponse(id, result);
    }

    private String handlePromptsGet(Object id, JsonObject params) {
        if (params == null || !params.has("name")) {
            return buildErrorResponse(id, -32602, "Invalid params: missing prompt name");
        }

        String promptName = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();

        for (MCPPromptProvider provider : promptProviders) {
            JsonObject response = provider.getPrompt(promptName, arguments);
            if (response != null) {
                return buildSuccessResponse(id, response);
            }
        }

        return buildErrorResponse(id, -32601, "Prompt not found: " + promptName);
    }
}
