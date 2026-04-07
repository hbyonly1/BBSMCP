package theblocklab.bbsmcp.mcp.tools.anchor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.anchor.AnchorManagerAPI;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * AI 将勘察结果写回 Anchor 的 camera_hints 字段。
 * 可一次性写入多个 hints，并可选择性丰富锚点的描述。
 */
public class UpdateAnchorHintsTool extends MCPTool {

    public UpdateAnchorHintsTool() {
        super("update_anchor_hints",
                "将 scout_anchor 勘察后选出的摄像机视角写入指定 Anchor 的 camera_hints 列表。支持追加或清空重写，并可选择丰富锚点描述。");
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
                    "hints": {
                      "type": "array",
                      "description": "要写入的摄像机视角列表，每个 hint 包含 camera_x/y/z、yaw、pitch、screenshot（路径）、note（导演注记）",
                      "items": {
                        "type": "object",
                        "properties": {
                          "camera_x": { "type": "number" },
                          "camera_y": { "type": "number" },
                          "camera_z": { "type": "number" },
                          "yaw": { "type": "number" },
                          "pitch": { "type": "number" },
                          "screenshot": { "type": "string", "description": "截图文件绝对路径" },
                          "note": { "type": "string", "description": "AI 导演的视角分析与推荐理由" }
                        },
                        "required": ["camera_x", "camera_y", "camera_z", "yaw", "pitch"]
                      }
                    },
                    "clear_existing": {
                      "type": "boolean",
                      "description": "是否在写入前清空旧的 hints（默认 false：追加）"
                    },
                    "enrich_description": {
                      "type": "string",
                      "description": "可选。AI 基于截图视觉理解后，对锚点描述的补充内容（将追加到当前描述末尾）"
                    }
                  },
                  "required": ["anchor_id", "hints"]
                }
                """).getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        int anchorId = requireInt(arguments, "anchor_id");
        JsonArray hints = arguments.getAsJsonArray("hints");
        boolean clearExisting = arguments.has("clear_existing") && arguments.get("clear_existing").getAsBoolean();

        if (hints == null || hints.size() == 0) {
            return MCPToolResponse.error("hints 不能为空", "请至少提供一个 camera hint 对象。");
        }

        // 可选：清空旧 hints
        if (clearExisting) {
            AnchorManagerAPI.INSTANCE.clearCameraHints(anchorId);
        }

        // 逐个写入 hints
        int added = 0;
        for (JsonElement elem : hints) {
            if (!elem.isJsonObject()) continue;
            JsonObject hint = elem.getAsJsonObject();
            if (AnchorManagerAPI.INSTANCE.addCameraHint(anchorId, hint)) {
                added++;
            }
        }

        // 可选：丰富描述
        if (arguments.has("enrich_description")) {
            String enrichment = arguments.get("enrich_description").getAsString();
            String anchorJson = AnchorManagerAPI.INSTANCE.getAnchorJson(anchorId);
            if (anchorJson != null) {
                JsonObject anchorObj = JsonParser.parseString(anchorJson).getAsJsonObject();
                String currentDesc = anchorObj.has("description") ? anchorObj.get("description").getAsString() : "";
                String newDesc = currentDesc.isEmpty() ? enrichment : currentDesc + " | " + enrichment;
                AnchorManagerAPI.INSTANCE.update(null, anchorId, null, newDesc, null);
            }
        }

        return MCPToolResponse.success(
                "已成功向 Anchor #" + anchorId + " 写入 " + added + " 个 camera_hints。",
                "下一步：可通过 get_anchors 查看完整结果，或调用 set_preferred_hint 标记首选视角。"
        );
    }
}
