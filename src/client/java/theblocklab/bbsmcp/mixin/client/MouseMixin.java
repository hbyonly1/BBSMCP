package theblocklab.bbsmcp.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import theblocklab.bbsmcp.building.BuildingWandItem;
import theblocklab.bbsmcp.building.BuildingClientRepository;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && !client.player.isSpectator() && client.currentScreen == null) {
            ItemStack stack = client.player.getMainHandStack();
            if (!stack.isEmpty() && stack.getItem() instanceof BuildingWandItem) {
                if (!BuildingClientRepository.isEmpty()) {
                    // Update preview distance
                    double delta = vertical > 0 ? 1.0 : -1.0;
                    if (client.player.isSneaking()) {
                        delta *= 0.1; // Fine adjustment
                    }
                    
                    BuildingClientRepository.previewDistance += delta;
                    if (BuildingClientRepository.previewDistance < 2.0) {
                        BuildingClientRepository.previewDistance = 2.0;
                    }
                    if (BuildingClientRepository.previewDistance > 64.0) {
                        BuildingClientRepository.previewDistance = 64.0;
                    }
                    
                    client.player.sendMessage(Text.literal(String.format("§a[建筑] 预览距离: %.1f 格", BuildingClientRepository.previewDistance)), true);
                    
                    ci.cancel(); // Prevent hotbar scrolling
                }
            }
        }
    }
}
