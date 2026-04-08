package theblocklab.bbsmcp.film.clips.utils;

import theblocklab.bbsmcp.BBSMCP;
import theblocklab.bbsmcp.film.FilmManagerAPI;
import theblocklab.bbsmcp.film.clips.ClipManagerAPI;
import mchorse.bbs_mod.data.DataParser;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.utils.clips.Clip;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 负责从 JSON 文件加载镜头到 Film
 */
public class ClipFileLoader {

    private static final Path CLIPS_DIR_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("bbsmcp/clips");

    // 记录最近加载的文件路径, 为方便测试，默认为 example.json
    private static String lastLoadedFilepath = "example.json";

    /**
     * 从 JSON 文件加载镜头
     * 
     * @param filmId   Film ID
     * @param filepath 文件路径（相对于 .minecraft/config/bbsmcp/clips/）
     * @param player   玩家
     */
    public static void loadClipsFromFile(ServerPlayerEntity player, String filmId, String filepath) {
        try {
            // 解析文件路径
            Path fullPath = resolveFilePath(filepath);

            // 检查文件是否存在
            if (!Files.exists(fullPath)) {
                player.sendMessage(Text.literal(
                        String.format("§c[BBSMCP ClipFileLoader] 文件不存在: %s", fullPath.toString())));
                return;
            }

            // 读取文件内容
            String fileContent = Files.readString(fullPath);

            // 解析 JSON
            BaseType data = DataParser.parse(fileContent);
            if (data == null || !data.isMap()) {
                player.sendMessage(Text.literal("§c[BBSMCP ClipFileLoader] JSON 格式错误: 根对象必须是 {}"));
                return;
            }

            MapType root = data.asMap();
            if (!root.has("clips")) {
                player.sendMessage(Text.literal("§c[BBSMCP ClipFileLoader] JSON 格式错误: 缺少 'clips' 数组"));
                return;
            }

            ListType clipsList = root.getList("clips");
            if (clipsList == null) {
                player.sendMessage(Text.literal("§c[BBSMCP ClipFileLoader] JSON 格式错误: 'clips' 必须是数组"));
                return;
            }

            // 获取 Film
            Film film = FilmManagerAPI.INSTANCE.getFilm(filmId);

            int successCount = 0;
            int warningCount = 0;

            // 遍历处理每个 clip
            for (int i = 0; i < clipsList.size(); i++) {
                try {
                    MapType clipData = clipsList.getMap(i);

                    int index = clipData.getInt("index");
                    String type = clipData.getString("type");

                    // 类型冲突检测
                    boolean hasTypeConflict = checkTypeConflict(film, index, type, player);
                    if (hasTypeConflict) {
                        warningCount++;
                    }

                    // 将 MapType 转换为 JSON 字符串
                    String clipJson = clipData.toString();

                    // 调用 ClipManagerAPI 添加镜头（index 和 type 从 clipJson 内部读取）
                    ClipManagerAPI.addClip(player, filmId, clipJson);
                    successCount++;

                } catch (Exception e) {
                    player.sendMessage(Text.literal(
                            String.format("§c[BBSMCP ClipFileLoader] 处理 Clip #%d 时出错: %s", i, e.getMessage())));
                    BBSMCP.LOGGER.error("处理 Clip 时出错", e);
                }
                // 记录最近加载的文件路径
                lastLoadedFilepath = filepath;
            }

            FilmManagerAPI.pushFilmS2C(player, filmId, (MapType) film.toData());

            // 选中最后一个添加的镜头
            // if (successCount > 0) {
            // ClipManagerAPI.pickLastClip(filmId, player);
            // }

            // 发送总结消息
            player.sendMessage(Text.literal(
                    String.format("§a[BBSMCP ClipFileLoader] 成功处理 %d 个镜头%s",
                            successCount,
                            warningCount > 0 ? String.format("，其中 %d 个有类型冲突警告", warningCount) : "")));

        } catch (Exception e) {
            player.sendMessage(Text.literal("§c[BBSMCP ClipFileLoader] 加载文件失败: " + e.getMessage()));
            BBSMCP.LOGGER.error("加载 Clip 文件失败", e);
        }
    }

    /**
     * 获取最近加载的文件路径
     * 
     * @return 最近加载的文件路径，如果没有加载过则返回 null
     */
    public static String getLastLoadedFilepath() {
        return lastLoadedFilepath;
    }

    /**
     * 解析文件路径
     * 
     * @param filepath 相对路径
     * @return 完整路径
     */
    public static Path resolveFilePath(String filepath) throws Exception {
        // 如果基础目录不存在，创建它
        if (!Files.exists(CLIPS_DIR_PATH)) {
            Files.createDirectories(CLIPS_DIR_PATH);
        }

        // 解析相对路径
        Path fullPath = CLIPS_DIR_PATH.resolve(filepath);

        return fullPath;
    }

    /**
     * 创建示例 JSON 文件
     * 在 Mod 初始化时调用，如果示例文件不存在则创建
     */
    public static void createExampleFile() throws Exception {
        Path exampleFilePath = resolveFilePath("example.json");

        // 如果文件已存在，不覆盖
        if (Files.exists(exampleFilePath)) {
            return;
        }

        // 从资源文件读取示例内容
        try (var inputStream = ClipFileLoader.class.getResourceAsStream("/assets/bbsmcp/example.json")) {
            if (inputStream == null) {
                BBSMCP.LOGGER.warn("无法找到示例文件资源");
                return;
            }

            // 读取资源文件内容
            byte[] content = inputStream.readAllBytes();

            // 写入到配置目录
            Files.write(exampleFilePath, content);

            BBSMCP.LOGGER.info("已创建示例文件: " + exampleFilePath.toString());
        }
    }

    /**
     * 检查类型冲突
     * 
     * @param film   Film 对象
     * @param index  镜头索引
     * @param type   新镜头类型
     * @param player 玩家
     * @return 是否有类型冲突
     */
    public static boolean checkTypeConflict(Film film, int index, String type, ServerPlayerEntity player) {
        if (index >= 0 && index < film.camera.get().size()) {
            Clip clip = film.camera.get(index);

            // 获取现有 Clip 的类名，例如 "IdleClip" -> "idle"
            String className = clip.getClass().getSimpleName();
            String currentType = className.replace("Clip", "").toLowerCase();

            if (!type.equals(currentType)) {
                player.sendMessage(Text.literal(
                        String.format("§e[BBSMCP ClipFileLoader 警告] Index %d 的镜头类型不一致: 现有类型=%s, 新类型=%s, 将尝试更新",
                                index, currentType, type)));
                return true;
            }
        }
        return false;
    }

}
