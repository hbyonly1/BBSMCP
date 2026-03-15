package theblocklab.bbsmcp.network;

import theblocklab.bbsmcp.film.FilmManagerAPI;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.network.ServerPacketCrusher;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ServerNetwork {
    // Identifier 是数据包的实际名称，例如 SERVER_FILM_DATA 就是 server 端的 data

    public static final Identifier REQUEST_CLIENT_FILM_DATA = new Identifier("bbsmcp", "request_client_film_data");
    public static final Identifier CLIENT_FILM_DATA = new Identifier("bbsmcp", "client_film_data");

    public static final Identifier SERVER_FILM_DATA = new Identifier("bbsmcp", "server_film_data");
    public static final Identifier CLIENT_PICK_CLIP = new Identifier("bbsmcp", "client_pick_clip");

    // === AI building 相关标识 ===
    public static final Identifier API_KEY = new Identifier("bbsmcp", "api_key");
    public static final Identifier USER_INPUT = new Identifier("bbsmcp", "user_input");
    /**
     * 客户端到服务端: 生成建筑请求
     */
    public static final Identifier GENERATE_BUILDING_C2S = new Identifier("bbsmcp", "generate_building");
    /**
     * 服务端到客户端: 建筑生成进度
     */
    public static final Identifier BUILDING_PROGRESS_S2C = new Identifier("bbsmcp", "building_progress");

    private static ServerPacketCrusher crusher = new ServerPacketCrusher();

    public static void setup() {
        // 注册 server 接收器相关代码
        // === BBS 相关逻辑 ===
        ServerPlayNetworking.registerGlobalReceiver(ServerNetwork.CLIENT_FILM_DATA,
                (server, player, handler, buf, responseSender) -> {
                    handleRequestClientFilmDataPacket(player, buf);
                });

        // === AI building 相关逻辑 ===
        // 注册 AI 建筑生成请求处理器
        ServerPlayNetworking.registerGlobalReceiver(GENERATE_BUILDING_C2S,
                (server, player, handler, buf, responseSender) -> {
                    handleGenerateBuildingPacket(server, player, buf);
                });
    }

    // === AI building 相关逻辑 ===

    /**
     * 发送建筑生成进度到客户端
     */
    public static void sendBuildingProgress(ServerPlayerEntity player, int progress) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(progress);
            ServerPlayNetworking.send(player, BUILDING_PROGRESS_S2C, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleGenerateBuildingPacket(MinecraftServer server, ServerPlayerEntity player,
            PacketByteBuf buf) {
        String json = buf.readString();

        server.execute(() -> {
            try {
                // 解析建筑数据
                theblocklab.bbsmcp.building.BuildingData data = theblocklab.bbsmcp.building.BuildingData
                        .fromJson(json);

                // 验证数据
                if (!data.validate()) {
                    player.sendMessage(Text.literal("§c建筑数据包含无效方块!"));
                    return;
                }

                // 在玩家前方5格放置建筑
                net.minecraft.util.math.BlockPos origin = player.getBlockPos().add(5, 0, 0);

                // 放置建筑
                theblocklab.bbsmcp.building.BuildingPlacer.placeBuilding(
                        player.getServerWorld(),
                        origin,
                        data,
                        player);
            } catch (Exception e) {
                theblocklab.bbsmcp.BBSMCP.LOGGER.error("处理建筑生成请求失败", e);
                player.sendMessage(Text.literal("§c建筑生成失败: " + e.getMessage()));
            }
        });
    }

    // === BBS 相关逻辑 ===

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

                // if (FilmManagerAPI.INSTANCE.isEmpty()) {
                //     FilmManagerAPI.INSTANCE.createFilm(filmId);
                //     player.sendMessage(Text
                //             .literal(String.format("§c[BBSMCP Server] 没有可同步的 Film, 正在创建 filmId: %s", filmId)));
                // }

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
    public static void sendServerFilmDataPacket(ServerPlayerEntity player, String filmId) {
        try {
            player.sendMessage(Text
                    .literal(String.format("§c[BBSMCP Server] 已发送 ServerFilmDataPacket 数据包: filmId: %s", filmId)));

            Film film = BBSMod.getFilms().load(filmId);
            crusher.send(player, SERVER_FILM_DATA, film.toData(), (packetByteBuf) -> {
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
}
