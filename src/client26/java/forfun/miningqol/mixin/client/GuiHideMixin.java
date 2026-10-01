package forfun.miningqol.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the whole in-game HUD (hotbar, health, every mod HUD element) while the custom
 * commissions menu GUI is replacing the Royal Pigeon screen — the grid floats over the
 * bare world, like Odin's terminals, with nothing else drawn.
 */
@Mixin(Gui.class)
public class GuiHideMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void miningqol$hideHudDuringCommGui(GuiGraphicsExtractor ctx, DeltaTracker delta, CallbackInfo ci) {
        if (forfun.miningqol.client.CommissionGui.isActive()) {
            ci.cancel();
        }
    }
}
