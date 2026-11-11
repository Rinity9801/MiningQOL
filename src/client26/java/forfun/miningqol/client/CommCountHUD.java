package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lifetime commission count on screen, in the vanilla font like the Pickaxe Cooldown HUD.
 * The total is CommTracker's (Current Comms / /commtrack set, plus every completion since);
 * an optional second line shows this session's count.
 */
public final class CommCountHUD {
    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath("miningqol", "comm_count_hud");
    private static final int LINE_HEIGHT = 10;

    private static boolean registered;
    private static boolean enabled = false;
    private static boolean hideWithF1 = false;
    private static int textAlign = 0; // 0 left, 1 center, 2 right
    private static boolean showSession = true;
    /** Hide the labels and draw the bare numbers. */
    private static boolean numberOnly = false;
    private static final float[] labelColor = {1.0f, 170.0f / 255.0f, 0.0f};
    private static final float[] valueColor = {1.0f, 1.0f, 1.0f};
    private static final HudAnchor ANCHOR = new HudAnchor(10, 74, CommCountHUD::getWidth, CommCountHUD::getHeight);

    private CommCountHUD() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_ID,
            (context, tickCounter) -> render(context)
        );
    }

    public static void render(GuiGraphicsExtractor context) {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (hideWithF1 && client.options.hideGui) return;
        drawLines(context, lines(CommTracker.getTotalCompleted(), CommTracker.getSessionCompleted()));
    }

    /** The HUD editor's preview, drawn at the live position. */
    public static void drawPreview(GuiGraphicsExtractor context) {
        drawLines(context, getPreviewLines());
    }

    private static void drawLines(GuiGraphicsExtractor context, List<Component> lines) {
        Minecraft client = Minecraft.getInstance();
        int y = ANCHOR.y();
        for (Component line : lines) {
            context.text(client.font, line, alignedX(line), y, 0xFFFFFFFF, true);
            y += LINE_HEIGHT;
        }
    }

    private static List<Component> lines(long total, int session) {
        List<Component> lines = new ArrayList<>(2);
        lines.add(line("Commissions: ", String.format(Locale.US, "%,d", total)));
        if (showSession) lines.add(line("Session: ", String.format(Locale.US, "%,d", session)));
        return lines;
    }

    public static List<Component> getPreviewLines() {
        return lines(Math.max(1234, CommTracker.getTotalCompleted()), Math.max(12, CommTracker.getSessionCompleted()));
    }

    private static Component line(String label, String value) {
        MutableComponent valueText = Component.literal(value).setStyle(Style.EMPTY.withColor(toRgb(valueColor)));
        if (numberOnly) return valueText;
        return Component.literal(label).setStyle(Style.EMPTY.withColor(toRgb(labelColor))).append(valueText);
    }

    /** Left edge for [text] once alignment is applied, within the widest line's box. */
    private static int alignedX(Component text) {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null || textAlign == 0) return ANCHOR.x();
        int slack = getWidth() - client.font.width(text);
        return ANCHOR.x() + (textAlign == 1 ? slack / 2 : slack);
    }

    /** Widest line it can draw, sampled with a 7-digit total so the box doesn't jump as it grows. */
    public static int getWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return 100;
        int width = 12;
        for (Component line : lines(Math.max(1_234_567, CommTracker.getTotalCompleted()), 9_999)) {
            width = Math.max(width, client.font.width(line));
        }
        return width;
    }

    public static int getHeight() {
        return (showSession ? 2 : 1) * LINE_HEIGHT - 1;
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static boolean isHideWithF1() { return hideWithF1; }
    public static void setHideWithF1(boolean value) { hideWithF1 = value; }

    public static int getTextAlign() { return textAlign; }
    public static void setTextAlign(int value) { textAlign = Math.max(0, Math.min(2, value)); }

    public static boolean isShowSession() { return showSession; }
    public static void setShowSession(boolean value) { showSession = value; }

    public static boolean isNumberOnly() { return numberOnly; }
    public static void setNumberOnly(boolean value) { numberOnly = value; }

    public static float[] getLabelColor() { return labelColor.clone(); }
    public static void setLabelColor(float red, float green, float blue) { setColor(labelColor, red, green, blue); }

    public static float[] getValueColor() { return valueColor.clone(); }
    public static void setValueColor(float red, float green, float blue) { setColor(valueColor, red, green, blue); }

    public static void setPosition(int x, int y) { ANCHOR.set(x, y); }

    /** Edge anchor for the config — see {@link HudAnchor}. */
    public static HudAnchor anchor() { return ANCHOR; }

    public static int getX() { return ANCHOR.x(); }
    public static int getY() { return ANCHOR.y(); }

    private static int toRgb(float[] color) {
        int red = Math.round(color[0] * 255.0f);
        int green = Math.round(color[1] * 255.0f);
        int blue = Math.round(color[2] * 255.0f);
        return (red << 16) | (green << 8) | blue;
    }

    private static void setColor(float[] color, float red, float green, float blue) {
        color[0] = Math.max(0.0f, Math.min(1.0f, red));
        color[1] = Math.max(0.0f, Math.min(1.0f, green));
        color[2] = Math.max(0.0f, Math.min(1.0f, blue));
    }
}
