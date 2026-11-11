package forfun.miningqol.mixin.client;

import forfun.miningqol.client.CheatHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block changes from the server, read at HEAD while the level still holds the old block. Each
 * handler runs once on the network thread, only to re-queue itself, hence the thread check.
 */
@Mixin(ClientPacketListener.class)
public class BlockUpdateMixin {
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void miningqol$onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != null && mc.isSameThread()) {
            miningqol$blockChange(packet.getPos(), level.getBlockState(packet.getPos()), packet.getBlockState());
        }
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"))
    private void miningqol$onSectionUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != null && mc.isSameThread()) {
            packet.runUpdates((pos, state) -> miningqol$blockChange(pos, level.getBlockState(pos), state));
        }
    }

    @Unique
    private static void miningqol$blockChange(BlockPos pos, BlockState oldState, BlockState newState) {
        if (CheatHooks.onBlockChange != null) {
            CheatHooks.onBlockChange.onBlockChange(pos, oldState, newState);
        }
    }
}
