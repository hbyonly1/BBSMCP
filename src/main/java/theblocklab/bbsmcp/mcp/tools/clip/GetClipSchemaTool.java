package theblocklab.bbsmcp.mcp.tools.clip;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import theblocklab.bbsmcp.mcp.core.MCPTool;
import theblocklab.bbsmcp.mcp.core.MCPToolResponse;

/**
 * MCP 工具：查询 Clip 的类型枚举、各类型所需字段及插值类型常量
 * 实现"渐进式披露"——AI 在需要添加 Clip 时按需调用此工具获取约束信息
 */
public class GetClipSchemaTool extends MCPTool {

  // 所有插值类型，提取自 Interpolations.java
  private static final String INTERP_ENUM = "linear | constant | step | hermite | bezier | cubic | " +
      "sine_in | sine_out | sine_inout | " +
      "circle_in | circle_out | circle_inout | " +
      "quad_in | quad_out | quad_inout | " +
      "cubic_in | cubic_out | cubic_inout | " +
      "quart_in | quart_out | quart_inout | " +
      "quint_in | quint_out | quint_inout | " +
      "exp_in | exp_out | exp_inout | " +
      "back_in | back_out | back_inout | " +
      "elastic_in | elastic_out | elastic_inout | " +
      "bounce_in | bounce_out | bounce_inout";

  public GetClipSchemaTool() {
    super("get_clip_schema",
        "Query available clip types, their fields, and interpolation constants. Call before add_clip if unsure about the schema.");
  }

  @Override
  public JsonObject getInputSchema() {
    return JsonParser
        .parseString(
            """
                {
                  "type": "object",
                  "properties": {
                    "clipType": {
                      "type": "string",
                      "description": "Optional. One of — overwrite: idle, dolly, path, keyframe; modifier: angle, translate, shake, drag, math, look, orbit, dolly_zoom, remapper, tracker; misc: audio, curve, subtitle. Omit to get all."
                    }
                  }
                }
                """)
        .getAsJsonObject();
  }

  @Override
  public MCPToolResponse execute(JsonObject arguments, MinecraftServer server) throws Exception {
    String clipType = arguments.has("clipType") ? arguments.get("clipType").getAsString().toLowerCase() : null;

    if (clipType != null) {
      return MCPToolResponse.success(getSchemaForType(clipType));
    }

    return MCPToolResponse.success(getFullSchema());
  }

  private String getSchemaForType(String type) {
    return switch (type) {
      // ── overwrite ──
      case "idle" -> SCHEMA_IDLE;
      case "dolly" -> SCHEMA_DOLLY;
      case "path" -> SCHEMA_PATH;
      case "keyframe" -> SCHEMA_KEYFRAME;
      // ── modifier ──
      case "angle" -> SCHEMA_ANGLE;
      case "translate" -> SCHEMA_TRANSLATE;
      case "shake" -> SCHEMA_SHAKE;
      case "drag" -> SCHEMA_DRAG;
      case "math" -> SCHEMA_MATH;
      case "look" -> SCHEMA_LOOK;
      case "orbit" -> SCHEMA_ORBIT;
      case "dolly_zoom" -> SCHEMA_DOLLY_ZOOM;
      case "remapper" -> SCHEMA_REMAPPER;
      case "tracker" -> SCHEMA_TRACKER;
      // ── misc ──
      case "audio" -> SCHEMA_AUDIO;
      case "curve" -> SCHEMA_CURVE;
      case "subtitle" -> SCHEMA_SUBTITLE;
      default -> "Unknown clip type: " + type +
          ". Available: idle, dolly, path, keyframe, " +
          "angle, translate, shake, drag, math, look, orbit, dolly_zoom, remapper, tracker, " +
          "audio, curve, subtitle";
    };
  }

  private String getFullSchema() {
    return "types (overwrite): idle, dolly, path, keyframe\n" +
        "types (modifier):  angle, translate, shake, drag, math, look, orbit, dolly_zoom, remapper, tracker\n" +
        "types (misc):      audio, curve, subtitle\n\n" +
        SCHEMA_COMMON + "\n\n" +
        SCHEMA_IDLE + "\n\n" +
        SCHEMA_DOLLY + "\n\n" +
        SCHEMA_PATH + "\n\n" +
        SCHEMA_KEYFRAME + "\n\n" +
        SCHEMA_ANGLE + "\n\n" +
        SCHEMA_TRANSLATE + "\n\n" +
        SCHEMA_SHAKE + "\n\n" +
        SCHEMA_DRAG + "\n\n" +
        SCHEMA_MATH + "\n\n" +
        SCHEMA_LOOK + "\n\n" +
        SCHEMA_ORBIT + "\n\n" +
        SCHEMA_DOLLY_ZOOM + "\n\n" +
        SCHEMA_REMAPPER + "\n\n" +
        SCHEMA_TRACKER + "\n\n" +
        SCHEMA_AUDIO + "\n\n" +
        SCHEMA_CURVE + "\n\n" +
        SCHEMA_SUBTITLE + "\n\n" +
        SCHEMA_INTERP;
  }

  // ── 通用字段 ─────────────────────────────────────────────────────────────
  private static final String SCHEMA_COMMON = """
      common fields (all clips):
        type(string,required), index(int,required), tick(int,default=0),
        duration(int,default=1), layer(int,default=0),
        enabled(bool,default=true), title(string,optional)""";

  // ════════════════════ OVERWRITE ════════════════════

  // idle
  private static final String SCHEMA_IDLE = """
      {"type":"idle","index":0,"tick":0,"duration":20,"layer":0,
        "position":{"point":{"x":0.0,"y":64.0,"z":0.0},"angle":{"yaw":0.0,"pitch":-10.0,"roll":0.0,"fov":70.0}}}""";

  // dolly — extends idle, adds distance/interp/yaw/pitch
  private static final String SCHEMA_DOLLY = """
      {"type":"dolly","index":1,"tick":10,"duration":40,"layer":0,
        "position":{"point":{"x":10.0,"y":65.0,"z":-5.0},"angle":{"yaw":45.0,"pitch":-5.0,"roll":0.0,"fov":70.0}},
        "distance":5.0,"interp":"linear","yaw":45.0,"pitch":-5.0}""";

  // path — points array, interpPoint/interpAngle
  private static final String SCHEMA_PATH = """
      {"type":"path","index":0,"tick":0,"duration":80,"layer":0,
        "points":[
          {"point":{"x":0.0,"y":64.0,"z":0.0},"angle":{"yaw":0.0,"pitch":-10.0,"roll":0.0,"fov":70.0}},
          {"point":{"x":10.0,"y":65.0,"z":5.0},"angle":{"yaw":30.0,"pitch":-5.0,"roll":0.0,"fov":70.0}},
          {"point":{"x":20.0,"y":64.0,"z":10.0},"angle":{"yaw":60.0,"pitch":-10.0,"roll":0.0,"fov":70.0}}
        ],"interpPoint":"hermite","interpAngle":"linear"}""";

  // keyframe — each channel: [[tick,value,interpIn,interpOut],...]
  private static final String SCHEMA_KEYFRAME = """
      {"type":"keyframe","index":0,"tick":0,"duration":60,"layer":0,
        "x":[[0,0.0,"linear","linear"],[60,10.0,"linear","linear"]],
        "y":[[0,64.0,"hermite","hermite"]],
        "z":[[0,0.0,"linear","linear"],[60,5.0,"linear","linear"]],
        "yaw":[[0,0.0,"linear","linear"]],"pitch":[[0,-10.0,"linear","linear"]],
        "roll":[[0,0.0,"linear","linear"]],"fov":[[0,70.0,"linear","linear"]],
        "distance":[],"additive":false}""";

  // ════════════════════ MODIFIER ════════════════════
  // Modifiers apply on top of overwrite clips on the same/lower layer.

  // angle — overrides specific angle components
  private static final String SCHEMA_ANGLE = """
      {"type":"angle","index":0,"tick":0,"duration":20,"layer":0,
        "active":15,
        "angle":{"yaw":0.0,"pitch":-10.0,"roll":0.0,"fov":70.0}}
      note: active bitmask — bit0=yaw,bit1=pitch,bit2=roll,bit3=fov; bit=1 overrides absolutely, bit=0 adds relatively""";

  // translate — overrides/offsets xyz position
  private static final String SCHEMA_TRANSLATE = """
      {"type":"translate","index":0,"tick":0,"duration":20,"layer":0,
        "active":7,
        "translate":{"x":0.0,"y":0.0,"z":0.0}}
      note: active bitmask — bit0=x,bit1=y,bit2=z; bit=1 overrides absolutely, bit=0 adds relatively""";

  // shake — camera shake via sin/cos oscillation
  private static final String SCHEMA_SHAKE = """
      {"type":"shake","index":0,"tick":0,"duration":40,"layer":0,
        "active":24,
        "shake":2.0,
        "shakeAmount":0.5}
      note: active bitmask — bit0=x,bit1=y,bit2=z,bit3=yaw,bit4=pitch,bit5=roll,bit6=fov; active=24(=bit3+bit4) means yaw+pitch swing mode""";

  // drag — smooth lag/drag on camera movement
  private static final String SCHEMA_DRAG = """
      {"type":"drag","index":0,"tick":0,"duration":60,"layer":0,
        "active":15,
        "deterministic":true,
        "factor":0.5,
        "rate":60}
      note: factor[0,1] — higher=faster catch-up; rate=updates/sec in deterministic mode; active=bitmask same as translate""";

  // math — apply math expression to selected components
  private static final String SCHEMA_MATH = """
      {"type":"math","index":0,"tick":0,"duration":60,"layer":0,
        "active":1,
        "expression":"sin(p/d*3.14)*5+value"}
      note: active bitmask — bit0=x,bit1=y,bit2=z,bit3=yaw,bit4=pitch,bit5=roll,bit6=fov
      expression variables: t(ticks),o(relativeTick),pt(transition),d(duration),p(progress),f(factor 0-1),v(velocity),dt(distance),value(current channel value),x,y,z,yaw,pitch,roll,fov""";

  // look — make camera look at entity or block
  private static final String SCHEMA_LOOK = """
      {"type":"look","index":0,"tick":0,"duration":40,"layer":0,
        "selector":-1,
        "offset":{"x":0.0,"y":1.6,"z":0.0},
        "relative":false,
        "atBlock":false,
        "block":{"x":0.0,"y":64.0,"z":0.0},
        "forward":false}
      note: selector=-1=no entity; atBlock=true→looks at block; forward=true→looks toward movement direction; relative=true adds yaw/pitch delta""";

  // orbit — orbit camera around an entity
  private static final String SCHEMA_ORBIT = """
      {"type":"orbit","index":0,"tick":0,"duration":60,"layer":0,
        "selector":0,
        "offset":{"x":0.0,"y":1.6,"z":0.0},
        "distance":5.0,
        "yaw":0.0,
        "pitch":-15.0,
        "copy":false,
        "absolute":false}
      note: copy=true copies entity head rotation; absolute=true treats yaw/pitch/distance as absolute camera values""";

  // dolly_zoom — Hitchcock zoom (position compensates for FOV change)
  private static final String SCHEMA_DOLLY_ZOOM = """
      {"type":"dolly_zoom","index":0,"tick":0,"duration":40,"layer":0,
        "focus":10.0}
      note: focus=distance to focal plane; camera position shifts to keep focal plane fixed as FOV changes in clips below""";

  // remapper — remap clip time via keyframe curve
  private static final String SCHEMA_REMAPPER = """
      {"type":"remapper","index":0,"tick":0,"duration":60,"layer":0,
        "channel":[[0,0.0,"linear","linear"],[60,1.0,"linear","linear"]]}
      note: channel maps normalized progress[0,1] to a factor remapping playback time of clips below""";

  // tracker — tracks entity position/angle (client-side rendering)
  private static final String SCHEMA_TRACKER = """
      {"type":"tracker","index":0,"tick":0,"duration":60,"layer":0,
        "selector":0,
        "offset":{"x":0.0,"y":0.0,"z":0.0},
        "angle":{"x":0.0,"y":0.0,"z":0.0},
        "fov":70.0,
        "group":"",
        "look_at":false,
        "relative":false,
        "active":127}""";

  // ════════════════════ MISC ════════════════════

  // audio — play a sound at a given tick
  private static final String SCHEMA_AUDIO = """
      {"type":"audio","index":0,"tick":0,"duration":1,"layer":0,
        "audio":"bbs.ambient.wind",
        "offset":0}
      note: audio=resource Link (namespace.path); offset=playback start offset in ticks""";

  // curve — keyframe-driven world/shader parameters
  private static final String SCHEMA_CURVE = """
      {"type":"curve","index":0,"tick":0,"duration":80,"layer":0,
        "channels":{"sun_rotation":[[0,0.0,"linear","linear"],[80,360.0,"linear","linear"]]}}
      note: channels=map of named KeyframeChannels; built-in: "sun_rotation"
      legacy format also accepted: {"key":"sun_rotation","channel":[[0,0.0,...],[80,360.0,...]]}""";

  // subtitle — text overlay on screen
  private static final String SCHEMA_SUBTITLE = """
      {"type":"subtitle","index":0,"tick":0,"duration":60,"layer":0,
        "title":"Hello World",
        "x":0,"y":0,
        "size":10.0,
        "anchorX":0.5,"anchorY":0.5,
        "color":-1,
        "textShadow":true,
        "windowX":0.5,"windowY":0.5,
        "background":0,
        "backgroundOffset":2.0,
        "shadow":0.0,
        "shadowOpaque":false,
        "lineHeight":12,
        "maxWidth":0}
      note: color=ARGB int(-1=white opaque); windowX/Y=screen pos(0.5=center); title=displayed text""";

  // ── 插值类型 ──────────────────────────────────────────────────────────────
  private static final String SCHEMA_INTERP = "interp values: " + INTERP_ENUM;
}
