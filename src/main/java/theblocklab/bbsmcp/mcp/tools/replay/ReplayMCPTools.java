package theblocklab.bbsmcp.mcp.tools.replay;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;

public class ReplayMCPTools extends MCPToolProvider {
    public ReplayMCPTools() {
        // 查询类
        registerTool(new GetReplaySchemaTool());
        registerTool(new GetReplaysTool());
        registerTool(new GetKeyframesTool());

        // CRUD
        registerTool(new AddReplayTool());
        registerTool(new RemoveReplayTool());
        registerTool(new SetReplayPropTool());

        // 关键帧操作
        registerTool(new AddKeyframeTool());
        registerTool(new BatchAddKeyframesTool());
        registerTool(new RemoveKeyframesTool());
        registerTool(new ShiftKeyframesTool());
    }
}
