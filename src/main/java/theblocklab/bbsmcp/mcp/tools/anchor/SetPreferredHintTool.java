package theblocklab.bbsmcp.mcp.tools.anchor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.anchor.AnchorManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * 标记指定 Anchor 的某个 camera_hint 为首选视角。
 * 用户可手动标记他们认为最好的角度；AI 也可以在分析结果后调用此工具。
 */
public class SetPreferredHintTool extends MCPTool {

    public SetPreferredHintTool() {
        super("set_preferred_hint",
                "将指定 Anchor 下某个 camera_hint 标记为首选（preferred=true），其余自动取消。用户或 AI 均可调用此工具，用于在多个候选视角中标定最佳摄像机角度。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("""
                {
                  "type": "object",
                  "properties": {
                    "anchor_id": {
                      "type": "integer",
                      "description": "目标 Anchor ID"
                    },
                    "hint_id": {
                      "type": "integer",
                      "description": "要标记为首选的 camera_hint ID（可通过 get_anchors 的 camera_hints 列表查看每个 hint 的 id）"
                    }
                  },
                  "required": ["anchor_id", "hint_id"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        int anchorId = requireInt(arguments, "anchor_id");
        int hintId = requireInt(arguments, "hint_id");

        boolean success = AnchorManagerAPI.INSTANCE.setPreferredHint(anchorId, hintId);

        if (success) {
            return MCPToolResponse.success(
                    "已将 Anchor #" + anchorId + " 的 hint #" + hintId + " 标记为首选视角。",
                    "分镜规划时，AI 导演将优先使用此视角作为摄像机起始参考。"
            );
        } else {
            return MCPToolResponse.error(
                    "标记失败：找不到 Anchor #" + anchorId + " 或其下不存在 hint #" + hintId,
                    "请通过 get_anchors 确认 anchor_id 和 hint_id 均正确。"
            );
        }
    }
}
