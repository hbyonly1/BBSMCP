package theblocklab.bbsmcp.mcp.tools.test;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

public class TestMCPTools extends MCPToolProvider {
    public TestMCPTools() {
        registerTool(new FilmClipTestTool());
        registerTool(new ReplayTestTool());
        registerTool(new SetupEnvTestTool());
        registerTool(new AnchorTestTool());
        registerTool(new TestBuildingTool());
    }
}
