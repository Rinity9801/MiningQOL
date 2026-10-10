package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class RollingMinerCooldown {
    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath("miningqol", "rolling_miner_cooldown_hud");
    private static final String TRIGGER_MESSAGE = "rolling miner granted you double drops!";

    private static boolean registered;
    private static boolean enabled;
    private static long cooldownEndsAt;
    // Same display options as the Pickaxe Cooldown HUD.
    private static boolean hideWithF1 = false;
    private static int textAlign = 0; // 0 left, 1 center, 2 right
    private static boolean secondsSuffix = true;
    private static boolean cooldownOnly = false;
    /** Rolling Miner is always timed locally from its chat line; this is how long it lasts. */
    private static int cooldownSeconds = 20;
    private static boolean titleEnabled = false;
    private static int titleThreshold = 3;
    private static long lastTitleSetTime = 0;
    private static int lastTitleCooldown = -1;
    private static final HudAnchor ANCHOR = new HudAnchor(10, 62, RollingMinerCooldown::getWidth, RollingMinerCooldown::getHeight);
    private static final float[] cooldownLabelColor = {1.0f, 170.0f / 255.0f, 0.0f};
    private static final float[] cooldownValueColor = {1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
    private static final float[] readyLabelColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    private static final float[] readyValueColor = {0.0f, 170.0f / 255.0f, 0.0f};

    private RollingMinerCooldown() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_ID,
            (context, tickCounter) -> render(context)
        );
    }

    public static void onGameMessage(String message) {
        if (enabled && message.toLowerCase(Locale.ROOT).contains(TRIGGER_MESSAGE)) {
            cooldownEndsAt = System.currentTimeMillis() + cooldownSeconds * 1000L;
            lastTitleCooldown = -1;
        }
    }

    public static void tick(Minecraft client) {
        if (!enabled || cooldownEndsAt == 0) {
            return;
        }
        if (client.player == null) {
            cooldownEndsAt = 0;
            return;
        }
        if (System.currentTimeMillis() >= cooldownEndsAt) {
            cooldownEndsAt = 0;
        }
    }

    public static void render(GuiGraphicsExtractor context) {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (hideWithF1 && client.options.hideGui) return;

        int secondsLeft = secondsLeft();
        boolean ready = secondsLeft <= 0;

        if (titleEnabled && !ready && secondsLeft <= titleThreshold) {
            long now = System.currentTimeMillis();
            if (lastTitleCooldown != secondsLeft || now - lastTitleSetTime > 500) {
                client.gui.setTimes(0, 15, 3);
                client.gui.setTitle(Component.literal(""));
                client.gui.setSubtitle(formatText(secs(secondsLeft), false, true));
                lastTitleSetTime = now;
                lastTitleCooldown = secondsLeft;
            }
        } else {
            lastTitleCooldown = -1;
        }

        Component line = formatText(ready ? "✔ Ready" : secs(secondsLeft), ready, false);
        context.text(client.font, line, alignedX(line), ANCHOR.y(), 0xFFFFFFFF, true);
    }

    /** Left edge for [text] once alignment is applied, within the widest line's box. */
    public static int alignedX(Component text) {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null || textAlign == 0) return ANCHOR.x();
        int slack = getWidth() - client.font.width(text);
        return ANCHOR.x() + (textAlign == 1 ? slack / 2 : slack);
    }

    public static boolean isHideWithF1() { return hideWithF1; }
    public static void setHideWithF1(boolean value) { hideWithF1 = value; }

    public static int getTextAlign() { return textAlign; }
    public static void setTextAlign(int value) { textAlign = Math.max(0, Math.min(2, value)); }

    public static boolean isSecondsSuffix() { return secondsSuffix; }
    public static void setSecondsSuffix(boolean value) { secondsSuffix = value; }

    public static boolean isCooldownOnly() { return cooldownOnly; }
    public static void setCooldownOnly(boolean value) { cooldownOnly = value; }

    public static int getCooldownSeconds() { return cooldownSeconds; }
    public static void setCooldownSeconds(int seconds) { cooldownSeconds = Math.max(1, Math.min(120, seconds)); }

    public static boolean isTitleEnabled() { return titleEnabled; }
    public static void setTitleEnabled(boolean value) { titleEnabled = value; }

    public static int getTitleThreshold() { return titleThreshold; }
    public static void setTitleThreshold(int threshold) { titleThreshold = threshold; }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) {
            cooldownEndsAt = 0;
        }
    }

    public static void setPosition(int x, int y) {
        ANCHOR.set(x, y);
    }

    /** Edge anchor for the config — see {@link HudAnchor}. */
    public static HudAnchor anchor() { return ANCHOR; }

    public static int getX() {
        return ANCHOR.x();
    }

    public static int getY() {
        return ANCHOR.y();
    }

    /** Width of the widest line this HUD can draw, so alignment and the mover box agree. */
    public static int getWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return 120;
        int ready = client.font.width(formatText("✔ Ready", true, false));
        int counting = client.font.width(formatText(secs(Math.max(10, cooldownSeconds)), false, false));
        return Math.max(12, Math.max(ready, counting));
    }

    public static int getHeight() {
        Minecraft client = Minecraft.getInstance();
        return client.font == null ? 10 : client.font.lineHeight;
    }

    public static float[] getCooldownLabelColor() {
        return cooldownLabelColor.clone();
    }

    public static void setCooldownLabelColor(float red, float green, float blue) {
        setColor(cooldownLabelColor, red, green, blue);
    }

    public static float[] getCooldownValueColor() {
        return cooldownValueColor.clone();
    }

    public static void setCooldownValueColor(float red, float green, float blue) {
        setColor(cooldownValueColor, red, green, blue);
    }

    public static float[] getReadyLabelColor() {
        return readyLabelColor.clone();
    }

    public static void setReadyLabelColor(float red, float green, float blue) {
        setColor(readyLabelColor, red, green, blue);
    }

    public static float[] getReadyValueColor() {
        return readyValueColor.clone();
    }

    public static void setReadyValueColor(float red, float green, float blue) {
        setColor(readyValueColor, red, green, blue);
    }

    public static Component getPreviewText() {
        return formatText("✔ Ready", true, false);
    }

    private static Component formatText(String value, boolean ready, boolean boldValue) {
        float[] labelColor = ready ? readyLabelColor : cooldownLabelColor;
        float[] valueColor = ready ? readyValueColor : cooldownValueColor;
        MutableComponent valueText = Component.literal(value)
            .setStyle(Style.EMPTY.withColor(toRgb(valueColor)).withBold(boldValue));
        if (cooldownOnly) return valueText;
        MutableComponent text = Component.literal("Rolling Miner: ")
            .setStyle(Style.EMPTY.withColor(toRgb(labelColor)));
        return text.append(valueText);
    }

    /** Formats a second count for display, honouring [secondsSuffix]. */
    private static String secs(int value) {
        return secondsSuffix ? value + "s" : String.valueOf(value);
    }

    private static int toRgb(float[] color) {
        int red = Math.round(color[0] * 255.0f);
        int green = Math.round(color[1] * 255.0f);
        int blue = Math.round(color[2] * 255.0f);
        return (red << 16) | (green << 8) | blue;
    }

    private static void setColor(float[] color, float red, float green, float blue) {
        color[0] = clampColor(red);
        color[1] = clampColor(green);
        color[2] = clampColor(blue);
    }

    private static float clampColor(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int secondsLeft() {
        long remaining = cooldownEndsAt - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }
}
