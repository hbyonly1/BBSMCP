package theblocklab.bbsmcp.network;

import theblocklab.bbsmcp.film.FilmManagerAPI;

import java.util.concurrent.CompletableFuture;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.network.ServerPacketCrusher;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ServerNetwork {
    // === bbs 相关 Identifier ===
    // Identifier 是数据包的实际名称，例如 SERVER_FILM_DATA 就是 server 端的 data
    public static final Identifier REQUEST_CLIENT_FILM_DATA = new Identifier("bbsmcp", "request_client_film_data");
    public static final Identifier CLIENT_FILM_DATA = new Identifier("bbsmcp", "client_film_data");
    public static final Identifier SERVER_FILM_DATA = new Identifier("bbsmcp", "server_film_data");

    // === UI Query & Command ===
    public static final Identifier CLIENT_PICK_CLIP = new Identifier("bbsmcp", "client_pick_clip");
    public static final Identifier CLIENT_OPEN_FILM_PANEL = new Identifier("bbsmcp", "client_open_film_panel");
    public static final Identifier CLIENT_OPEN_REPLAY_EDITOR = new Identifier("bbsmcp", "client_open_replay_editor");
    public static final Identifier CLIENT_TOGGLE_PLAYBACK = new Identifier("bbsmcp", "client_toggle_playback");
    public static final Identifier CLIENT_CLOSE_UI = new Identifier("bbsmcp", "client_close_ui");

    public static final Identifier CLIENT_SET_CURSOR = new Identifier("bbsmcp", "client_set_cursor");
    public static final Identifier CLIENT_GET_CURSOR = new Identifier("bbsmcp", "client_get_cursor");

    public static final Identifier CLIENT_SAVE_FILM = new Identifier("bbsmcp", "client_save_film");

    // === AI building 相关 Identifier ===
    public static final Identifier GENERATE_BUILDING_C2S = new Identifier("bbsmcp", "generate_building");
    public static final Identifier BUILDING_PROGRESS_S2C = new Identifier("bbsmcp", "building_progress");

    // === Anchor 锚点系统 ===
    public static final Identifier S2C_ANCHOR_LIST = new Identifier("bbsmcp", "s2c_anchor_list");
    public static final Identifier S2C_ANCHOR_UPDATE = new Identifier("bbsmcp", "s2c_anchor_update");
    public static final Identifier S2C_ANCHOR_TOGGLE_VISIBILITY = new Identifier("bbsmcp", "s2c_anchor_toggle_visibility");
    public static final Identifier C2S_ANCHOR_CREATE = new Identifier("bbsmcp", "c2s_anchor_create");
    public static final Identifier C2S_ANCHOR_REMOVE = new Identifier("bbsmcp", "c2s_anchor_remove");
    public static final Identifier C2S_ANCHOR_SYNC_REQUEST = new Identifier("bbsmcp", "c2s_anchor_sync_request");

    // === utils 相关 Identifier ===
    public static final Identifier OK = new Identifier("bbsmcp", "ok");
    public static final Identifier CLIENT_ERROR = new Identifier("bbsmcp", "client_error");
    public static final Identifier CLIENT_DATA_RESPONSE = new Identifier("bbsmcp", "client_data_response");

    private static ServerPacketCrusher crusher = new ServerPacketCrusher();

    public static void setup() {
        // 初始化异步桥接器（接收客户端发来的 OK 包）
        ServerRequestBridge.setup();

        // 注册 server 接收器相关代码
        // === BBS 相关逻辑 ===
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_FILM_DATA,
                (server, player, handler, buf, responseSender) -> {
                    handleRequestClientFilmDataPacket(player, buf);
                });

        // === AI building 相关逻辑 ===
    }
    // === AI building 相关逻辑 ===

    // === BBS 相关逻辑 ===

    // 请求客户端打开指定的 filmPanel 的逻辑
    public static CompletableFuture<Boolean> requestClientOpenFilmPanelPacket(ServerPlayerEntity player,
            String filmId) {
        player.sendMessage(Text.literal(String.format("§c[BBSMCP Server] 正在通过 Bridge 请求打开面板: filmId: %s", filmId)));

        return ServerRequestBridge.request(player, CLIENT_OPEN_FILM_PANEL, buf -> {
            buf.writeString(filmId);
        });
    }

    // 请求客户端打开指定的 replayEditor 面板的逻辑
    public static CompletableFuture<Boolean> requestClientOpenReplayEditorPacket(ServerPlayerEntity player,
            String filmId, int replayIndex) {
        player.sendMessage(
                Text.literal(String.format("§c[BBSMCP Server] 正在通过 Bridge 请求打开回放编辑器: filmId: %s, replayIndex: %d",
                        filmId, replayIndex)));

        return ServerRequestBridge.request(player, CLIENT_OPEN_REPLAY_EDITOR, buf -> {
            buf.writeString(filmId);
            buf.writeInt(replayIndex);
        });
    }

    // 请求客户端关闭当前的任何 UI 面板 (相当于按 ESC)
    public static CompletableFuture<Boolean> requestClientCloseUIPacket(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§c[BBSMCP Server] 正在通过 Bridge 请求关闭客户端 UI"));

        return ServerRequestBridge.request(player, CLIENT_CLOSE_UI, buf -> {
        });
    }

    // 请求客户端立刻执行 Film 落盘保存
    public static CompletableFuture<Boolean> requestClientSaveFilmPacket(ServerPlayerEntity player, String filmId) {
        player.sendMessage(
                Text.literal(String.format("§c[BBSMCP Server] 正在通过 Bridge 请求客户端落盘保存影片: filmId: %s", filmId)));

        return ServerRequestBridge.request(player, CLIENT_SAVE_FILM, buf -> {
            buf.writeString(filmId);
        });
    }

    // 请求客户端发送 film data 的逻辑
    public static void requestClientFilmDataPacket(ServerPlayerEntity player, String filmId) {
        try {
            player.sendMessage(Text
                    .literal(
                            String.format("§c[BBSMCP Server] 已发送 requestClientFilmDataPacket 数据包: filmId: %s",
                                    filmId)));

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(filmId);

            ServerPlayNetworking.send(player, REQUEST_CLIENT_FILM_DATA, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 处理来自客户端的 film data 的逻辑
    public static void handleRequestClientFilmDataPacket(ServerPlayerEntity player, PacketByteBuf buf) {
        crusher.receive(buf, (bytes, packetByteBuf) -> {
            try {
                String filmId = packetByteBuf.readString();
                MapType filmData = (MapType) DataStorageUtils.readFromBytes(bytes);

                FilmManagerAPI.INSTANCE.getFilm(filmId).fromData(filmData);
                player.sendMessage(Text
                        .literal(String.format(
                                "§c[BBSMCP Server] 已接收 RequestClientFilmDataPacket 数据包并完成客户端 Film 同步: filmId: %s",
                                filmId)));
            } catch (Exception e) {
                e.printStackTrace();
            }

        });
    }

    // 发送 film data 到客户端的逻辑
    public static void sendServerFilmDataPacket(ServerPlayerEntity player, String filmId, MapType filmData) {
        try {
            player.sendMessage(Text
                    .literal(String.format("§c[BBSMCP Server] 已发送 ServerFilmDataPacket 数据包: filmId: %s", filmId)));

            crusher.send(player, SERVER_FILM_DATA, filmData, (packetByteBuf) -> {
                packetByteBuf.writeString(filmId);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendPickClipPacket(ServerPlayerEntity player, String filmId, int clipIndex) {
        try {
            player.sendMessage(Text.literal(String
                    .format("§c[BBSMCP Server] 已发送 PickClipPacket 数据包: filmId: %s clipIndex: %d", filmId, clipIndex)));

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(filmId);
            buf.writeInt(clipIndex);

            ServerPlayNetworking.send(player, CLIENT_PICK_CLIP, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CompletableFuture<Boolean> requestClientTogglePlaybackPacket(ServerPlayerEntity player,
            String filmId) {
        player.sendMessage(Text.literal(String
                .format("§c[BBSMCP Server] 已发送 requestClientTogglePlaybackPacket 数据包: filmId: %s", filmId)));

        return ServerRequestBridge.request(player, CLIENT_TOGGLE_PLAYBACK, buf -> {
            buf.writeString(filmId);
        });
    }

    public static CompletableFuture<Boolean> requestClientSetCursorPacket(ServerPlayerEntity player, String filmId,
            int tick) {
        return ServerRequestBridge.request(player, CLIENT_SET_CURSOR, buf -> {
            buf.writeString(filmId);
            buf.writeInt(tick);
        });
    }

    public static CompletableFuture<String> requestClientGetCursorPacket(ServerPlayerEntity player, String filmId) {
        return ServerRequestBridge.requestData(player, CLIENT_GET_CURSOR, buf -> {
            buf.writeString(filmId);
        });
    }
}
