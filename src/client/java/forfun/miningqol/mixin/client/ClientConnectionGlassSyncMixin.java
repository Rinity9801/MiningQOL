package forfun.miningqol.mixin.client;

import forfun.miningqol.client.GlassSync;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionGlassSyncMixin {
    @Unique
    private static BlockPos lastUpdatedToFullConnectedPane = null;

    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        if (!GlassSync.isEnabled()) return;
        if (GlassSync.latestGemBreaking == null) return;

        if (packet instanceof BlockUpdateS2CPacket blockUpdatePacket) {
            if (GlassSync.posEquals(GlassSync.latestGemBreaking, blockUpdatePacket.getPos())) {
                BlockState newState = blockUpdatePacket.getState();
                if (newState.isAir()) {
                    for (Direction direction : Direction.Type.HORIZONTAL) {
                        BlockPos neighborPos = blockUpdatePacket.getPos().offset(direction);
                        BlockState neighborState = MinecraftClient.getInstance().world.getBlockState(neighborPos);
                        if (neighborState.getBlock() instanceof PaneBlock) {
                            var nProperty = PaneBlock.FACING_PROPERTIES.get(direction.getOpposite());

                            MinecraftClient.getInstance().execute(() -> {
                                var nState = neighborState.with(nProperty, false);
                                if (GlassSync.isDisconnectedPane(nState, direction.getOpposite())) {
                                    lastUpdatedToFullConnectedPane = neighborPos;
                                    MinecraftClient.getInstance().world.setBlockState(neighborPos, GlassSync.createFullConnectedPane(neighborState));
                                } else {
                                    if (!neighborPos.equals(lastUpdatedToFullConnectedPane)) {
                                        MinecraftClient.getInstance().world.setBlockState(neighborPos, nState);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}
