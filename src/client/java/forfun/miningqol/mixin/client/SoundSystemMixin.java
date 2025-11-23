package forfun.miningqol.mixin.client;

import forfun.miningqol.client.SoundPitchFixer;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundInstance.class)
public interface SoundSystemMixin {

    @Shadow
    net.minecraft.client.sound.Sound getSound();

    @Inject(method = "getPitch", at = @At("RETURN"), cancellable = true)
    default void fixPitch(CallbackInfoReturnable<Float> cir) {
        if (SoundPitchFixer.shouldFixPitch(this.getSound().getIdentifier())) {
            cir.setReturnValue(1.0f);
        }
    }
}
