package theblocklab.bbsmcp.film.clips;

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
    // private static Clip lastClip;

    // public static final ClipManagerAPI INSTANCE = new ClipManagerAPI();

    // 从 JSON 添加 clip（index 和 type 从 JSON 字段中读取）
    public static void addClipFromJSON(ServerPlayerEntity player, String filmId, String json) {
        try {
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
            if (film == null) {
                throw new IllegalArgumentException("Film ID 无效: " + filmId);
            }

            // 解析 JSON 为 MapType
            BaseType data = DataParser.parse(json);
            if (data == null || !data.isMap()) {
                throw new IllegalArgumentException("JSON 必须是对象格式: {...}");
            }
            MapType mapData = data.asMap();

            // 从 JSON 中读取 type 字段
            String type = mapData.getString("type");
            if (type == null || type.isEmpty()) {
                throw new IllegalArgumentException("JSON 中缺少 'type' 字段");
            }

            // 从 JSON 中读取 index 字段（必填）
            if (!mapData.has("index")) {
                throw new IllegalArgumentException("JSON 中缺少必填字段 'index'");
            }
            int index = mapData.getInt("index");

            // 创建 Link 和 Clip 实例
            Link typeLink = Link.bbs(type);
            Clip clip = (Clip) film.camera.getFactory().create(typeLink);
            if (clip == null) {
                throw new IllegalArgumentException("未知的 Clip 类型: " + type);
            }

            clip.fromData(mapData);

            if (index >= 0 && index < film.camera.get().size()) {
                // 已存在则覆盖数据（暂不支持更改 clip 类型）
                film.camera.get(index).copy(clip);
            } else if (index == film.camera.get().size()) {
                // 不存在则添加
                film.camera.addClip(clip);
            } else {
                throw new IllegalArgumentException("Clip index 无效: " + index);
            };

            FilmManagerAPI.pushFilmToUI(player, filmId, (MapType)film.toData());
            // load 返回的仅仅是 film 的数据副本，要使改动生效，应该保存到硬盘
            FilmManagerAPI.INSTANCE.saveFilm(filmId, (MapType)film.toData());
            System.out.println(film.toData().toString());

            // 选中刚刚添加/更新的 Clip
            pickClip(player, filmId, clip);

            // 保存到内存中
            //lastClip = clip;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeClip(ServerPlayerEntity player, String filmId, int index) {
        try {
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
            if (film == null) {
                throw new IllegalArgumentException("Film ID 无效: " + filmId);
            }

            Clip clip = film.camera.get(index);
            if (clip == null) {
                throw new IllegalArgumentException("Clip index 无效: " + index);
            }

            // 删除 Clip
            film.camera.remove(clip);

            FilmManagerAPI.pushFilmToUI(player, filmId, (MapType)film.toData());
            // load 返回的仅仅是 film 的数据副本，要使改动生效，应该保存到硬盘
            FilmManagerAPI.INSTANCE.saveFilm(filmId, (MapType)film.toData());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getClips(String filmId) {
        try {
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
            if (film == null) {
                throw new IllegalArgumentException("Film ID 无效: " + filmId);
            }
            ListType list = new ListType();
            for (Clip clip : film.camera.get()){
                list.add(clip.toData());
            }
            return list.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getClipsByIndex(String filmId, int index) {
        try {
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
            if (film == null) {
                throw new IllegalArgumentException("Film ID 无效: " + filmId);
            }

            Clip clip = film.camera.get(index);
            return clip == null ? "" : clip.toData().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getClipsByTick(String filmId, int tick) {
        try {
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
            if (film == null) {
                throw new IllegalArgumentException("Film ID 无效: " + filmId);
            }

            ListType list = new ListType();
            for (Clip clip : film.camera.getClips(tick)){
                list.add(clip.toData());
            }
            return list.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getClipByTickAndLayer(String filmId, int tick, int layer) {
        try {
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
            if (film == null) {
                throw new IllegalArgumentException("Film ID 无效: " + filmId);
            }

            Clip clip = film.camera.getClipAt(tick, layer);
            return clip == null ? "" : clip.toData().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // 选中片段
    public static <T extends Clip> T pickClip(ServerPlayerEntity player, String filmId, T clip) {
        Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
        if (film == null) {
            throw new IllegalArgumentException("Film ID 无效: " + filmId);
        }

        int clipIndex = film.camera.getIndex(clip);
        ServerNetwork.sendPickClipPacket(player, filmId, clipIndex);
        return clip;
    }

    // 选中最后添加的片段
    // public void pickLastClip(ServerPlayerEntity player, String filmId) {
    //     Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);
    //     if (film == null) {
    //         throw new IllegalArgumentException("Film ID 无效: " + filmId);
    //     }

    //     int clipIndex = film.camera.getIndex(lastClip);
    //     if (clipIndex == -1) {
    //         throw new IllegalArgumentException("Film id: " + filmId + " Clip 无效!");
    //     }

    //     ServerNetwork.sendPickClipPacket(player, filmId, clipIndex);
    // }
}
