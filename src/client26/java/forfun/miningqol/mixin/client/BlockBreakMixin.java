package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CommBlocksHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tells the commission block counter when you break a block; it reads what the block was first. */
@Mixin(MultiPlayerGameMode.class)
public class BlockBreakMixin {
    @Unique
    private BlockState miningqol$destroying;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void miningqol$beforeDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        miningqol$destroying = level == null ? null : level.getBlockState(pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void miningqol$afterDestroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = miningqol$destroying;
        miningqol$destroying = null;
        if (state != null && cir.getReturnValue()) {
            CommBlocksHUD.onOwnBreak(state);
        }
    }
}
