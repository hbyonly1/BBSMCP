package theblocklab.bbsmcp.film;

import java.util.Collection;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Film 管理器
 */
public class FilmManagerAPI {
    private FilmManager films = BBSMod.getFilms();

    public static final String DEFAULT_FILM_ID = "default";
    public static final FilmManagerAPI INSTANCE = new FilmManagerAPI();

    // 为某个玩家创建film
    public void createFilm(MinecraftServer server, String filmId) {
        // 这个应该是异步的
        this.films.save(filmId, (MapType)this.films.create(filmId).toData());
        // 为什么要sync?让客户端打开保存好的不行吗
        //syncFilmS2C(server.getPlayerManager().getPlayerList().get(0) , filmId);
    }

    public Collection<String> getFilmsList() {
        return this.films.getKeys();
    }

    public Film getFilm(String filmId) {
        Film film = this.films.load(filmId);
        if (film == null) {
            throw new IllegalArgumentException("Film not found: " + filmId);
        }
        return film;
    }

    public void syncFilmC2S(ServerPlayerEntity player, String filmId) {
        theblocklab.bbsmcp.network.ServerNetwork.requestClientFilmDataPacket(player, filmId);
    }

    // 若要同步到客户端，应先保存到磁盘，否则客户端 UI 读取不到实际文件，无法播放和操作
    // sync 完成单一职能，已精简
    public void syncFilmS2C(ServerPlayerEntity player, String filmId) {
        // 客户端接收后实际上会向 UI 同步数据
        theblocklab.bbsmcp.network.ServerNetwork.sendServerFilmDataPacket(player, filmId);
    }

    public void playFilm(ServerPlayerEntity player, String filmId, boolean withCamera) {
        ServerNetwork.sendPlayFilm(player, filmId, withCamera);
    }

    public void playFilm(ServerPlayerEntity player, ServerWorld world, String filmId, boolean withCamera) {
        ServerNetwork.sendPlayFilm(player, world, filmId, withCamera);
    }

    public void pauseFilm(ServerPlayerEntity player, String filmId) {
        ServerNetwork.sendPauseFilm(player, filmId);
    }

    public void stopFilm(ServerPlayerEntity player, String filmId) {
        ServerNetwork.sendStopFilm(player, filmId);
    }

    /**
     * 自动初始化 Film
     * 在玩家加入服务器时调用
     */
    // public void initOnJoin(ServerPlayerEntity player) {
    //     try {
    //         if (isEmpty()) {
    //             createFilm(DEFAULT_FILM_ID);
    //         }
    //         syncFilmS2C(player, DEFAULT_FILM_ID);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
}
