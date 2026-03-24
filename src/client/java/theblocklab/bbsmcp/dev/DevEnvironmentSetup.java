package theblocklab.bbsmcp.dev;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.text.Text;

/**
 * 开发环境专属组件：自动挂机与鼠标焦点释放。
 * 仅在 isDevelopmentEnvironment() 为 true（即 VSCode / IDEA 运行源码）时生效。
 */
@Environment(EnvType.CLIENT)
public class DevEnvironmentSetup {

    private static boolean hasAutoPaused = false;

    public static void register() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                // 等待玩家成功加载进入单人世界，并且确保当前没有打开任何 UI
                if (!hasAutoPaused && client.player != null && client.world != null && client.currentScreen == null) {
                    hasAutoPaused = true;
                    // 模拟玩家按下了 ESC，弹出暂停菜单并释放鼠标捕获
                    client.setScreen(new GameMenuScreen(true));

                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§a[BBSMCP Dev] 检测到自动登录，已自动通过 ESC 挂起并释放鼠标焦点！"), false);
                    }
                }
            });
        }
    }
}
