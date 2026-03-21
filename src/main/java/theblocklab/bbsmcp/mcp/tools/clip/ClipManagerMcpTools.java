package theblocklab.bbsmcp.mcp.tools.clip;

import theblocklab.bbsmcp.mcp.core.*;

/**
 * ClipManager 工具提供者
 */
public class ClipManagerMCPTools extends MCPToolProvider {
    public ClipManagerMCPTools() {
        registerTool(new LoadClipsFromFileTool());
        registerTool(new AddClipTool());
        registerTool(new RemoveClipTool());
        registerTool(new GetClipsTool());
        registerTool(new GetClipSchemaTool());
    }

}
