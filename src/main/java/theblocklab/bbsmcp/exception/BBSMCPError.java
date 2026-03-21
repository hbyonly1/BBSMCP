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

    // ── 运行环境相关 ─────────────────────────────────────────────────────────
    PLAYER_NOT_ONLINE("ENV_001", "服务器内没有在线玩家",
            "同步 Film 需要至少一名在线玩家，请先登录游戏"),
    PLAYER_NOT_FOUND("ENV_002", "找不到玩家 '%s'",
            "请确认玩家是否在线，并检查名称拼写"),

    // ── 参数校验相关 ─────────────────────────────────────────────────────────
    MISSING_PARAM("PARAM_001", "缺少必填参数 '%s'",
            "请检查工具调用参数，确保所有 required 字段都已提供");

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
