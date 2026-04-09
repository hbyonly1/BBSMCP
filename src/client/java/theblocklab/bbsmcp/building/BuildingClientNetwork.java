package theblocklab.bbsmcp.building;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import theblocklab.bbsmcp.network.ServerNetwork;

/**
 * 客户端建筑系统网络处理。
 * 注册 S2C 接收器：S2C_BUILDING_PREVIEW 和 S2C_BUILDING_CLEAR。
 */
@Environment(EnvType.CLIENT)
public class BuildingClientNetwork {

    public static void setup() {
        // 接收蓝图预览数据，构建 BuildingBlueprint 并激活虚影渲染
        ClientPlayNetworking.registerGlobalReceiver(
            ServerNetwork.S2C_BUILDING_PREVIEW,
            (client, handler, buf, responseSender) -> {
                String name = buf.readString();
                String json = buf.readString();
                client.execute(() -> {
                    try {
                        BuildingBlueprint blueprint = new BuildingBlueprint(json);
                        BuildingClientRepository.current     = blueprint;
                        BuildingClientRepository.currentName = name;
                        BuildingClientRepository.rotation    = 0; // 重置旋转
                    } catch (Exception e) {
                        System.err.println("[BBSMCP Building] 蓝图解析失败: " + e.getMessage());
                    }
                });
            }
        );

        // 接收清除指令，移除虚影
        ClientPlayNetworking.registerGlobalReceiver(
            ServerNetwork.S2C_BUILDING_CLEAR,
            (client, handler, buf, responseSender) -> {
                client.execute(BuildingClientRepository::clear);
            }
        );
    }
}
