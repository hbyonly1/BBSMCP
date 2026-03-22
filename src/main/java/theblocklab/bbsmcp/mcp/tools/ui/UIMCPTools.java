package theblocklab.bbsmcp.mcp.tools.ui;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

public class UIMCPTools extends MCPToolProvider {
    public UIMCPTools() {
        registerTool(new OpenFilmTool());
        registerTool(new PlayFilmTool());
        registerTool(new CloseGUITool());
        registerTool(new TogglePlaybackTool());
    }
}
