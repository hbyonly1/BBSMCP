package theblocklab.bbsmcp.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.network.ClientPacketCrusher;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.clips.Clip;

@Environment(EnvType.CLIENT)
public class ClientNetwork {
    private static ClientPacketCrusher crusher = new ClientPacketCrusher();

    public static void setup() {
        // 注册 client 接收器相关代码

        // === BBS 相关逻辑 ===
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.REQUEST_CLIENT_FILM_DATA,
                (client, handler, buf, responseSender) -> {
                    sendClientFilmDataPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.SERVER_FILM_DATA,
                (client, handler, buf, responseSender) -> {
                    handleServerFilmDataPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_PICK_CLIP,
                (client, handler, buf, responseSender) -> {
                    handleClientPickClipPacket(client, buf);
                });

        // === AI building 相关逻辑 ===
    }

    // === BBS 相关逻辑 ===

    // 接收要发送的 filmId，然后发送 film data 到服务器的逻辑
    public static void sendClientFilmDataPacket(MinecraftClient client, PacketByteBuf buf) {
        String filmId = buf.readString();
        try {
            MinecraftClient.getInstance().player.sendMessage(Text
                    .literal(String.format("§c[BBSMCP Client] 正在发送 sendClientFilmDataPacket 数据包: filmId: %s", filmId)));

            Film film = BBSMod.getFilms().load(filmId);
            crusher.send(MinecraftClient.getInstance().player, ServerNetwork.CLIENT_FILM_DATA, film.toData(),
                    (packetByteBuf) -> {
                        packetByteBuf.writeString(filmId);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleServerFilmDataPacket(MinecraftClient client, PacketByteBuf buf) {
        crusher.receive(buf, (bytes, packetByteBuf) -> {
            String filmId = packetByteBuf.readString();
            MapType filmData = (MapType) DataStorageUtils.readFromBytes(bytes);

            client.execute(() -> {
                try {
                    // 从 Packet 创建 Film 对象
                    Film film = new Film();
                    film.setId(filmId);
                    film.fromData(filmData);

                    // 获取 Dashboard 和 Film 面板
                    UIDashboard dashboard = BBSModClient.getDashboard();
                    UIFilmPanel filmPanel = dashboard.getPanels().getPanel(UIFilmPanel.class);

                    // 填充 Film 数据
                    filmPanel.fill(film);

                    // 打开 Dashboard 面板
                    // dashboard.setPanel(filmPanel);
                    // UIScreen.open(dashboard);
                    // filmPanel.open();

                    client.player.sendMessage(
                            Text.literal(
                                    String.format("§c[BBSMCP Client] 已接收 ServerFilmDataPacket 数据包并更新 UI: %s", filmId)));

                } catch (Exception e) {
                    e.printStackTrace();
                    client.player.sendMessage(
                            Text.literal(String.format("§c[BBSMCP Client] 接收 ServerFilmDataPacket 数据包并更新 UI: %s 失败",
                                    filmId)));
                }
            });
        });
    }

    private static void handleClientPickClipPacket(MinecraftClient client, PacketByteBuf buf) {
        String filmId = buf.readString();
        int clipIndex = buf.readInt();

        client.execute(() -> {
            try {
                client.player.sendMessage(Text.literal(String.format(
                        "§c[BBSMCP Client] 已接收 PickClipPacket 数据包: filmId: %s clipIndex: %d", filmId, clipIndex)));
                // 获取 UIDashboard 和 UIFilmPanel
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIFilmPanel filmPanel = (UIFilmPanel) dashboard.getPanels().getPanel(UIFilmPanel.class);

                // 检查当前打开的 Film 是否匹配
                Film currentFilm = filmPanel.getData();
                if (currentFilm == null || !currentFilm.getId().equals(filmId)) {
                    client.player.sendMessage(Text.literal(String.format(
                            "§c[BBSMCP Client] Film not loaded or ID mismatch, current film: %s, but filmID is: %s",
                            currentFilm.getId(), filmId)));
                    return;
                }

                // 获取 UIClipsPanel, UIClips, Clip 对象
                UIClipsPanel cameraEditor = filmPanel.cameraEditor;
                UIClips clips = cameraEditor.clips;
                Clip clip = currentFilm.camera.get(clipIndex);
                if (clip == null) {
                    client.player.sendMessage(Text.literal(String.format(
                            "§c[BBSMCP Client] Clip not found at index: %d", clipIndex)));
                    return;
                }

                // 调用 pickClip
                clips.pickClip(clip);
                client.player.sendMessage(
                        Text.literal(String.format("§c[BBSMCP Client] Successfully picked clip: %d", clipIndex)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
