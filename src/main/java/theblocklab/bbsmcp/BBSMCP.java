package theblocklab.bbsmcp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import theblocklab.bbsmcp.mcp.MCPServerImpl;
import theblocklab.bbsmcp.mcp.tools.clip.ClipManagerMCPTools;
import theblocklab.bbsmcp.mcp.tools.film.FilmManagerMCPTools;
import theblocklab.bbsmcp.mcp.tools.test.TestMCPTools;
import theblocklab.bbsmcp.mcp.tools.ui.UIMCPTools;

public class BBSMCP implements ModInitializer {
	public static final String MOD_ID = "bbsmcp";
	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("BBS MCP 初始化中...");

		// 注册服务端网络接收器
		theblocklab.bbsmcp.network.ServerNetwork.setup();

		// 创建示例文件
		try {
			theblocklab.bbsmcp.film.clips.ClipFileLoader.createExampleFile();
			LOGGER.info("创建 clip 示例文件成功! ");
		} catch (Exception e) {
			LOGGER.warn("创建 clip 示例文件失败! ", e);
		}

		// 注册 Server 启动和关闭事件，管理 MCP 服务器生命周期
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// 将端口硬编码为 8000
			MCPServerImpl.INSTANCE.start(server, 8000);

			// 注册 MCP 工具
			MCPServerImpl.INSTANCE.getRouter().registerProvider(new FilmManagerMCPTools());
			MCPServerImpl.INSTANCE.getRouter().registerProvider(new ClipManagerMCPTools());
			MCPServerImpl.INSTANCE.getRouter().registerProvider(new UIMCPTools());
			MCPServerImpl.INSTANCE.getRouter().registerProvider(new TestMCPTools());

			LOGGER.info("BBS MCP Server Hook Registered");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			MCPServerImpl.INSTANCE.stop();
			LOGGER.info("BBS MCP Server Hook Unregistered");
		});

		LOGGER.info("BBS MCP 初始化完成！");
	}
}