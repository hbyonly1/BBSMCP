package theblocklab.bbsmcp.mcp.tools.clip;

import theblocklab.bbsmcp.mcp.tools.core.*;

/**
 * ClipManager 工具提供者
 */
public class ClipManagerMcpTools extends MCPToolProvider {

    public ClipManagerMcpTools() {
        registerTool(new LoadClipsTool());
    }

}
