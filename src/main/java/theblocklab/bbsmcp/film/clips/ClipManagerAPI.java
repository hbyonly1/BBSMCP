package theblocklab.bbsmcp.film.clips;

import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.clips.utils.ClipDataConverter;
import theblocklab.bbsmcp.film.clips.utils.ClipValidationChecker;
import theblocklab.bbsmcp.network.ServerNetwork;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;

import mchorse.bbs_mod.data.DataParser;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.server.network.ServerPlayerEntity;

public class ClipManagerAPI {

    /**
     * 从 JSON 添加或覆盖 Clip（index 和 type 从 JSON 字段中读取）。
     * 所有业务规则校验均在此方法内完成，失败时抛出 BBSMCPException。
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

        // [核心保护机制] 确保在这个图层上，新追加或修改后的 Clip 不会与其它任何人重叠冲突
        ClipValidationChecker.validateNoOverlap(film, clip, index);

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

        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        java.util.List<Clip> clips = film.camera.get();
        for (int i = 0; i < clips.size(); i++) {
            array.add(ClipDataConverter.convertClipToJson(clips.get(i), i));
        }
        return array.toString();
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
        return ClipDataConverter.convertClipToJson(clip, index).toString();
    }

    /**
     * 查询覆盖指定 tick 的所有 Clip。
     */
    public static String getClipsByTick(String filmId, int tick) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        java.util.List<Clip> clipsForTick = film.camera.getClips(tick);

        // 注意：getClips(tick) 返回的是筛选后的片段，它的下标并不能直接体现它在原本数组的 index
        // 为了确保 index 正确，我们需要通过 getIndex 找到它真正的序号
        for (Clip clip : clipsForTick) {
            int realIndex = film.camera.getIndex(clip);
            array.add(ClipDataConverter.convertClipToJson(clip, realIndex));
        }
        return array.toString();
    }

    /**
     * 查询指定 layer 上的所有 Clip。
     */
    public static String getClipsByLayer(String filmId, int layer) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

        JsonArray array = new JsonArray();
        List<Clip> clips = film.camera.get();
        for (int i = 0; i < clips.size(); i++) {
            Clip clip = clips.get(i);
            if ((Integer) clip.layer.get() == layer) {
                array.add(ClipDataConverter.convertClipToJson(clip, i));
            }
        }
        return array.toString();
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
        int realIndex = film.camera.getIndex(clip);
        return ClipDataConverter.convertClipToJson(clip, realIndex).toString();
    }

    /**
     * 向客户端发送异步执行 Film 持久化保存的强制命令
     */
    public static CompletableFuture<Boolean> requestSaveFilmAsync(ServerPlayerEntity player, String filmId) {
        // 利用底层预先校验机制阻挡无效请求
        FilmManagerAPI.INSTANCE.getFilm(filmId);
        return theblocklab.bbsmcp.network.ServerNetwork.requestClientSaveFilmPacket(player, filmId);
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
