package theblocklab.bbsmcp.exception;

/**
 * BBSMCP 统一业务异常。
 * 所有 Domain / Core 层的业务规则校验失败都应抛出此异常，
 * 而不是依赖 null 返回值或散落的 IllegalArgumentException。
 *
 * 使用方式：
 * throw new BBSMCPException(BBSMCPError.FILM_NOT_FOUND, filmId);
 *
 * 在 Application 层（MCPTool）捕获：
 * try {
 * ClipManagerAPI.addClip(player, filmId, json);
 * } catch (BBSMCPException e) {
 * return MCPToolResponse.error(e.getMessage(), e.getHint());
 * }
 */
public class BBSMCPException extends RuntimeException {

    private final BBSMCPError error;
    private final String hint;

    /**
     * 从 BBSMCPError 枚举构造，消息由枚举模板格式化。
     *
     * @param error 错误码枚举
     * @param args  填入消息模板的参数（对应 %s / %d 占位符）
     */
    public BBSMCPException(BBSMCPError error, Object... args) {
        super(error.format(args));
        this.error = error;
        this.hint = error.getHint();
    }

    /**
     * 返回错误码，格式如 "FILM_001"。
     * 可用于日志中的精确识别。
     */
    public String getCode() {
        return error.getCode();
    }

    /**
     * 返回给 AI 的行动建议。
     */
    public String getHint() {
        return hint;
    }

    /**
     * 返回所属的 BbsError 枚举项。
     */
    public BBSMCPError getError() {
        return error;
    }
}
