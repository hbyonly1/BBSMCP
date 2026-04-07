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
        String guide = loadResource("/prompts/bbs_onboarding.md");
        if (guide != null) {
            registerPrompt(new MCPPrompt(
                "bbs-onboarding",
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
