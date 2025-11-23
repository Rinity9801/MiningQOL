package forfun.miningqol.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import forfun.miningqol.client.SoundPitchFixer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    @ModifyExpressionValue(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/sound/SoundInstance;getPitch()F"
        )
    )
    private float modifySoundPitch(float originalPitch, SoundInstance sound) {
        if (SoundPitchFixer.shouldFixPitch(sound.getSound().getIdentifier())) {
            return 1.0f;
        }
        return originalPitch;
    }
}
