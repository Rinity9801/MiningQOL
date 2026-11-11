package forfun.miningqol.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sees every system chat line before other mods can cancel it. Chat filters in other
 * mods can swallow Hypixel lines, which then never reach the Fabric message event;
 * priority 900 runs ahead of a default-priority cancel on the same method.
 */
@Mixin(value = ClientPacketListener.class, priority = 900)
public class SystemChatMixin {
    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void miningqol$onSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        // The handler runs once on the network thread to re-dispatch and once on the
        // client thread; only count the client-thread pass.
        if (packet.overlay() || !Minecraft.getInstance().isSameThread()) return;
        String text = packet.content().getString();
        forfun.miningqol.client.CommTracker.onChatMessage(text);
        forfun.miningqol.client.CommissionHUD.onCommissionComplete(text);
        forfun.miningqol.client.MineshaftPortal.onChatMessage(text);
    }
}
