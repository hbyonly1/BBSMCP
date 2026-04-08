package theblocklab.bbsmcp.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.ScreenshotRecorder;
import net.minecraft.client.MinecraftClient;

import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端截图助手 (Refined)
 * 负责监控 Film 播放进度并在指定 Tick 触发截图，附带电影 ID 标识。
 */
public class CaptureHelper {
    public static int targetTick = -1;
    public static String currentFilmId = "";
    public static String currentFilename = "";
    public static CompletableFuture<Void> currentTask = null;

    /**
     * 启动截图任务
     * @param target 目标截图 tick
     * @param start 可选的起始 tick (-1 表示不设置)
     */
    public static CompletableFuture<Void> startCaptureTask(String filename, int target, int start) {
        if (currentTask != null && !currentTask.isDone()) {
            return CompletableFuture.failedFuture(new RuntimeException("当前已有截图任务正在进行中！"));
        }

        UIDashboard dashboard = BBSModClient.getDashboard();
        UIFilmPanel filmPanel = dashboard != null ? dashboard.getPanel(UIFilmPanel.class) : null;

        // 仅在面板已打开的情况下工作
        if (filmPanel == null || filmPanel.getData() == null) {
            return CompletableFuture.failedFuture(new RuntimeException("未检测到打开的电影面板，请先打开 Film 编辑界面。"));
        }

        // 初始化任务状态
        targetTick = target;
        currentFilmId = filmPanel.getData().getId();
        currentFilename = filename;
        currentTask = new CompletableFuture<>();

        // 可选寻时
        if (start != -1 && filmPanel.getCursor() != start) {
            filmPanel.setCursor(start);
        }

        // 若未在播放则强制播放
        if (!filmPanel.isRunning()) {
            filmPanel.togglePlayback();
        }

        return currentTask;
    }

    /**
     * 由 MinecraftClient 每 Tick 调用一次
     */
    public static void onClientTick(MinecraftClient client) {
        if (targetTick == -1 || currentTask == null) {
            return;
        }

        UIDashboard dashboard = BBSModClient.getDashboard();
        UIFilmPanel filmPanel = dashboard != null ? dashboard.getPanel(UIFilmPanel.class) : null;

        // 如果面板中途被关闭
        if (filmPanel == null) {
            currentTask.completeExceptionally(new RuntimeException("截图过程中电影面板被意外关闭！"));
            reset();
            return;
        }

        int currentTick = filmPanel.getRunner().ticks;
        // 等待直到到达目标 tick (为了稳妥，考虑 currentTick 刚好等于 targetTick 或略超过)
        if (currentTick >= targetTick) {
            captureBbsFrame();
            
            // 截图完成后停止播放
            if (filmPanel.isRunning()) {
                filmPanel.togglePlayback();
            }
            
            reset();
        }
    }

    private static void reset() {
        targetTick = -1;
        currentFilmId = "";
        currentFilename = "";
        currentTask = null;
    }

    private static void captureBbsFrame() {
        try {
            ScreenshotRecorder recorder = BBSModClient.getScreenshotRecorder();
            Texture texture = BBSRendering.getTexture();
            
            if (texture != null) {
                // 使用服务端下发的文件名
                String filename = currentFilename.isEmpty() ? "bbs_unknown.png" : currentFilename;
                
                // 指定截图保存路径：config/bbsmcp/screenshot
                File folder = FabricLoader.getInstance().getConfigDir()
                        .resolve("bbsmcp")
                        .resolve("screenshot").toFile();
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                File screenshotFile = new File(folder, filename);
                
                recorder.takeScreenshot(screenshotFile, texture.id, texture.width, texture.height);
                
                System.out.println("BBS 自动截图已保存: " + screenshotFile.getAbsolutePath());
                // 返回成功
                currentTask.complete(null);
            } else {
                currentTask.completeExceptionally(new RuntimeException("无法获取画面纹理 (Texture is null)"));
            }
        } catch (Exception e) {
            currentTask.completeExceptionally(e);
        }
    }
}
