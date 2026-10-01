package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Loops the Fusion Machine: clicks "Repeat Previous Fusion", then the lime terracotta
 * confirm button, and again as long as the GUI keeps offering them. Pressing Escape in
 * the fusion GUI turns it off and closes the menu as usual.
 */
public final class AutoFusionManager {
    private static final String REPEAT_BUTTON = "repeat previous fusion";
    private static final String CONFIRM_ITEM = "lime_terracotta";

    private static boolean enabled = false;
    private static int clickDelay = 4;
    private static int cooldown;
    private static int fusions;
    private static boolean active;

    private AutoFusionManager() {}

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        active = false;
        cooldown = 0;
        MqoChat.log(value ? "§6[Auto Fusion] §aEnabled." : "§6[Auto Fusion] §cDisabled.");
    }
    public static int getClickDelay() { return clickDelay; }
    public static void setClickDelay(int value) { clickDelay = Math.max(1, Math.min(20, value)); }
    public static int getFusions() { return fusions; }
    /** True while it is actually clicking through a fusion GUI. */
    public static boolean isActive() { return enabled && active; }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void tick() {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return;
        if (!(client.screen instanceof ContainerScreen screen)) {
            active = false;
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        int slots = Math.max(0, screen.getMenu().slots.size() - 36);
        int repeat = -1;
        int confirm = -1;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            if (repeat < 0 && cleanName(stack).contains(REPEAT_BUTTON)) repeat = i;
            if (confirm < 0 && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(CONFIRM_ITEM)) confirm = i;
        }
        int target = repeat >= 0 ? repeat : confirm;
        if (target < 0) return;
        active = true;
        client.gameMode.handleContainerInput(screen.getMenu().containerId, target, 0, ContainerInput.PICKUP, client.player);
        if (target == confirm && repeat < 0) fusions++;
        cooldown = clickDelay;
    }

    /** Whether Escape in this screen should switch the loop off. */
    public static boolean isFusionScreen(Screen screen) {
        if (!enabled || !(screen instanceof ContainerScreen container)) return false;
        if (active) return true;
        String title = container.getTitle().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT);
        return title.contains("fusion");
    }

    public static void onEscape() {
        setEnabled(false);
    }

    public static void cleanup() {
        active = false;
        cooldown = 0;
    }

    private static String cleanName(ItemStack stack) {
        return stack.getHoverName().getString().replaceAll("§.", "").trim().toLowerCase(Locale.ROOT);
    }
}
