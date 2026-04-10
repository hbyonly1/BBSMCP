package theblocklab.bbsmcp.mcp.resources;

import theblocklab.bbsmcp.mcp.core.MCPResource;
import theblocklab.bbsmcp.mcp.core.MCPResourceProvider;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * BBS MCP 资源提供者，将内置的 Markdown 指南文件注册为可读资源。
 * AI 可通过 resources/list 发现它们，并通过 resources/read 读取内容。
 */
public class MCPResources extends MCPResourceProvider {

    public MCPResources() {
        registerDoc(
            "bbsmcp://docs/background",
            "BBS-MCP 项目背景",
            "BBS-MCP 入门认知地图：介绍项目背景、核心术语、AI 能力边界与工作原则。适合在开始任何任务前加载。",
            "/prompts/bg.md"
        );

        registerDoc(
            "bbsmcp://docs/ai-director-workflow",
            "AI 导演工作流",
            "AI 导演闭环系统指南：将自然语言转化为 MC 动画场景的完整工作流，包含分镜规划、逐镜执行与视觉验证流程。",
            "/prompts/ai_director_workflow.md"
        );

        registerDoc(
            "bbsmcp://docs/ai-building-workflow",
            "AI 建筑布景工作流",
            "AI 建筑布景系统指南：从导演视角规划和生成建筑蓝图，与用户确认并通过 load_building 放置，为影片提供场景布景。",
            "/prompts/ai_building_workflow.md"
        );
    }

    private void registerDoc(String uri, String name, String description, String resourcePath) {
        String text = loadResource(resourcePath);
        if (text != null) {
            registerResource(new MCPResource(uri, name, description, "text/markdown", text));
        }
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            try (Scanner s = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                return s.hasNext() ? s.next() : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
