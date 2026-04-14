package theblocklab.bbsmcp.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;
import theblocklab.bbsmcp.network.ServerNetwork;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class RegionClientNetwork {

    private RegionClientNetwork() {}

    public static void setup() {
        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.S2C_REGION_PREVIEW, (client, handler, buf, responseSender) -> {
            String regionKey = buf.readString();
            String payload = buf.readString();
            client.execute(() -> {
                JsonArray array = JsonParser.parseString(payload).getAsJsonArray();
                List<RegionClientRepository.PreviewChange> changes = new ArrayList<>(array.size());
                array.forEach(elem -> {
                    var obj = elem.getAsJsonObject();
                    changes.add(new RegionClientRepository.PreviewChange(
                        new BlockPos(obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt()),
                        obj.get("before").getAsString(),
                        obj.get("after").getAsString()
                    ));
                });
                RegionClientRepository.setPreview(regionKey, changes);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ServerNetwork.S2C_REGION_CLEAR_PREVIEW, (client, handler, buf, responseSender) ->
            client.execute(RegionClientRepository::clearPreview)
        );
    }
}
