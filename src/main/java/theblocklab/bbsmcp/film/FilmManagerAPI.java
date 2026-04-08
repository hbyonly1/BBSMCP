package theblocklab.bbsmcp.film;

import java.util.Collection;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.FilmManager;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import theblocklab.bbsmcp.exception.BBSMCPError;
import theblocklab.bbsmcp.exception.BBSMCPException;

/**
 * Film 管理器
 */
public class FilmManagerAPI {
    private FilmManager films = BBSMod.getFilms();

    public static final String DEFAULT_FILM_ID = "default";
    public static final FilmManagerAPI INSTANCE = new FilmManagerAPI();

    // 为某个玩家创建film
    public void createFilm(String filmId) {
        this.films.save(filmId, (MapType) this.films.create(filmId).toData());
    }

    public void deleteFilm(String filmId) {
        boolean success = this.films.delete(filmId);
        if (!success) {
            throw new BBSMCPException(BBSMCPError.FILM_NOT_FOUND, filmId);
        }
    }

    public void saveFilm(String filmId, MapType filmData) {
        this.films.save(filmId, filmData);
    }

    public Collection<String> getFilmsList() {
        return this.films.getKeys();
    }

    public boolean hasFilm(String filmId) {
        return this.films.getKeys().contains(filmId);
    }

    public Film getFilm(String filmId) {
        Film film = this.films.load(filmId);
        if (film == null) {
            throw new BBSMCPException(BBSMCPError.FILM_NOT_FOUND, filmId);
        }
        return film;
    }

    public static void pushFilmS2C(ServerPlayerEntity player, String filmId, Film film) {
        MapType filmData = (MapType) film.toData();
        pushFilmToUI(player, filmId, filmData);
        INSTANCE.saveFilm(filmId, filmData);
    }

    public static void pushFilmS2C(ServerPlayerEntity player, String filmId, MapType filmData) {
        pushFilmToUI(player, filmId, filmData);
        INSTANCE.saveFilm(filmId, filmData);
    }

    // 若要同步到客户端，应先向客户端 UI 填充电影数据！再保存
    // UI 会定期保存，所以第一更改 UI 的数据
    private static void pushFilmToUI(ServerPlayerEntity player, String filmId, MapType filmData) {
        // 客户端接收后实际上会向 UI 同步数据
        theblocklab.bbsmcp.network.ServerNetwork.sendServerFilmDataPacket(player, filmId, filmData);
    }

    public static void playFilm(ServerPlayerEntity player, String filmId, boolean withCamera) {
        ServerNetwork.sendPlayFilm(player, filmId, withCamera);
    }

    public static void playFilm(ServerPlayerEntity player, ServerWorld world, String filmId, boolean withCamera) {
        ServerNetwork.sendPlayFilm(player, world, filmId, withCamera);
    }

    public static void pauseFilm(ServerPlayerEntity player, String filmId) {
        ServerNetwork.sendPauseFilm(player, filmId);
    }

    public static void stopFilm(ServerPlayerEntity player, String filmId) {
        ServerNetwork.sendStopFilm(player, filmId);
    }

    public static java.util.concurrent.CompletableFuture<String> captureScreenshot(ServerPlayerEntity player,
            int targetTick, int startTick) {
        return theblocklab.bbsmcp.network.ServerNetwork.requestClientCaptureScreenshotPacket(player, targetTick,
                startTick);
    }

    /**
     * 向客户端发送异步执行 Film 持久化保存的强制命令
     */
    public static java.util.concurrent.CompletableFuture<Boolean> requestClientSaveFilm(ServerPlayerEntity player,
            String filmId) {
        // 利用底层预先校验机制阻挡无效请求
        FilmManagerAPI.INSTANCE.getFilm(filmId);
        return theblocklab.bbsmcp.network.ServerNetwork.requestClientSaveFilmPacket(player, filmId);
    }
}
