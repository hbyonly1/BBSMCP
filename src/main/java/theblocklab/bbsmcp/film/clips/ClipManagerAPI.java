package theblocklab.bbsmcp.film.clips;

import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.network.ServerNetwork;
import mchorse.bbs_mod.data.DataParser;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.server.network.ServerPlayerEntity;

public class ClipManagerAPI {

    /**
     * 从 JSON 添加或覆盖 Clip（index 和 type 从 JSON 字段中读取）。
     * 所有业务规则校验均在此方法内完成，失败时抛出 BbsException。
     */
    public static void addClip(ServerPlayerEntity player, String filmId, String json) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        // 解析 JSON 为 MapType
        BaseType data = DataParser.parse(json);
        if (data == null || !data.isMap()) {
            throw new BBSMCPException(BBSMCPError.CLIP_INVALID_JSON);
        }
        MapType mapData = data.asMap();

        // 校验 type 字段
        String type = mapData.getString("type");
        if (type == null || type.isEmpty()) {
            throw new BBSMCPException(BBSMCPError.CLIP_MISSING_FIELD, "type");
        }

        // 校验 index 字段
        if (!mapData.has("index")) {
            throw new BBSMCPException(BBSMCPError.CLIP_MISSING_FIELD, "index");
        }
        int index = mapData.getInt("index");

        // 校验 index 范围
        int currentSize = film.camera.get().size();
        if (index < 0 || index > currentSize) {
            throw new BBSMCPException(BBSMCPError.CLIP_INDEX_INVALID, index, filmId, currentSize);
        }

        // 创建 Clip 实例
        Link typeLink = Link.bbs(type);
        Clip clip = (Clip) film.camera.getFactory().create(typeLink);
        if (clip == null) {
            throw new BBSMCPException(BBSMCPError.CLIP_UNKNOWN_TYPE, type);
        }

        clip.fromData(mapData);

        if (index < currentSize) {
            // 已存在则覆盖数据
            film.camera.get(index).copy(clip);
        } else {
            // 追加到末尾
            film.camera.addClip(clip);
        }

        FilmManagerAPI.pushFilmToUI(player, filmId, (MapType) film.toData());
        FilmManagerAPI.INSTANCE.saveFilm(filmId, (MapType) film.toData());

        // 选中刚刚添加/更新的 Clip
        pickClip(player, filmId, clip);
    }

    /**
     * 删除指定索引的 Clip 并同步到客户端。
     */
    public static void removeClip(ServerPlayerEntity player, String filmId, int index) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        Clip clip = film.camera.get(index);
        if (clip == null) {
            throw new BBSMCPException(BBSMCPError.CLIP_NOT_FOUND, index);
        }

        film.camera.remove(clip);

        FilmManagerAPI.pushFilmToUI(player, filmId, (MapType) film.toData());
        FilmManagerAPI.INSTANCE.saveFilm(filmId, (MapType) film.toData());
    }

    /**
     * 获取 Film 下的所有 Clip（JSON 字符串）。
     */
    public static String getClips(String filmId) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        ListType list = new ListType();
        for (Clip clip : film.camera.get()) {
            list.add(clip.toData());
        }
        return list.toString();
    }

    /**
     * 按索引查询单个 Clip。
     */
    public static String getClipsByIndex(String filmId, int index) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        Clip clip = film.camera.get(index);
        if (clip == null) {
            throw new BBSMCPException(BBSMCPError.CLIP_NOT_FOUND, index);
        }
        return clip.toData().toString();
    }

    /**
     * 查询覆盖指定 tick 的所有 Clip。
     */
    public static String getClipsByTick(String filmId, int tick) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        ListType list = new ListType();
        for (Clip clip : film.camera.getClips(tick)) {
            list.add(clip.toData());
        }
        return list.toString();
    }

    /**
     * 查询指定 tick + layer 上的单个 Clip。
     */
    public static String getClipByTickAndLayer(String filmId, int tick, int layer) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        Clip clip = film.camera.getClipAt(tick, layer);
        if (clip == null) {
            throw new BBSMCPException(BBSMCPError.CLIP_NOT_FOUND, tick);
        }
        return clip.toData().toString();
    }

    /**
     * 选中指定 Clip，发送网络包通知客户端。
     */
    public static <T extends Clip> T pickClip(ServerPlayerEntity player, String filmId, T clip) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        int clipIndex = film.camera.getIndex(clip);
        ServerNetwork.sendPickClipPacket(player, filmId, clipIndex);
        return clip;
    }
}
