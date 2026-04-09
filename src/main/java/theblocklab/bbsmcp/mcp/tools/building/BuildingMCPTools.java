package theblocklab.bbsmcp.mcp.tools.building;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

/**
 * AI 建筑系统 MCP 工具集。
 */
public class BuildingMCPTools extends MCPToolProvider {
    public BuildingMCPTools() {
        registerTool(new LoadBuildingTool());
        registerTool(new GiveBuildingWandTool());
    }
}
