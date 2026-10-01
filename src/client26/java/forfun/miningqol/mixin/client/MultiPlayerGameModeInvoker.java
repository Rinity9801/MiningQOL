package forfun.miningqol.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Sends a hotbar change to the server now instead of on the next client tick, so a swap timed
 * between ticks (Comm Claim's pigeon) reaches the server when it happens on screen.
 */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeInvoker {
    @Invoker("ensureHasSentCarriedItem")
    void miningqol$ensureHasSentCarriedItem();
}
