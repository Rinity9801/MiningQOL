package forfun.miningqol.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlassSync {
    private static final Logger LOGGER = LoggerFactory.getLogger("GlassSync");
    private static boolean enabled = false;

    public static BlockPos latestGemBreaking = null;

    public static void setEnabled(boolean enabled) {
        GlassSync.enabled = enabled;
        LOGGER.info("GlassSync " + (enabled ? "enabled" : "disabled"));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean posEquals(BlockPos pos1, BlockPos pos2) {
        if (pos1 == null || pos2 == null) return false;
        return pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
    }

    public static boolean isDisconnectedPane(BlockState state, Direction toIgnore) {
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (direction == toIgnore) continue;
            if (state.get(PaneBlock.FACING_PROPERTIES.get(direction))) {
                return false;
            }
        }
        return true;
    }

    public static BlockState createFullConnectedPane(BlockState state) {
        BlockState newState = state;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            newState = newState.with(PaneBlock.FACING_PROPERTIES.get(direction), true);
        }
        return newState;
    }
}
