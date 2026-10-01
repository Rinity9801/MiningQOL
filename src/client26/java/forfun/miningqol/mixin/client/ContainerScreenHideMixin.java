package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CheatHooks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips drawing a container GUI while a cheat feature asks for it (e.g. the comm-claim
 * "Hide GUI" toggle mid-claim). The screen stays open and interactive — only the visuals
 * are suppressed. Legit builds leave the hook null, so this never fires.
 */
@Mixin(AbstractContainerScreen.class)
public class ContainerScreenHideMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"), cancellable = true)
    private void miningqol$hideDuringClaim(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Custom commissions menu GUI: the NanoVG pass draws the replacement each frame.
        if (forfun.miningqol.client.CommissionGui.renderReplacing(
                (net.minecraft.client.gui.screens.Screen) (Object) this, mouseX, mouseY)) {
            ci.cancel();
            return;
        }
        if (CheatHooks.hideContainerGui != null && CheatHooks.hideContainerGui.getAsBoolean()) {
            ci.cancel();
            return;
        }
        // Cheat replacement visuals (e.g. Auto Forge mid-craft status card).
        if (CheatHooks.containerGuiOverlay != null
                && CheatHooks.containerGuiOverlay.renderReplacing(
                    (net.minecraft.client.gui.screens.Screen) (Object) this, ctx, mouseX, mouseY)) {
            ci.cancel();
        }
    }

    /** The item/slot passes are invoked separately from extractRenderState — hide them too. */
    @Inject(method = "extractContents(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"), cancellable = true)
    private void miningqol$hideContents(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (miningqol$hideAll()) ci.cancel();
    }

    @Inject(method = "extractCarriedItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            at = @At("HEAD"), cancellable = true)
    private void miningqol$hideCarried(GuiGraphicsExtractor ctx, int mouseX, int mouseY, CallbackInfo ci) {
        if (miningqol$hideAll()) ci.cancel();
    }

    @Inject(method = "extractSnapbackItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            at = @At("HEAD"), cancellable = true)
    private void miningqol$hideSnapback(GuiGraphicsExtractor ctx, CallbackInfo ci) {
        if (miningqol$hideAll()) ci.cancel();
    }

    private boolean miningqol$hideAll() {
        if (forfun.miningqol.client.CommissionGui.isReplacing(
                (net.minecraft.client.gui.screens.Screen) (Object) this)) {
            return true;
        }
        return CheatHooks.hideContainerGui != null && CheatHooks.hideContainerGui.getAsBoolean();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("RETURN"))
    private void miningqol$overlayOnTop(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Cheat overlay drawn on top of the vanilla visuals (e.g. Auto Forge side picker).
        if (CheatHooks.containerGuiOverlay != null) {
            CheatHooks.containerGuiOverlay.renderOnTop(
                (net.minecraft.client.gui.screens.Screen) (Object) this, ctx, mouseX, mouseY);
        }
    }
}
