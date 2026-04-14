package theblocklab.bbsmcp.mcp.tools.region;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

/**
 * 区域编辑 MCP 工具集。
 */
public class RegionMCPTools extends MCPToolProvider {
    public RegionMCPTools() {
        registerTool(new GiveRegionWandTool());
        registerTool(new GetRegionSelectionTool());
        registerTool(new EditRegionTool());
        registerTool(new UndoRegionTool());
    }
}
