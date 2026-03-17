package theblocklab.bbsmcp.mcp.tools.ui;

import theblocklab.bbsmcp.mcp.tools.core.MCPToolProvider;

public class UIMCPTools extends MCPToolProvider{
    public UIMCPTools() {
        registerTool(new OpenFilmTool());
    }
}
