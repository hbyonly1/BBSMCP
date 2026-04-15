package theblocklab.bbsmcp.exception;

/**
 * 所有业务错误码的统一定义。
 * message 为给 AI / 日志使用的错误描述模板（支持 String.format 占位符 %s / %d）。
 * hint 为给 AI 的行动建议，告诉它下一步该怎么做。
 */
public enum BBSMCPError {

    // ── Film 相关 ────────────────────────────────────────────────────────────
    FILM_NOT_FOUND("FILM_001", "Film '%s' 不存在",
            "请先用 create_film 创建，或用 get_film_list 查看已有 Film 列表"),

    // ── Clip 相关 ────────────────────────────────────────────────────────────
    CLIP_INDEX_INVALID("CLIP_001", "索引 %d 超出范围，当前 Film '%s' 共有 %d 个 Clip",
            "有效的 index 范围是 0 到当前 Clip 数量（含末尾追加位）"),
    CLIP_NOT_FOUND("CLIP_002", "索引 %d 处不存在 Clip",
            "请用 get_clips 确认当前 Clip 列表后再操作"),
    CLIP_MISSING_FIELD("CLIP_003", "JSON 缺少必要字段 '%s'",
            "请参考 get_clip_schema 获取各类型所需字段"),
    CLIP_UNKNOWN_TYPE("CLIP_004", "未知的 Clip 类型: '%s'",
            "请用 get_clip_schema 查看支持的 Clip 类型列表"),
    CLIP_INVALID_JSON("CLIP_005", "Clip JSON 格式错误，必须是 {...} 对象格式",
            "请检查 JSON 格式，确保是合法的对象"),
    CLIP_DURATION_INVALID("CLIP_006", "非法的持续时长: %d",
            "Clip 的 duration 必须大于 0"),
    CLIP_OVERLAPS("CLIP_007", "时间轴重叠冲突：图层 %d 上的待添加片段 [%d ~ %d] 与现存的第 %d 个片段 [%d ~ %d] 发生重叠覆盖！",
            "同一个图层 (layer) 的时间轴上绝对不允许存在重叠的 Clip，建议更改 tick, duration 或者移动到其他 layer 上。"),

    // ── 运行环境相关 ─────────────────────────────────────────────────────────
    PLAYER_NOT_ONLINE("ENV_001", "服务器内没有在线玩家",
            "同步 Film 需要至少一名在线玩家，请先登录游戏"),
    PLAYER_NOT_FOUND("ENV_002", "找不到玩家 '%s'",
            "请确认玩家是否在线，并检查名称拼写"),
    
    // ── 客户端交互相关 ────────────────────────────────────────────────────────
    CLIENT_EXECUTION_FAILED("CLIENT_001", "客户端执行过程中发生失败或异常: %s",
            "这可能由于游戏客户端尚未加载 UI、处于不匹配的场景或内部抛出异常导致，请检查参数或手动确认客户端状态"),

    // ── 参数校验相关 ─────────────────────────────────────────────────────────
    MISSING_PARAM("PARAM_001", "缺少必填参数 '%s'",
            "请检查工具调用参数，确保所有 required 字段都已提供"),

    // ── Replay 相关 ───────────────────────────────────────────────────────────
    REPLAY_NOT_FOUND("REPLAY_001", "索引 %d 处不存在 Replay，当前 Film '%s' 共有 %d 个 Replay",
            "请用 get_replays 确认当前 Replay 列表后再操作"),
    REPLAY_CHANNEL_NOT_FOUND("REPLAY_002", "关键帧通道 '%s' 不存在于 Replay 的 ReplayKeyframes 中",
            "请先调用 get_replay_schema 获取所有合法通道 ID，注意通道 ID 与 Java 字段名不完全一致"),
    REPLAY_FORM_NOT_SET("REPLAY_003", "Replay[%d] 尚未设置 Form，无法操作 FormProperties",
            "请先用 set_replay_form 或 set_replay_form(mobId/formJson) 为该 Replay 设置外观"),
    REPLAY_FORM_PROPERTY_NOT_FOUND("REPLAY_004", "FormProperties 通道 '%s' 不存在于 Replay[%d]",
            "请先用 add_form_keyframe / batch_add_form_keyframes 创建该通道，或用 get_form_properties 查看已有通道"),
    REPLAY_INTERPOLATION_NOT_FOUND("REPLAY_005", "未知插值类型 '%s'",
            "请使用 get_replay_schema 查看支持的 interpolation 列表，例如 linear、sine_inout、hermite、bezier"),

    // ── Anchor 相关 ──────────────────────────────────────────────────────────
    ANCHOR_NOT_FOUND("ANCHOR_001", "锚点 ID %d 不存在",
            "请用 get_anchors 查看已有锚点列表，或确认 ID 拼写"),
    ANCHOR_HINT_NOT_FOUND("ANCHOR_002", "锚点 ID %d 下不存在提示 ID %d",
            "请先通过 get_anchors 查看该锚点的 camera_hints 列表以获取正确的 hint ID"),

    // ── Building 文件相关 ───────────────────────────────────────────────────
    INVALID_BUILDING_NAME("BUILDING_001", "建筑名称 '%s' 非法",
            "请使用简洁的建筑名，避免仅包含空格或非法路径字符"),
    BUILDING_FILE_NOT_FOUND("BUILDING_002", "建筑文件 '%s' 不存在",
            "请先传入 blueprint_json 进行保存，或用 get_building_list 查看已有建筑文件"),
    BUILDING_FILE_ALREADY_EXISTS("BUILDING_003", "建筑文件 '%s' 已存在且 overwrite=false",
            "如需覆盖已有建筑文件，请显式传入 overwrite=true"),
    BUILDING_FILE_IO_ERROR("BUILDING_004", "建筑文件操作失败: %s",
            "请检查 config/bbsmcp/buildings 目录是否可访问，或确认文件未被其他进程占用");

    // ─────────────────────────────────────────────────────────────────────────

    private final String code;
    private final String messageTemplate;
    private final String hint;

    BBSMCPError(String code, String messageTemplate, String hint) {
        this.code = code;
        this.messageTemplate = messageTemplate;
        this.hint = hint;
    }

    public String getCode() {
        return code;
    }

    /**
     * 格式化消息模板，用法同 String.format。
     * 例如：BbsError.FILM_NOT_FOUND.format("default") → "Film 'default' 不存在"
     */
    public String format(Object... args) {
        return String.format(messageTemplate, args);
    }

    public String getHint() {
        return hint;
    }
}
