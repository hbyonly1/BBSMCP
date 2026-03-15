package theblocklab.bbsmcp.film.clips;

import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.network.ServerNetwork;
import mchorse.bbs_mod.data.DataParser;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.server.network.ServerPlayerEntity;

public class ClipFactory {
    private static Clip lastClip;

    // 从 JSON 添加 clip
    public static void addClipFromJSON(String filmId, int index, String type, String json) {
        try {
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
            if (film == null) {
                throw new IllegalArgumentException("Film 未找到: " + filmId);
            }

            // 创建 Link 和 Clip 实例
            Link typeLink = Link.bbs(type);
            Clip clip = (Clip) film.camera.getFactory().create(typeLink);
            if (clip == null) {
                throw new IllegalArgumentException("未知的 Clip 类型: " + type);
            }

            // 解析 JSON 为 MapType
            BaseType data = DataParser.parse(json);
            if (data == null || !data.isMap()) {
                throw new IllegalArgumentException("JSON 必须是对象格式: {...}");
            }
            MapType mapData = data.asMap();

            clip.fromData(mapData);

            // 检查要添加的片段是否已存在, 若存在, 则修改数据(注: 暂时不能更改 clip 类型)
            if (index >= 0 && index < film.camera.get().size()) {
                film.camera.get(index).copy(clip);
            } else {
                // 不存在，则添加新的 Clip 到 Film
                film.camera.addClip(clip);
            }

            // 保存到内存中
            lastClip = clip;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 选中片段（改为实例方法）
    public static <T extends Clip> T pickClip(String filmId, ServerPlayerEntity player, T clip) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        if (film == null) {
            throw new IllegalArgumentException("Film 未找到: " + filmId);
        }

        int clipIndex = film.camera.getIndex(clip);
        ServerNetwork.sendPickClipPacket(player, filmId, clipIndex);
        return clip;
    }

    // 选中最后添加的片段
    public static void pickLastClip(String filmId, ServerPlayerEntity player) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        if (film == null) {
            throw new IllegalArgumentException("Film 未找到: " + filmId);
        }

        int clipIndex = film.camera.getIndex(lastClip);
        if (clipIndex == -1) {
            throw new IllegalArgumentException("Film id: " + filmId + " Clip 无效!");
        }

        ServerNetwork.sendPickClipPacket(player, filmId, clipIndex);
    }
}
