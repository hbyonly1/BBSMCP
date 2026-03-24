package theblocklab.bbsmcp.mcp.tools.test;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

public class TestMCPTools extends MCPToolProvider {
    public TestMCPTools() {
        registerTool(new E2EWorkflowTestTool());
        registerTool(new SetupEnvTestTool());
    }
}
