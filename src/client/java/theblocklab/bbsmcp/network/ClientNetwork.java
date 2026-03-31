package theblocklab.bbsmcp.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.List;

import com.google.gson.JsonObject;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.network.ClientPacketCrusher;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;
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
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_OPEN_FILM_PANEL,
                (client, handler, buf, responseSender) -> {
                    handleClientOpenFilmPanelPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_OPEN_REPLAY_EDITOR,
                (client, handler, buf, responseSender) -> {
                    handleClientOpenReplayEditorPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_CLOSE_UI,
                (client, handler, buf, responseSender) -> {
                    handleClientCloseUIPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_TOGGLE_PLAYBACK,
                (client, handler, buf, responseSender) -> {
                    handleClientTogglePlaybackPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_SAVE_FILM,
                (client, handler, buf, responseSender) -> {
                    handleClientSaveFilmPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_SET_CURSOR,
                (client, handler, buf, responseSender) -> {
                    handleClientSetCursorPacket(client, buf);
                });
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_GET_CURSOR,
                (client, handler, buf, responseSender) -> {
                    handleClientGetCursorPacket(client, buf);
                });

        // === AI building 相关逻辑 ===
    }

    // === utils ===
    private static void sendOK(int requestId) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(requestId);
            ClientPlayNetworking.send(ServerNetwork.OK, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendData(int requestId, String data) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(requestId);
            buf.writeString(data);
            ClientPlayNetworking.send(ServerNetwork.CLIENT_DATA_RESPONSE, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendError(int requestId, String message) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(requestId);
            buf.writeString(message);
            ClientPlayNetworking.send(ServerNetwork.CLIENT_ERROR, buf);
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(Text.literal("§c[BBSMCP Client 内部错误抛出] " + message));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private static void handleClientOpenFilmPanelPacket(MinecraftClient client, PacketByteBuf buf) {
        int requestId = buf.readInt();
        String filmId = buf.readString();
        client.execute(() -> {
            try {
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIScreen.open(dashboard);
                UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);
                filmPanel.fill(BBSMod.getFilms().load(filmId));

                dashboard.setPanel(filmPanel);
                filmPanel.overlay.close();
                client.player.sendMessage(Text
                        .literal(String.format("§c[BBSMCP Client] 已接收 OpenFilmPanelPacket 数据包并打开影片面板: filmId: %s",
                                filmId)));
                sendOK(requestId);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(requestId, "打开影片面板失败: " + e.getMessage());
            }
        });
    }

    private static void handleClientOpenReplayEditorPacket(MinecraftClient client, PacketByteBuf buf) {
        int requestId = buf.readInt();
        String filmId = buf.readString();
        int replayIndex = buf.readInt();
        client.execute(() -> {
            try {
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIScreen.open(dashboard);
                UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);
                filmPanel.fill(BBSMod.getFilms().load(filmId));

                dashboard.setPanel(filmPanel);
                filmPanel.overlay.close();

                // 打开回放编辑器子面板
                filmPanel.showPanel(filmPanel.replayEditor);

                // 选中指定的 Replay
                Film film = (Film) filmPanel.getData();
                if (film != null) {
                    List<Replay> replays = film.replays.getList();
                    if (replayIndex >= 0 && replayIndex < replays.size()) {
                        filmPanel.replayEditor.setReplay(replays.get(replayIndex));
                    }
                }

                client.player.sendMessage(Text
                        .literal(String.format(
                                "§c[BBSMCP Client] 已接收 OpenReplayEditorPacket 数据包并打开回放编辑器: filmId: %s, replayIndex: %d",
                                filmId, replayIndex)));
                sendOK(requestId);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(requestId, "打开回放编辑器失败: " + e.getMessage());
            }
        });
    }

    private static void handleClientTogglePlaybackPacket(MinecraftClient client, PacketByteBuf buf) {
        int requestId = buf.readInt();
        String filmId = buf.readString();
        client.execute(() -> {
            try {
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIScreen.open(dashboard);
                UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);
                filmPanel.fill(BBSMod.getFilms().load(filmId));
                dashboard.setPanel(filmPanel);
                filmPanel.overlay.close();

                // 开始播放
                filmPanel.togglePlayback();
                client.player.sendMessage(Text.literal(String.format(
                        "§c[BBSMCP Client] 已接收 handleClientTogglePlaybackPacket 数据包，开始播放影片: %s，正在等待播放结束...", filmId)));

                // 启动一个后台线程进行轮询，绝对不阻塞 Minecraft 主渲染线程
                new Thread(() -> {
                    try {
                        // 给主线程一点时间启动播放状态
                        Thread.sleep(100);

                        // 持续轮询：只要是播放状态，或者时间还没到总时长，就继续挂起
                        while (filmPanel.isRunning()
                                || filmPanel.getCursor() < ((Film) filmPanel.getData()).camera.calculateDuration()) {
                            Thread.sleep(50);
                        }

                        // 循环结束说明播放已经完全停止
                        client.execute(() -> {
                            if (client.player != null) {
                                client.player.sendMessage(Text.literal("§a[BBSMCP Client] 播放已完成，向服务端发送回调 (OK)"));
                            }
                            sendOK(requestId);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }, "BBSMCP-PlaybackMonitor").start();

            } catch (Exception e) {
                e.printStackTrace();
                sendError(requestId, "播放影片交互失败: " + e.getMessage());
            }
        });
    }

    private static void handleClientCloseUIPacket(MinecraftClient client, PacketByteBuf buf) {
        int requestId = buf.readInt();
        client.execute(() -> {
            try {
                // 等同于玩家按下了 ESC 键关掉当前 UI 面板
                client.setScreen(null);

                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§c[BBSMCP Client] 收到关闭命令，已关闭当前 UI 界面"));
                }
                sendOK(requestId);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(requestId, "尝试唤下 ESC 或清空界面时报错: " + e.getMessage());
            }
        });
    }

    private static void handleClientSaveFilmPacket(MinecraftClient client, PacketByteBuf buf) {
        int requestId = buf.readInt();
        String filmId = buf.readString();
        client.execute(() -> {
            try {
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);
                if (filmPanel == null || filmPanel.getData() == null) {
                    sendError(requestId, "客户端尚未打开影片，无法执行保存操作！");
                    return;
                }

                String openedFilmId = filmPanel.getData().getId();
                if (!openedFilmId.equals(filmId)) {
                    sendError(requestId, "客户端当前打开的影片 (" + openedFilmId + ") 与请求保存的影片 (" + filmId + ") 不匹配！");
                    return;
                }
                filmPanel.forceSave();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(String.format("§c[BBSMCP Client] 已成功保存影片并落盘: %s", filmId)));
                }
                sendOK(requestId);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(requestId, "强制保存文件过程发生异常: " + e.getMessage());
            }
        });
    }

    private static void handleClientSetCursorPacket(MinecraftClient client, PacketByteBuf buf) {
        int requestId = buf.readInt();
        String filmId = buf.readString();
        int tick = buf.readInt();
        client.execute(() -> {
            try {
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);
                if (filmPanel == null || filmPanel.getData() == null) {
                    sendError(requestId, "客户端尚未打开影片 " + filmId);
                    return;
                }
                String openedFilmId = filmPanel.getData().getId();
                if (!openedFilmId.equals(filmId)) {
                    sendError(requestId, "客户端当前打开的影片 (" + openedFilmId + ") 与请求操作的影片 (" + filmId + ") 不匹配！");
                    return;
                }

                filmPanel.setCursor(tick);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal(String.format("§c[BBSMCP Client] 已成功设置游标到: %d", tick)));
                }
                sendOK(requestId);
            } catch (Exception e) {
                e.printStackTrace();
                sendError(requestId, "设置游标失败: " + e.getMessage());
            }
        });
    }

    private static void handleClientGetCursorPacket(MinecraftClient client, PacketByteBuf buf) {
        int requestId = buf.readInt();
        String filmId = buf.readString();
        client.execute(() -> {
            try {
                UIDashboard dashboard = BBSModClient.getDashboard();
                UIFilmPanel filmPanel = dashboard.getPanel(UIFilmPanel.class);
                if (filmPanel == null || filmPanel.getData() == null) {
                    sendError(requestId, "客户端尚未打开影片 " + filmId);
                    return;
                }
                String openedFilmId = filmPanel.getData().getId();
                if (!openedFilmId.equals(filmId)) {
                    sendError(requestId, "客户端当前打开的影片 (" + openedFilmId + ") 与请求操作的影片 (" + filmId + ") 不匹配！");
                    return;
                }

                int tick = filmPanel.getCursor();
                JsonObject response = new JsonObject();
                response.addProperty("cursor", tick);

                sendData(requestId, response.toString());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(requestId, "获取游标失败: " + e.getMessage());
            }
        });
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
