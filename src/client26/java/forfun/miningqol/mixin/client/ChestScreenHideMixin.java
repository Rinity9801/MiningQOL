package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CheatHooks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The chest screen's background (texture + slot grid) is drawn from its own
 * {@code extractBackground} override, invoked separately from the render extraction that
 * {@link ContainerScreenHideMixin} cancels — so hiding a menu needs this cancelled too.
 */
@Mixin(ContainerScreen.class)
public class ChestScreenHideMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"), cancellable = true)
    private void miningqol$hideBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (forfun.miningqol.client.CommissionGui.isReplacing(
                (net.minecraft.client.gui.screens.Screen) (Object) this)) {
            ci.cancel();
            return;
        }
        if (CheatHooks.hideContainerGui != null && CheatHooks.hideContainerGui.getAsBoolean()) {
            ci.cancel();
        }
    }
}
