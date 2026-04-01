package theblocklab.bbsmcp.anchor;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

/**
 * 服务端锚点交互事件（非网络包的部分）。
 */
public class AnchorServerEvent {

    public static void register() {
        // 拦截聊天消息（用于描述输入），交由 Network 处理
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            boolean intercepted = AnchorServerNetwork.handleChatInput(sender, message.getSignedContent());
            return !intercepted; // 如果拦截了，则返回 false 阻止广播
        });
    }
}
