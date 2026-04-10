package theblocklab.bbsmcp.mcp.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 资源提供者的基类。
 * 子类调用 registerResource() 来注册可供 AI 读取的资源。
 */
public abstract class MCPResourceProvider {
    // 使用 LinkedHashMap 保持注册顺序
    protected final Map<String, MCPResource> resources = new LinkedHashMap<>();

    protected void registerResource(MCPResource resource) {
        if (resource != null) {
            resources.put(resource.getUri(), resource);
        }
    }

    /**
     * 获取资源列表定义（不含内容），用于 resources/list 响应。
     */
    public JsonArray getResourceDefinitions() {
        JsonArray array = new JsonArray();
        for (MCPResource resource : resources.values()) {
            array.add(resource.toJsonDefinition());
        }
        return array;
    }

    /**
     * 根据 URI 读取具体资源内容，用于 resources/read 响应。
     * 返回 null 表示此 Provider 不拥有该 URI 的资源。
     */
    public JsonObject readResource(String uri) {
        MCPResource resource = resources.get(uri);
        return resource != null ? resource.toJsonContent() : null;
    }
}
