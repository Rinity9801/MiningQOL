package forfun.miningqol.mixin.client;

import forfun.miningqol.client.SoundPitchFixer;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {

    private SoundInstance currentSound;

    @ModifyVariable(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private SoundInstance captureSound(SoundInstance sound) {
        currentSound = sound;
        return sound;
    }

    @ModifyVariable(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V",
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/sound/SoundInstance;getPitch()F"),
        ordinal = 0
    )
    private float modifyPitch(float pitch) {
        if (currentSound != null && SoundPitchFixer.shouldFixPitch(currentSound.getSound().getIdentifier())) {
            return 1.0f;
        }
        return pitch;
    }
}
