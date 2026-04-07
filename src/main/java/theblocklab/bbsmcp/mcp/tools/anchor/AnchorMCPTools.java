package theblocklab.bbsmcp.mcp.tools.anchor;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

public class AnchorMCPTools extends MCPToolProvider {
    public AnchorMCPTools() {
        registerTool(new GetAnchorsTool());
        registerTool(new AddAnchorTool());
        registerTool(new RemoveAnchorTool());
        registerTool(new SetAnchorPropertyTool());
        registerTool(new GiveAnchorWandTool());
        registerTool(new ScoutAnchorTool());
        registerTool(new UpdateAnchorHintsTool());
        registerTool(new SetPreferredHintTool());
    }
}
