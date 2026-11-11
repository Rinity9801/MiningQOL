package forfun.miningqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * Right-clicks powder chests for you while you're breaking blocks.
 *
 * <p>Vanilla won't use an item while a block is being destroyed ({@code startUseItem} returns early
 * while {@code isDestroying()}), so with toggle-attack on, or the button held, right-clicking a powder
 * chest does nothing until break is let go. With this on, once the crosshair lands on a powder chest
 * while break is down, it:
 * <ol>
 *   <li>waits Release Delay, then lets go of break (a toggle-attack press, or a release in hold mode);</li>
 *   <li>waits Click Delay, then right-clicks the chest, once;</li>
 *   <li>once you look off the chest, waits Resume Delay and turns break back on.</li>
 * </ol>
 * The steps run per rendered frame, so the delays land in ms rather than on the next 50ms tick.
 *
 * <p>Powder chests are told apart from decorative ones by how they arrive: the server places them with
 * a block update while you mine, while ordinary chests come in with the chunk.
 */
public final class PowderChestClickManager {
    /** Spawned chests are forgotten after this; powder chests despawn well before it. */
    private static final long CHEST_TTL_MS = 5 * 60_000L;

    private enum Phase {
        /** Waiting for the crosshair to land on a powder chest while break is down. */
        IDLE,
        /** On a chest, waiting Release Delay before letting go of break. */
        RELEASE_WAIT,
        /** Break let go, waiting Click Delay before the right-click. */
        CLICK_WAIT,
        /** Clicked (or the player took over), waiting for the crosshair to leave the chest. */
        LOOKING,
        /** Off the chest, waiting Resume Delay before break goes back on. */
        RESUME_WAIT
    }

    private static boolean enabled = false;
    private static boolean showToggleMessage = true;
    private static int releaseDelayMs = 50;
    private static int clickDelayMs = 75;
    private static int resumeDelayMs = 100;

    /** Chest positions placed by a block update, with when they appeared. */
    private static final Map<BlockPos, Long> spawnedChests = new HashMap<>();
    private static ClientLevel trackedLevel = null;

    private static Phase phase = Phase.IDLE;
    private static BlockPos chestPos = null;
    private static long dueAt = 0L;
    /** True while break is off because we let go of it, so resuming is ours to do. */
    private static boolean releasedBreak = false;

    private PowderChestClickManager() {}

    // --- settings -------------------------------------------------------------------------

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (!enabled) abort(Minecraft.getInstance());
    }

    public static void toggle() {
        setEnabled(!enabled);
        MiningqolClient.saveConfig();
        if (!showToggleMessage) return;
        MqoChat.log(Component.literal(enabled
            ? "§6[MQO] §aPowder Chest Click enabled"
            : "§6[MQO] §cPowder Chest Click disabled"));
    }

    public static boolean isShowToggleMessage() {
        return showToggleMessage;
    }

    public static void setShowToggleMessage(boolean value) {
        showToggleMessage = value;
    }

    public static int getReleaseDelayMs() {
        return releaseDelayMs;
    }

    public static void setReleaseDelayMs(int ms) {
        releaseDelayMs = Math.max(0, ms);
    }

    public static int getClickDelayMs() {
        return clickDelayMs;
    }

    public static void setClickDelayMs(int ms) {
        clickDelayMs = Math.max(0, ms);
    }

    public static int getResumeDelayMs() {
        return resumeDelayMs;
    }

    public static void setResumeDelayMs(int ms) {
        resumeDelayMs = Math.max(0, ms);
    }

    /**
     * True while break is held off for a chest. The mod's own clickers check this so they don't
     * press attack straight back down under us.
     */
    public static boolean isSuppressingBreak() {
        return releasedBreak && phase != Phase.IDLE;
    }

    // --- chest tracking -------------------------------------------------------------------

    /** Every server block change, read before it applies (from BlockUpdateMixin via CheatHooks). */
    public static void onBlockChange(BlockPos pos, BlockState oldState, BlockState newState) {
        boolean wasChest = oldState.is(Blocks.CHEST);
        boolean isChest = newState.is(Blocks.CHEST);
        if (isChest && !wasChest) {
            spawnedChests.put(pos.immutable(), System.currentTimeMillis());
        } else if (wasChest && !isChest) {
            spawnedChests.remove(pos);
        }
    }

    private static boolean isPowderChest(ClientLevel level, BlockPos pos, long now) {
        Long spawnedAt = spawnedChests.get(pos);
        if (spawnedAt == null) return false;
        if (now - spawnedAt > CHEST_TTL_MS || !level.getBlockState(pos).is(Blocks.CHEST)) {
            spawnedChests.remove(pos);
            return false;
        }
        return true;
    }

    // --- driving --------------------------------------------------------------------------

    /** End of every client tick. */
    public static void tick() {
        step();
    }

    /** Every rendered frame, so the ms delays aren't rounded up to the next tick. */
    public static void frame() {
        step();
    }

    private static void step() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != trackedLevel) {
            // New world or lobby: the old positions mean nothing here.
            spawnedChests.clear();
            trackedLevel = level;
            resetState();
        }
        if (!enabled || level == null || mc.player == null || mc.gameMode == null) return;

        long now = System.currentTimeMillis();
        KeyMapping attack = mc.options.keyAttack;
        // A chest menu (or any screen) pauses everything; nothing resumes until it closes.
        boolean screenOpen = mc.screen != null;
        BlockPos aimed = aimedBlock(mc);

        switch (phase) {
            case IDLE -> {
                if (screenOpen || !attack.isDown() || aimed == null || !isPowderChest(level, aimed, now)) return;
                chestPos = aimed;
                releasedBreak = false;
                phase = Phase.RELEASE_WAIT;
                dueAt = now + releaseDelayMs;
                if (releaseDelayMs == 0) step();
            }
            case RELEASE_WAIT -> {
                if (!chestPos.equals(aimed)) {
                    // Looked away before we did anything — nothing to undo.
                    resetState();
                    return;
                }
                if (screenOpen || now < dueAt) return;
                if (attack.isDown()) {
                    setBreak(mc, false);
                    // Stop the dig now rather than on the next tick, or the right-click is refused.
                    mc.gameMode.stopDestroyBlock();
                    releasedBreak = true;
                }
                phase = Phase.CLICK_WAIT;
                dueAt = now + clickDelayMs;
                if (clickDelayMs == 0) step();
            }
            case CLICK_WAIT -> {
                if (!chestPos.equals(aimed)) {
                    // Looked off before the click: leave the chest for next time and resume.
                    phase = Phase.RESUME_WAIT;
                    dueAt = now + resumeDelayMs;
                    return;
                }
                if (attack.isDown()) {
                    // The player turned break back on themselves; they're in charge now.
                    releasedBreak = false;
                    phase = Phase.LOOKING;
                    return;
                }
                if (screenOpen || now < dueAt) return;
                rightClick(mc, chestPos);
                spawnedChests.remove(chestPos); // once per chest
                phase = Phase.LOOKING;
            }
            case LOOKING -> {
                if (attack.isDown()) releasedBreak = false;
                if (screenOpen || chestPos.equals(aimed)) return;
                phase = Phase.RESUME_WAIT;
                dueAt = now + resumeDelayMs;
            }
            case RESUME_WAIT -> {
                if (attack.isDown()) {
                    // Break is already back on (the player clicked): nothing left to restore.
                    resetState();
                    return;
                }
                if (screenOpen) return;
                if (chestPos.equals(aimed)) {
                    phase = Phase.LOOKING;
                    return;
                }
                if (aimed != null && isPowderChest(level, aimed, now)) {
                    // Straight onto another chest with break still off: go right to its click.
                    chestPos = aimed;
                    phase = Phase.CLICK_WAIT;
                    dueAt = now + clickDelayMs;
                    return;
                }
                if (now < dueAt) return;
                if (releasedBreak) setBreak(mc, true);
                resetState();
            }
        }
    }

    /** The block under the crosshair, or null when it's on an entity or nothing in reach. */
    private static BlockPos aimedBlock(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) return null;
        return blockHit.getBlockPos();
    }

    /** Right-clicks the aimed chest the way vanilla's startUseItem does for a block. */
    private static void rightClick(Minecraft mc, BlockPos pos) {
        if (!(mc.hitResult instanceof BlockHitResult blockHit) || !blockHit.getBlockPos().equals(pos)) return;
        if (mc.player.isHandsBusy()) return;
        if (mc.gameMode.isDestroying()) mc.gameMode.stopDestroyBlock();
        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHit);
        if (result instanceof InteractionResult.Success success
                && success.swingSource() == InteractionResult.SwingSource.CLIENT) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * Turns break on or off. With toggle-attack on, a press flips the latch, so it presses once when
     * the state needs to change. In hold mode it only goes back down if the button is still held, the
     * same as if the player had never been interrupted.
     */
    private static void setBreak(Minecraft mc, boolean down) {
        KeyMapping attack = mc.options.keyAttack;
        if (attack.isDown() == down) return;
        if (mc.options.toggleAttack().get()) {
            attack.setDown(true);
        } else if (!down || isPhysicallyHeld(mc, attack)) {
            attack.setDown(down);
        }
    }

    private static boolean isPhysicallyHeld(Minecraft mc, KeyMapping mapping) {
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
        return switch (key.getType()) {
            case MOUSE -> GLFW.glfwGetMouseButton(mc.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
            case KEYSYM -> InputConstants.isKeyDown(mc.getWindow(), key.getValue());
            default -> false;
        };
    }

    /** Turned off mid-sequence: put break back the way we found it. */
    private static void abort(Minecraft mc) {
        if (releasedBreak && phase != Phase.IDLE && mc != null && mc.player != null) setBreak(mc, true);
        resetState();
    }

    private static void resetState() {
        phase = Phase.IDLE;
        chestPos = null;
        dueAt = 0L;
        releasedBreak = false;
    }
}
