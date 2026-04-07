package theblocklab.bbsmcp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import theblocklab.bbsmcp.anchor.AnchorClientEvent;
import theblocklab.bbsmcp.anchor.AnchorClientNetwork;
import theblocklab.bbsmcp.anchor.AnchorClientRenderer;
import theblocklab.bbsmcp.dev.DevEnvironmentSetup;
import theblocklab.bbsmcp.network.ClientNetwork;
import theblocklab.bbsmcp.utils.CaptureHelper;

public class BBSMCPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 注册客户端网络相关
		ClientNetwork.setup();

		// 注册锚点相关
		AnchorClientNetwork.setup();
		AnchorClientRenderer.register();
		AnchorClientEvent.register();

		// 注册截图助手
		ClientTickEvents.END_CLIENT_TICK.register(CaptureHelper::onClientTick);

		DevEnvironmentSetup.register();

		BBSMCP.LOGGER.info("BBS MCP 客户端初始化完成!");
	}
}