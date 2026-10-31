package forfun.miningqol.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import forfun.miningqol.client.EntityGlowESP;
import forfun.miningqol.client.utils.render.OutlineHaloBlit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// World renderers moved to forfun.miningqol.client.WorldRenderHook (26.2 removed renderLevel).
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Final
    private RenderTarget entityOutlineTarget;

    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    private void miningqol$compositeCustomEspGlow(CallbackInfo ci) {
        if (!EntityGlowESP.consumeCustomGlowFrame() || entityOutlineTarget == null) return;
        OutlineHaloBlit.blit(
            entityOutlineTarget,
            Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView());
        ci.cancel();
    }
}
