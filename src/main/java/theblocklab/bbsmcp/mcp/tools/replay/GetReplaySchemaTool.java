package theblocklab.bbsmcp.mcp.tools.replay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * 为 AI 提供 Replay 系统中所有可用关键帧通道的元数据描述。
 * 调用任何关键帧相关工具前，应首先调用此工具了解通道结构。
 */
public class GetReplaySchemaTool extends MCPTool {

    private static final String SCHEMA_JSON = """
            {
              "description": "ReplayKeyframes 所有可用通道。value 格式: 'number' 表示浮点数字符串, 'item_json' 表示物品 JSON 字符串",
              "channels": {
                "position": {
                  "x":        {"id": "x",        "valueFormat": "number", "description": "X 世界坐标"},
                  "y":        {"id": "y",        "valueFormat": "number", "description": "Y 世界坐标（高度）"},
                  "z":        {"id": "z",        "valueFormat": "number", "description": "Z 世界坐标"},
                  "vX":       {"id": "vX",       "valueFormat": "number", "description": "X 轴速度"},
                  "vY":       {"id": "vY",       "valueFormat": "number", "description": "Y 轴速度"},
                  "vZ":       {"id": "vZ",       "valueFormat": "number", "description": "Z 轴速度"},
                  "fall":     {"id": "fall",     "valueFormat": "number", "description": "下落距离"},
                  "grounded": {"id": "grounded", "valueFormat": "number", "description": "是否在地面（0/1）"}
                },
                "rotation": {
                  "yaw":      {"id": "yaw",      "valueFormat": "number", "description": "水平朝向（偏航角），-180~180"},
                  "pitch":    {"id": "pitch",    "valueFormat": "number", "description": "俯仰角，-90(向上)~90(向下)"},
                  "headYaw":  {"id": "headYaw",  "valueFormat": "number", "description": "头部偏航角"},
                  "bodyYaw":  {"id": "bodyYaw",  "valueFormat": "number", "description": "身体偏航角"}
                },
                "state": {
                  "sneaking":  {"id": "sneaking",  "valueFormat": "number", "description": "是否潜行（0=否, 1=是）"},
                  "sprinting": {"id": "sprinting", "valueFormat": "number", "description": "是否疾跑（0=否, 1=是）"},
                  "damage":    {"id": "damage",    "valueFormat": "number", "description": "受伤计时器"}
                },
                "equipment": {
                  "mainHand":    {"id": "item_main_hand", "valueFormat": "item_json", "description": "主手物品"},
                  "offHand":     {"id": "item_off_hand",  "valueFormat": "item_json", "description": "副手物品"},
                  "armorHead":   {"id": "item_head",      "valueFormat": "item_json", "description": "头盔"},
                  "armorChest":  {"id": "item_chest",     "valueFormat": "item_json", "description": "胸甲"},
                  "armorLegs":   {"id": "item_legs",      "valueFormat": "item_json", "description": "护腿"},
                  "armorFeet":   {"id": "item_feet",      "valueFormat": "item_json", "description": "靴子"},
                  "selectedSlot":{"id": "selected_slot",  "valueFormat": "number",    "description": "当前快捷栏格（0-8）"}
                },
                "gamepad_extended": {
                  "stickLeftX":    {"id": "stick_lx",   "valueFormat": "number"},
                  "stickLeftY":    {"id": "stick_ly",   "valueFormat": "number"},
                  "stickRightX":   {"id": "stick_rx",   "valueFormat": "number"},
                  "stickRightY":   {"id": "stick_ry",   "valueFormat": "number"},
                  "triggerLeft":   {"id": "trigger_l",  "valueFormat": "number"},
                  "triggerRight":  {"id": "trigger_r",  "valueFormat": "number"},
                  "extra1X":       {"id": "extra1_x",   "valueFormat": "number"},
                  "extra1Y":       {"id": "extra1_y",   "valueFormat": "number"},
                  "extra2X":       {"id": "extra2_x",   "valueFormat": "number"},
                  "extra2Y":       {"id": "extra2_y",   "valueFormat": "number"}
                }
              },
              "interpolations": ["LINEAR", "HERMITE", "STEP"],
              "notes": [
                "通道 ID 与 Java 字段名不完全一致，例如 mainHand 字段的通道 ID 是 item_main_hand",
                "物品通道的 value 示例: {id:'minecraft:diamond_sword',Count:1}",
                "fp 与 actor 属性通过 set_replay_prop 设置，不属于关键帧通道"
              ]
            }
            """;

    public GetReplaySchemaTool() {
        super("get_replay_schema",
                "获取 Replay 关键帧系统中所有通道的 ID、value 格式和描述。调用任何关键帧工具之前必须先查阅此 Schema。");
    }

    @Override
    public JsonObject getInputSchema() {
        return JsonParser.parseString("{\"type\":\"object\",\"properties\":{}}").getAsJsonObject();
    }

    @Override
    public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) {
        return MCPToolResponse.success(SCHEMA_JSON);
    }
}
