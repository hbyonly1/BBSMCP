package theblocklab.bbsmcp.mcp.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import theblocklab.bbsmcp.utils.JsonFormatUtils;

/**
 * 封装工具执行的结果。
 * 返回统一的 status、message 和可选的数据，帮助 AI （大语言模型）更好地理解命令是否成功，
 * 并在失败时能够触发其自我修复机制。
 */
public class MCPToolResponse {
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private final String status;
    private final String message;
    private final String details; // 提供给 AI 的行动建议或详细说明

    private MCPToolResponse(String status, String message, String details) {
        this.status = status;
        this.message = message;
        this.details = details;
    }

    /**
     * 表示工具执行成功
     */
    public static MCPToolResponse success(String message) {
        return new MCPToolResponse("success", message, null);
    }

    /**
     * 表示工具执行成功，并携带一些额外的说明
     */
    public static MCPToolResponse success(String message, String details) {
        return new MCPToolResponse("success", message, details);
    }

    /**
     * 表示工具执行失败
     */
    public static MCPToolResponse error(String message, String details) {
        return new MCPToolResponse("error", message, details);
    }

    /**
     * 转换为 JSON 对象
     */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("status", this.status);
        obj.addProperty("message", JsonFormatUtils.pretty(this.message));
        if (this.details != null) {
            obj.addProperty("details", JsonFormatUtils.pretty(this.details));
        }
        return obj;
    }

    /**
     * 转换为 JSON 格式的字符串返回给 AI
     */
    public String toJsonString() {
        return PRETTY_GSON.toJson(toJson());
    }

    public boolean isError() {
        return "error".equals(this.status);
    }
    
    public String getMessage() {
        return this.message;
    }
}
