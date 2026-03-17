package theblocklab.bbsmcp;

import theblocklab.bbsmcp.network.ClientNetwork;
import net.fabricmc.api.ClientModInitializer;


public class BBSMCPClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as
		// rendering.
		ClientNetwork.setup();
		BBSMCP.LOGGER.info("BBS MCP 客户端初始化完成!");
	}
}