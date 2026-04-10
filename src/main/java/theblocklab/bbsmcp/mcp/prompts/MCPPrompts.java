package theblocklab.bbsmcp.mcp.prompts;

import theblocklab.bbsmcp.mcp.core.MCPPrompt;
import theblocklab.bbsmcp.mcp.core.MCPPromptProvider;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * BBS MCP 提示词提供者，负责加载内置的 Markdown 说明文件。
 */
public class MCPPrompts extends MCPPromptProvider {
    public MCPPrompts() {
        // BBS 入门认知指南（项目背景、核心概念、能力地图与工作原则）
        String guide = loadResource("/prompts/bg.md");
        if (guide != null) {
            registerPrompt(new MCPPrompt(
                "background",
                "BBS-MCP 入门认知地图：介绍项目背景、核心术语、AI 能力边界与工作原则。适合在开始任何任务前加载。",
                guide
            ));
        }

        // AI 导演闭环工作流指南
        String directorGuide = loadResource("/prompts/ai_director_workflow.md");
        if (directorGuide != null) {
            registerPrompt(new MCPPrompt(
                "ai-director-workflow",
                "AI 导演闭环系统指南：将自然语言转化为 MC 动画场景的完整工作流，包含分镜规划、逐镜执行与视觉验证流程。",
                directorGuide
            ));
        }

        // AI 建筑布景工作流指南
        String buildingGuide = loadResource("/prompts/ai_building_workflow.md");
        if (buildingGuide != null) {
            registerPrompt(new MCPPrompt(
                "ai-building-workflow",
                "AI 建筑布景系统指南：从导演视角规划和生成建筑蓝图，与用户确认并通过 load_building 放置，为影片提供场景布景。",
                buildingGuide
            ));
        }
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            try (Scanner s = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A")) {
                return s.hasNext() ? s.next() : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
