package forfun.miningqol.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import forfun.miningqol.client.CustomFog;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom fog's "Fog the sky": skip the sky pass — dome, sun, moon and stars — so what shows is the
 * clear colour, which is the fog colour. The same way vanilla hides the sky in powder snow, lava,
 * or under blindness and darkness. Fogging the dome by distance does not work: it sits about 16
 * blocks overhead, so straight up it was barely fogged, and the sun, moon and stars take no fog.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererSkyMixin {

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void miningqol$fogTheSky(FrameGraphBuilder builder, CameraRenderState camera, GpuBufferSlice fog, CallbackInfo ci) {
        if (CustomFog.isEnabled() && CustomFog.isFogSky() && camera.fogType == FogType.NONE) ci.cancel();
    }
}
