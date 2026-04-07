package theblocklab.bbsmcp.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.ScreenshotRecorder;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端截图助手 (Refined)
 * 负责监控 Film 播放进度并在指定 Tick 触发截图，附带电影 ID 标识。
 */
public class CaptureHelper {
    public static int targetTick = -1;
    public static String currentFilmId = "";
    public static CompletableFuture<String> currentTask = null;

    /**
     * 启动截图任务
     * @param target 目标截图 tick
     * @param start 可选的起始 tick (-1 表示不设置)
     */
    public static CompletableFuture<String> startCaptureTask(int target, int start) {
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
            reset();
        }
    }

    private static void reset() {
        targetTick = -1;
        currentFilmId = "";
        currentTask = null;
    }

    private static void captureBbsFrame() {
        try {
            ScreenshotRecorder recorder = BBSModClient.getScreenshotRecorder();
            Texture texture = BBSRendering.getTexture();
            
            if (texture != null) {
                // 生成标识化文件名：bbs_[filmId]_t[tick]_[yyyyMMdd_HHmmss].png
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                String filename = String.format("bbs_%s_t%d_%s.png", currentFilmId, targetTick, timestamp);
                
                // 通过 BBS 默认截图文件推导其所在的截图文件夹
                File folder = recorder.getScreenshotFile().getParentFile();
                File screenshotFile = new File(folder, filename);
                
                recorder.takeScreenshot(screenshotFile, texture.id, texture.width, texture.height);
                
                // 返回绝对路径
                currentTask.complete(screenshotFile.getAbsolutePath());
                System.out.println("BBS 自动截图已保存: " + screenshotFile.getAbsolutePath());
            } else {
                currentTask.completeExceptionally(new RuntimeException("无法获取画面纹理 (Texture is null)"));
            }
        } catch (Exception e) {
            currentTask.completeExceptionally(e);
        }
    }
}
