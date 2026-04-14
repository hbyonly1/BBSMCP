package theblocklab.bbsmcp.film.replays;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import mchorse.bbs_mod.data.DataParser;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import net.minecraft.server.network.ServerPlayerEntity;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.utils.JsonFormatUtils;

/**
 * Replay 管理器——参照 ClipManagerAPI 的设计风格
 * 所有写操作都会在执行完毕后自动同步到客户端 UI 并落盘保存
 */
public class ReplayManagerAPI {

    // ────────────── 内部工具方法 ──────────────

    /**
     * 安全地从 Film 中按真实列表索引取 Replay，取不到则抛出异常
     */
    public static Replay getReplay(Film film, String filmId, int index) {
        List<Replay> list = film.replays.getList();
        if (index < 0 || index >= list.size()) {
            throw new BBSMCPException(BBSMCPError.REPLAY_NOT_FOUND, index, filmId, list.size());
        }
        return list.get(index);
    }

    /**
     * 安全地从 Replay 的 keyframes 中取通道，取不到则抛出异常
     */
    private static KeyframeChannel<?> getChannel(Replay replay, String channelId) {
        KeyframeChannel<?> val = (KeyframeChannel<?>) replay.keyframes.get(channelId);
        if (val == null) {
            throw new BBSMCPException(BBSMCPError.REPLAY_CHANNEL_NOT_FOUND, channelId);
        }
        return val;
    }

    private static List<KeyframeChannel<?>> getChannels(Replay replay) {
        return replay.keyframes.getChannels();
    }

    // ────────────── 查询 ──────────────

    /**
     * 获取 Film 中所有 Replay 的元信息（不含关键帧明细）
     */
    public static String getReplays(String filmId) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        List<Replay> list = film.replays.getList();
        JsonArray array = new JsonArray();
        for (int i = 0; i < list.size(); i++) {
            Replay replay = list.get(i);
            JsonObject obj = buildReplayMeta(replay, i);
            array.add(obj);
        }
        return JsonFormatUtils.pretty(array.toString());
    }

    /**
     * 获取指定 Replay 的元信息（不含关键帧明细）
     */
    public static String getReplayByIndex(String filmId, int index) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, index);
        return JsonFormatUtils.pretty(buildReplayMeta(replay, index).toString());
    }

    /**
     * 精确查询指定 Replay 的关键帧数据
     *
     * @param filmId   Film ID
     * @param index    Replay 索引
     * @param channels 要查询的通道 ID 列表（null 或空则返回全部通道）
     * @param fromTick 起始 tick（-1 表示不限制）
     * @param toTick   结束 tick（-1 表示不限制）
     */
    public static String getKeyframes(String filmId, int index,
            List<String> channels, float fromTick, float toTick) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, index);

        // 决定要查哪些通道
        List<String> targetChannels;
        if (channels == null || channels.isEmpty()) {
            // 查全部通道
            targetChannels = getChannels(replay).stream()
                    .map(ch -> ch.getId())
                    .toList();
        } else {
            targetChannels = channels;
        }

        JsonObject result = new JsonObject();
        for (String channelId : targetChannels) {
            KeyframeChannel<?> channel = getChannel(replay, channelId);
            JsonArray frames = new JsonArray();
            for (var kf : channel.getKeyframes()) {
                float tick = kf.getTick();
                if (fromTick >= 0 && tick < fromTick)
                    continue;
                if (toTick >= 0 && tick > toTick)
                    continue;
                JsonObject fo = new JsonObject();
                fo.addProperty("tick", tick);
                fo.addProperty("value", kf.getValue() == null ? "null" : kf.getValue().toString());
                fo.addProperty("interpolation", kf.getInterpolation().toString());
                frames.add(fo);
            }
            result.add(channelId, frames);
        }
        return JsonFormatUtils.pretty(result.toString());
    }

    // ────────────── 写入 ──────────────

    /**
     * 新建一个 Replay 并返回其列表索引
     */
    public static int addReplay(ServerPlayerEntity player, String filmId) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        int newIndex = film.replays.getList().size();
        film.replays.addReplay();
        FilmManagerAPI.pushFilmS2C(player, filmId, film);
        return newIndex;
    }

    /**
     * 按索引删除 Replay
     */
    public static void removeReplay(ServerPlayerEntity player, String filmId, int index) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, index);
        film.replays.remove(replay);
        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    /**
     * 设置 Replay 的基本属性，所有参数可选（传 null 表示不修改）
     */
    public static void setReplayProps(ServerPlayerEntity player, String filmId, int index,
            Boolean enabled, String label, String nameTag,
            Boolean actor, Boolean fp, Integer looping) {

        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, index);

        if (enabled != null)
            replay.enabled.set(enabled);
        if (label != null)
            replay.label.set(label);
        if (nameTag != null)
            replay.nameTag.set(nameTag);
        if (actor != null)
            replay.actor.set(actor);
        if (looping != null)
            replay.looping.set(looping);

        // fp，即 first person，需要保证全局唯一
        if (fp != null) {
            if (fp) {
                // 先清除其他所有 Replay 的 fp 标志
                for (Replay r : film.replays.getList()) {
                    r.fp.set(false);
                }
            }
            replay.fp.set(fp);
        }

        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    /**
     * 从 JSON 字符串设置 Replay 的 Form
     */
    public static void setReplayForm(ServerPlayerEntity player, String filmId, int index, String formJson) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, index);

        BaseType data = DataParser.parse(formJson);
        if (data == null || !data.isMap()) {
            throw new BBSMCPException(BBSMCPError.CLIP_INVALID_JSON);
        }
        replay.form.fromData(data.asMap());
        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    /**
     * 为 Replay 设置原版生物形式（等同于在 UI Palette 的分类中选择某个生物）
     * 原版生物统一通过 MobForm 表示，并指定对应的 Minecraft 实体 ID
     *
     * @param mobId 原版实体 ID，例如 "minecraft:pig", "minecraft:zombie"
     */
    public static void setReplayVanillaMob(ServerPlayerEntity player, String filmId, int index, String mobId) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, index);

        // 创建 MobForm 对象并设置其内部对应的 mob 标识
        MobForm mobForm = new MobForm();
        mobForm.mobID.set(mobId);

        // 使用 FormUtils.copy 来安全复制并赋予 Replay
        replay.form.set(FormUtils.copy(mobForm));

        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    /**
     * 向指定通道插入单个关键帧，value 统一用 String 接收，内部根据通道类型解析
     */
    public static void addKeyframe(ServerPlayerEntity player, String filmId, int replayIndex,
            String channelId, float tick, String value) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, replayIndex);
        KeyframeChannel<?> channel = getChannel(replay, channelId);

        channel.insert(tick, parseValue(channel, value));

        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    /**
     * 批量多通道插入关键帧
     *
     * @param channelFrames key=channelId, value=List of {tick, value} JsonObjects
     */
    public static void batchAddKeyframes(ServerPlayerEntity player, String filmId, int replayIndex,
            JsonObject channelFrames, String defaultInterpolation) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, replayIndex);

        for (String channelId : channelFrames.keySet()) {
            KeyframeChannel<?> channel = getChannel(replay, channelId);
            JsonArray frames = channelFrames.getAsJsonArray(channelId);
            for (var elem : frames) {
                JsonObject fo = elem.getAsJsonObject();
                float tick = fo.get("tick").getAsFloat();
                String value = fo.get("value").getAsString();
                channel.insert(tick, parseValue(channel, value));
                // 若帧本身不指定插值类型，则使用 defaultInterpolation
                // (BBS KeyframeChannel.insert 目前不接受插值参数，插值在整个通道层面配置)
            }
        }

        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    /**
     * 删除指定通道在时间区间内的关键帧
     *
     * @param channels  null 表示操作全部通道
     * @param fromTick  区间起点，-1 表示不限
     * @param toTick    区间终点，-1 表示不限（即 fromTick 之后全部删除）
     * @param exactTick 精确删除单点，与区间删除互斥。-1 表示不使用此模式
     */
    public static void removeKeyframes(ServerPlayerEntity player, String filmId, int replayIndex,
            List<String> channels, float fromTick, float toTick, float exactTick) {

        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        Replay replay = getReplay(film, filmId, replayIndex);

        // 确定操作哪些通道
        List<KeyframeChannel<?>> targets = new java.util.ArrayList<>();
        if (channels == null || channels.isEmpty()) {
            // 操作全部通道
            targets.addAll(getChannels(replay));
        } else {
            for (String id : channels) {
                targets.add(getChannel(replay, id));
            }
        }

        for (KeyframeChannel<?> channel : targets) {
            java.util.List<?> list = channel.getKeyframes();
            // channel.getKeyframes() 返回的是 unmodifiableList，因此不能直接 removeIf
            // 必须倒序遍历，并调用 channel.remove(index) 来逐个删除
            for (int i = list.size() - 1; i >= 0; i--) {
                Keyframe<?> kf = (Keyframe<?>) list.get(i);
                float t = kf.getTick();
                boolean shouldRemove;
                if (exactTick >= 0) {
                    shouldRemove = (t == exactTick);
                } else {
                    boolean afterFrom = fromTick < 0 || t >= fromTick;
                    boolean beforeTo = toTick < 0 || t <= toTick;
                    shouldRemove = afterFrom && beforeTo;
                }
                if (shouldRemove) {
                    channel.remove(i);
                }
            }
        }

        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    /**
     * 将指定 Replay 中指定通道在时间区间内的关键帧进行平移。
     * 采用“安全平移协议”：将受影响的关键帧暂时从通道取出并重新插入，以利用引擎内置的排序机制。
     *
     * @param channels null 表示操作全部通道
     * @param fromTick 区间起点
     * @param toTick   区间终点，-1 表示不限
     * @param offset   偏移量（float）
     */
    public static void shiftKeyframes(ServerPlayerEntity player, String filmId, int replayIndex,
            List<String> channels, float fromTick, float toTick, float offset) {

        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        mchorse.bbs_mod.film.replays.Replay replay = getReplay(film, filmId, replayIndex);

        // 确定操作哪些通道
        List<KeyframeChannel<?>> targets = new ArrayList<>();
        if (channels == null || channels.isEmpty()) {
            targets.addAll(getChannels(replay));
        } else {
            for (String id : channels) {
                targets.add(getChannel(replay, id));
            }
        }

        for (KeyframeChannel<?> channel : targets) {
            List<? extends Keyframe<?>> list = channel.getKeyframes();
            List<Keyframe<?>> toMove = new java.util.ArrayList<>();

            // 1. 甄别并提取需要移动的关键帧（倒着删，防止索引塌缩）
            for (int i = list.size() - 1; i >= 0; i--) {
                Keyframe<?> kf = list.get(i);
                float t = kf.getTick();
                if (t >= fromTick && (toTick < 0 || t <= toTick)) {
                    toMove.add(kf);
                    channel.remove(i);
                }
            }

            // 2. 将修改过 tick 的关键帧重新插回（使用 insert 保证有序）
            for (Keyframe<?> kf : toMove) {
                // 修改 tick 后插回
                kf.setTick(kf.getTick() + offset);
                // 复用 parseValue 模式来绕过通配符 capture 限制
                channel.insert(kf.getTick(), parseValue(channel, kf.getValue().toString()));
            }
        }

        FilmManagerAPI.pushFilmS2C(player, filmId, film);
    }

    // ────────────── 私有辅助 ──────────────

    /** 构造 Replay 的元信息 JsonObject（不含关键帧详细内容） */
    // 有意为之的轻量化设计，没有选择 toData()
    private static JsonObject buildReplayMeta(Replay replay, int index) {
        JsonObject obj = new JsonObject();
        obj.addProperty("index", index);
        obj.addProperty("id", replay.getId()); // Replay extends ValueGroup, getId() 取诅属性 id
        obj.addProperty("label", (String) replay.label.get());
        obj.addProperty("nameTag", (String) replay.nameTag.get());
        obj.addProperty("enabled", (Boolean) replay.enabled.get());
        obj.addProperty("actor", (Boolean) replay.actor.get());
        obj.addProperty("fp", (Boolean) replay.fp.get());
        obj.addProperty("looping", (Integer) replay.looping.get());
        obj.addProperty("shadow", (Boolean) replay.shadow.get());
        obj.addProperty("hasForm", replay.form.get() != null);
        // 统计各通道关键帧数量（轻量信息）
        JsonObject channelCounts = new JsonObject();
        for (Object obj2 : getChannels(replay)) {
            if (obj2 instanceof KeyframeChannel<?> ch && !ch.isEmpty()) {
                channelCounts.addProperty(ch.getId(), ch.getKeyframes().size());
            }
        }
        obj.add("keyframeCounts", channelCounts);
        return obj;
    }

    /** 根据通道内工厂类型，将 String 解析为对应值类型 */
    @SuppressWarnings("unchecked")
    private static <T> T parseValue(KeyframeChannel<?> channel, String value) {
        try {
            // 尝试解析为数字（绝大多数通道）
            return (T) Double.valueOf(value);
        } catch (NumberFormatException e) {
            // 尝试解析为 JSON/NBT 结构 (例如物品、颜色等)
            BaseType parsed = DataParser.parse(value);
            if (parsed != null) {
                return (T) parsed;
            }
            // 实在不行返回原始字符串
            return (T) value;
        }
    }
}
