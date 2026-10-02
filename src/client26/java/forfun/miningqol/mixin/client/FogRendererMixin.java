package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CustomFog;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Custom fog. {@code setupFog} builds the frame's {@link FogData} — the start and end of the fog
 * for terrain, sky and clouds, and its colour — and the game uploads it and clears the sky with
 * the colour only after it returns, so changing it on the way out is all it takes. The shader blends
 * by the colour's alpha, which makes alpha the fog's strength.
 *
 * <p>Water, lava and powder snow keep their own fog: it is only replaced when the camera is in air.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void miningqol$customFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float darken,
                                     ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (!CustomFog.isEnabled() || camera.getFluidInCamera() != FogType.NONE) return;
        FogData data = cir.getReturnValue();
        float start = Math.min(CustomFog.getStart(), CustomFog.getEnd());
        float end = Math.max(CustomFog.getEnd(), start + 0.1f);
        data.environmentalStart = start;
        data.environmentalEnd = end;
        if (CustomFog.isFogSky()) {
            data.skyEnd = end;
            data.cloudEnd = end;
        }
        float[] c = CustomFog.getColor();
        data.color.set(c[0], c[1], c[2], CustomFog.getAlpha());
    }
}
