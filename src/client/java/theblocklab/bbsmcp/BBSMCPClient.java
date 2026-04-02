package theblocklab.bbsmcp;

import net.fabricmc.api.ClientModInitializer;
import theblocklab.bbsmcp.anchor.AnchorClientEvent;
import theblocklab.bbsmcp.anchor.AnchorClientNetwork;
import theblocklab.bbsmcp.anchor.AnchorRenderer;
import theblocklab.bbsmcp.dev.DevEnvironmentSetup;
import theblocklab.bbsmcp.network.ClientNetwork;

public class BBSMCPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// 注册客户端网络接收器
		ClientNetwork.setup();

		// 注册锚点相关
		AnchorClientNetwork.setup();
		AnchorRenderer.register();
		AnchorClientEvent.register();

		DevEnvironmentSetup.register();

		BBSMCP.LOGGER.info("BBS MCP 客户端初始化完成!");
	}
}