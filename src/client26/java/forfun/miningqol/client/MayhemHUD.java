package forfun.miningqol.client;

import forfun.miningqol.client.party.MineshaftAutoParty;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows which Mineshaft Mayhem buff the current shaft rolled.
 *
 * <p>Hypixel announces the perk in two lines on entry:
 * <pre>
 * MAYHEM! You received a Mining Speed buff from your Mineshaft Mayhem perk!
 * Mayhem: Gain +200 [icon] Mining Speed.
 * </pre>
 * The first names the buff, the second carries the amount. Either alone is enough to show
 * something; the amount fills in when its line arrives. The buff lives as long as the shaft
 * does: it is dropped once the sidebar stops saying "Mineshaft" or the level is swapped out.
 */
public final class MayhemHUD {
    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath("miningqol", "mayhem_hud");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?%?)");
    /** Grace after the sidebar stops saying "Mineshaft" before the buff is dropped. */
    private static final long LEAVE_GRACE_MS = 3_000L;

    private enum Perk {
        ABILITY_COOLDOWN("Ability CD", "pickaxe ability", "-"),
        COLD_RESISTANCE("Cold Res", "cold resist", "+"),
        MINING_SPEED("Mining Speed", "mining speed", "+"),
        MINING_FORTUNE("Mining Fortune", "mining fortune", "+"),
        SUSPICIOUS_SCRAP("Sus Scrap", "suspicious scrap", "+");

        final String label;
        final String keyword;
        /** Sign shown when the chat line carries none: a cooldown reduction is a minus. */
        final String defaultSign;

        Perk(String label, String keyword, String defaultSign) {
            this.label = label;
            this.keyword = keyword;
            this.defaultSign = defaultSign;
        }

        static Perk match(String lowerCaseText) {
            for (Perk perk : values()) {
                if (lowerCaseText.contains(perk.keyword)) return perk;
            }
            return null;
        }
    }

    private static boolean registered;
    private static boolean enabled = false;
    private static boolean hideWithF1 = false;
    /** Draw "Mayhem: None" while no buff is active, rather than nothing. */
    private static boolean alwaysShow = false;

    private static final HudAnchor ANCHOR = new HudAnchor(10, 74, MayhemHUD::getWidth, MayhemHUD::getHeight);
    private static final float[] labelColor = {1.0f, 85.0f / 255.0f, 1.0f};
    private static final float[] valueColor = {1.0f, 1.0f, 85.0f / 255.0f};
    private static final float[] noneColor = {170.0f / 255.0f, 170.0f / 255.0f, 170.0f / 255.0f};

    /** Text drawn after "Mayhem: ", or null when no buff is active. */
    private static String buffText;
    private static Perk buffPerk;
    private static ClientLevel buffLevel;
    private static boolean seenInShaft;
    private static long leftShaftAt;

    private MayhemHUD() {}

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
        if (message == null) return;
        String clean = message.replaceAll("§.", "").trim();
        String lower = clean.toLowerCase(Locale.ROOT);

        if (lower.startsWith("mayhem!")) {
            Perk perk = Perk.match(lower);
            // A fresh shaft: wipe whatever the last one rolled, even if this perk is unknown.
            startBuff(perk, perk == null ? null : perk.label);
            return;
        }

        if (lower.startsWith("mayhem:")) {
            String detail = clean.substring("mayhem:".length()).trim();
            Perk perk = Perk.match(lower);
            if (perk == null) perk = buffPerk;

            Matcher amount = AMOUNT_PATTERN.matcher(detail);
            String text;
            if (perk != null && amount.find()) {
                String number = amount.group(1);
                boolean signed = number.startsWith("+") || number.startsWith("-");
                text = (signed ? "" : perk.defaultSign) + number + " " + perk.label;
            } else if (perk != null) {
                text = perk.label;
            } else {
                text = tidy(detail);
            }

            if (buffText == null || perk != buffPerk) {
                // Detail line without (or before) its announcement — still a new buff.
                startBuff(perk, text);
            } else {
                buffText = text;
            }
        }
    }

    private static void startBuff(Perk perk, String text) {
        buffPerk = perk;
        buffText = text == null ? "Unknown" : text;
        buffLevel = null;   // captured on the next tick, once the shaft's level is in
        seenInShaft = false;
        leftShaftAt = 0;
    }

    public static void clear() {
        buffPerk = null;
        buffText = null;
        buffLevel = null;
        seenInShaft = false;
        leftShaftAt = 0;
    }

    /** Strips the icon glyphs Hypixel puts in these lines and squashes the spacing they leave. */
    private static String tidy(String text) {
        String ascii = text.replaceAll("[^\\x20-\\x7E]", "");
        ascii = ascii.replaceAll("\\s+", " ").trim();
        // "Gain +200 Mining Speed." -> "+200 Mining Speed"
        if (ascii.regionMatches(true, 0, "gain ", 0, 5)) ascii = ascii.substring(5).trim();
        while (ascii.endsWith(".")) ascii = ascii.substring(0, ascii.length() - 1);
        return ascii.isEmpty() ? "Unknown" : ascii;
    }

    public static void tick() {
        if (buffText == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        if (buffLevel == null) {
            buffLevel = client.level;
        } else if (buffLevel != client.level) {
            // Server swap: the shaft — and its buff — are gone.
            clear();
            return;
        }

        boolean inShaft = MineshaftAutoParty.isInMineshaft();
        if (inShaft) {
            seenInShaft = true;
            leftShaftAt = 0;
        } else if (seenInShaft) {
            long now = System.currentTimeMillis();
            if (leftShaftAt == 0) {
                leftShaftAt = now;
            } else if (now - leftShaftAt >= LEAVE_GRACE_MS) {
                clear();
            }
        }
    }

    public static void render(GuiGraphicsExtractor ctx) {
        if (!enabled) return;
        if (buffText == null && !alwaysShow) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (hideWithF1 && client.options.hideGui) return;

        ctx.text(client.font, currentText(), ANCHOR.x(), ANCHOR.y(), 0xFFFFFFFF, true);
    }

    private static Component currentText() {
        if (buffText == null) {
            return formatText("None", noneColor);
        }
        return formatText(buffText, valueColor);
    }

    public static Component getPreviewText() {
        return formatText("+200 Mining Speed", valueColor);
    }

    private static Component formatText(String value, float[] color) {
        MutableComponent text = Component.literal("Mayhem: ")
            .setStyle(Style.EMPTY.withColor(toRgb(labelColor)));
        return text.append(Component.literal(value).setStyle(Style.EMPTY.withColor(toRgb(color))));
    }

    /** Whether a Mayhem buff is currently tracked. */
    public static boolean isBuffActive() {
        return buffText != null;
    }

    /** The tracked buff as drawn after "Mayhem: ", or null. */
    public static String getBuffText() {
        return buffText;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isHideWithF1() {
        return hideWithF1;
    }

    public static void setHideWithF1(boolean value) {
        hideWithF1 = value;
    }

    public static boolean isAlwaysShow() {
        return alwaysShow;
    }

    public static void setAlwaysShow(boolean value) {
        alwaysShow = value;
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

    /** Measured from the wider of the preview and the live line, so the mover box hugs it. */
    public static int getWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return 120;
        int width = client.font.width(getPreviewText());
        if (buffText != null) width = Math.max(width, client.font.width(currentText()));
        return Math.max(12, width);
    }

    public static int getHeight() {
        Minecraft client = Minecraft.getInstance();
        return client.font == null ? 10 : client.font.lineHeight;
    }

    public static float[] getLabelColor() {
        return labelColor.clone();
    }

    public static void setLabelColor(float red, float green, float blue) {
        setColor(labelColor, red, green, blue);
    }

    public static float[] getValueColor() {
        return valueColor.clone();
    }

    public static void setValueColor(float red, float green, float blue) {
        setColor(valueColor, red, green, blue);
    }

    public static float[] getNoneColor() {
        return noneColor.clone();
    }

    public static void setNoneColor(float red, float green, float blue) {
        setColor(noneColor, red, green, blue);
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
}
