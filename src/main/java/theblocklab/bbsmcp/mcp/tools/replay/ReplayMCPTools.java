package theblocklab.bbsmcp.mcp.tools.replay;

import theblocklab.bbsmcp.mcp.core.MCPToolProvider;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.AddKeyframeTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.BatchAddKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.GetKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.RemoveKeyframesTool;
import theblocklab.bbsmcp.mcp.tools.replay.keyframes.ShiftKeyframesTool;

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
        registerTool(new SetReplayFormTool());

        // keyframe
        registerTool(new AddKeyframeTool());
        registerTool(new BatchAddKeyframesTool());
        registerTool(new RemoveKeyframesTool());
        registerTool(new ShiftKeyframesTool());
    }
}
