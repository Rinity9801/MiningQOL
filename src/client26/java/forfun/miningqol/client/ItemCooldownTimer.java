package forfun.miningqol.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * A right-click ability cooldown shown as a one-line HUD with the Pickaxe Cooldown's options:
 * right clicking while holding the item starts the timer (not while one is already running,
 * since the ability can't fire then either), and the line reads "Name: 12s" until "✔ Ready".
 */
public final class ItemCooldownTimer {
    /** Rogue Sword's speed ability. */
    public static final ItemCooldownTimer ROGUE_SWORD = new ItemCooldownTimer(
        "rogue_sword", "Rogue Sword", 30, 10, 76, name -> name.contains("rogue sword"));
    /** Weird Tuba and Weirder Tuba. */
    public static final ItemCooldownTimer TUBA = new ItemCooldownTimer(
        "tuba", "Tuba", 20, 10, 90, name -> name.contains("weird tuba") || name.contains("weirder tuba"));

    public static final List<ItemCooldownTimer> ALL = List.of(ROGUE_SWORD, TUBA);

    public final String label;
    private final String id;
    private final int defaultSeconds;
    private final Predicate<String> matchesName;
    private final HudAnchor anchor;

    private boolean enabled;
    private long endsAt;
    private boolean useWasDown;
    private boolean hideWithF1 = false;
    /** 0 left, 1 centre, 2 right, within the HUD box. */
    private int textAlign = 1;
    private boolean secondsSuffix = true;
    /** Hide the name, show just the timer. */
    private boolean cooldownOnly = false;
    private int cooldownSeconds;
    private float scale = 1.0f;
    private boolean titleEnabled = false;
    private int titleThreshold = 3;
    private int lastTitleSecond = -1;
    private final float[] cooldownLabelColor = {1.0f, 170.0f / 255.0f, 0.0f};
    private final float[] cooldownValueColor = {1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
    private final float[] readyLabelColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
    private final float[] readyValueColor = {0.0f, 170.0f / 255.0f, 0.0f};

    private ItemCooldownTimer(String id, String label, int seconds, int defaultX, int defaultY, Predicate<String> matchesName) {
        this.id = id;
        this.label = label;
        this.defaultSeconds = seconds;
        this.cooldownSeconds = seconds;
        this.matchesName = matchesName;
        this.anchor = new HudAnchor(defaultX, defaultY, this::getWidth, this::getHeight);
    }

    public static void registerAll() {
        for (ItemCooldownTimer timer : ALL) {
            HudElementRegistry.attachElementBefore(VanillaHudElements.SLEEP,
                Identifier.fromNamespaceAndPath("miningqol", timer.id + "_cooldown_hud"),
                (context, tickCounter) -> timer.render(context));
        }
    }

    public static void tickAll(Minecraft client) {
        for (ItemCooldownTimer timer : ALL) timer.tick(client);
    }

    /**
     * Watches the use key rather than item-use events: Hypixel abilities fire on any right
     * click, at air or at a block, and only the key sees both the same way.
     */
    private void tick(Minecraft client) {
        boolean down = client.options.keyUse.isDown();
        boolean pressed = down && !useWasDown;
        useWasDown = down;
        if (!enabled || client.player == null) {
            endsAt = 0;
            return;
        }
        if (!pressed || client.screen != null || secondsLeft() > 0) return;
        ItemStack held = client.player.getMainHandItem();
        if (held.isEmpty()) return;
        String name = held.getHoverName().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT);
        if (matchesName.test(name)) endsAt = System.currentTimeMillis() + cooldownSeconds * 1000L;
    }

    private void render(GuiGraphicsExtractor context) {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (hideWithF1 && client.options.hideGui) return;
        int left = secondsLeft();
        if (titleEnabled && left > 0 && left <= titleThreshold) {
            if (left != lastTitleSecond) {
                client.gui.setTimes(0, 15, 3);
                client.gui.setTitle(Component.literal(""));
                client.gui.setSubtitle(formatText(secs(left), false, true));
                lastTitleSecond = left;
            }
        } else {
            lastTitleSecond = -1;
        }
        draw(context, formatText(left > 0 ? secs(left) : "✔ Ready", left <= 0, false));
    }

    /** Draws a line at the anchor, scaled and aligned within the HUD box. */
    private void draw(GuiGraphicsExtractor context, Component line) {
        Minecraft client = Minecraft.getInstance();
        int slack = unscaledWidth() - client.font.width(line);
        int offset = textAlign == 0 ? 0 : textAlign == 1 ? slack / 2 : slack;
        context.pose().pushMatrix();
        context.pose().translate(anchor.x(), anchor.y());
        context.pose().scale(scale, scale);
        context.text(client.font, line, offset, 0, 0xFFFFFFFF, true);
        context.pose().popMatrix();
    }

    /** The Move HUDs preview, drawn exactly as the live HUD would be. */
    public void drawPreview(GuiGraphicsExtractor context) {
        draw(context, getPreviewText());
    }

    private String secs(int value) {
        return secondsSuffix ? value + "s" : String.valueOf(value);
    }

    private int secondsLeft() {
        long remaining = endsAt - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    public Component getPreviewText() {
        return formatText(secs(defaultSeconds), false, false);
    }

    private Component formatText(String value, boolean ready, boolean boldValue) {
        float[] labelColor = ready ? readyLabelColor : cooldownLabelColor;
        float[] valueColor = ready ? readyValueColor : cooldownValueColor;
        MutableComponent valueText = Component.literal(value)
            .setStyle(Style.EMPTY.withColor(toRgb(valueColor)).withBold(boldValue));
        if (cooldownOnly) return valueText;
        MutableComponent text = Component.literal(label + ": ").setStyle(Style.EMPTY.withColor(toRgb(labelColor)));
        return text.append(valueText);
    }

    private static int toRgb(float[] color) {
        return (Math.round(color[0] * 255.0f) << 16) | (Math.round(color[1] * 255.0f) << 8) | Math.round(color[2] * 255.0f);
    }

    private static void setColor(float[] color, float red, float green, float blue) {
        color[0] = Math.max(0.0f, Math.min(1.0f, red));
        color[1] = Math.max(0.0f, Math.min(1.0f, green));
        color[2] = Math.max(0.0f, Math.min(1.0f, blue));
    }

    // ---- settings ----

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        enabled = value;
        if (!enabled) endsAt = 0;
    }

    public HudAnchor anchor() { return anchor; }
    public int getX() { return anchor.x(); }
    public int getY() { return anchor.y(); }
    public void setPosition(int x, int y) { anchor.set(x, y); }

    /** The widest line this HUD can draw, unscaled: a three-digit count or "✔ Ready". */
    private int unscaledWidth() {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return 100;
        return Math.max(12, Math.max(
            client.font.width(formatText(secs(Math.max(100, cooldownSeconds)), false, false)),
            client.font.width(formatText("✔ Ready", true, false))));
    }

    /** Measured from the text actually drawn, so the mover box hugs it. */
    public int getWidth() {
        return Math.round(unscaledWidth() * scale);
    }

    public int getHeight() {
        Minecraft client = Minecraft.getInstance();
        return Math.round((client.font == null ? 10 : client.font.lineHeight) * scale);
    }

    public boolean isHideWithF1() { return hideWithF1; }
    public void setHideWithF1(boolean value) { hideWithF1 = value; }
    public int getTextAlign() { return textAlign; }
    public void setTextAlign(int value) { textAlign = Math.max(0, Math.min(2, value)); }
    public boolean isSecondsSuffix() { return secondsSuffix; }
    public void setSecondsSuffix(boolean value) { secondsSuffix = value; }
    public boolean isCooldownOnly() { return cooldownOnly; }
    public void setCooldownOnly(boolean value) { cooldownOnly = value; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int value) { cooldownSeconds = Math.max(1, Math.min(600, value)); }
    public float getScale() { return scale; }
    public void setScale(float value) { scale = Math.max(0.5f, Math.min(3.0f, value)); }
    public boolean isTitleEnabled() { return titleEnabled; }
    public void setTitleEnabled(boolean value) { titleEnabled = value; }
    public int getTitleThreshold() { return titleThreshold; }
    public void setTitleThreshold(int value) { titleThreshold = Math.max(0, Math.min(30, value)); }

    public float[] getCooldownLabelColor() { return cooldownLabelColor.clone(); }
    public void setCooldownLabelColor(float r, float g, float b) { setColor(cooldownLabelColor, r, g, b); }
    public float[] getCooldownValueColor() { return cooldownValueColor.clone(); }
    public void setCooldownValueColor(float r, float g, float b) { setColor(cooldownValueColor, r, g, b); }
    public float[] getReadyLabelColor() { return readyLabelColor.clone(); }
    public void setReadyLabelColor(float r, float g, float b) { setColor(readyLabelColor, r, g, b); }
    public float[] getReadyValueColor() { return readyValueColor.clone(); }
    public void setReadyValueColor(float r, float g, float b) { setColor(readyValueColor, r, g, b); }

    /** Everything the config keeps for one timer. */
    public static final class Saved {
        public boolean enabled = false;
        public int x = 10;
        public int y = 76;
        public int anchorModeX = -1;
        public int anchorOffX = 0;
        public int anchorModeY = -1;
        public int anchorOffY = 0;
        public float[] cooldownLabelColor = {1.0f, 170.0f / 255.0f, 0.0f};
        public float[] cooldownValueColor = {1.0f, 85.0f / 255.0f, 85.0f / 255.0f};
        public float[] readyLabelColor = {85.0f / 255.0f, 1.0f, 85.0f / 255.0f};
        public float[] readyValueColor = {0.0f, 170.0f / 255.0f, 0.0f};
        public boolean hideWithF1 = false;
        public int textAlign = 1;
        public boolean secondsSuffix = true;
        public boolean cooldownOnly = false;
        /** 0 = the item's own cooldown. */
        public int cooldownSeconds = 0;
        public float scale = 1.0f;
        public boolean titleEnabled = false;
        public int titleThreshold = 3;
    }

    public Saved save() {
        Saved s = new Saved();
        s.enabled = enabled;
        s.x = getX();
        s.y = getY();
        s.anchorModeX = anchor.modeX();
        s.anchorOffX = anchor.offX();
        s.anchorModeY = anchor.modeY();
        s.anchorOffY = anchor.offY();
        s.cooldownLabelColor = getCooldownLabelColor();
        s.cooldownValueColor = getCooldownValueColor();
        s.readyLabelColor = getReadyLabelColor();
        s.readyValueColor = getReadyValueColor();
        s.hideWithF1 = hideWithF1;
        s.textAlign = textAlign;
        s.secondsSuffix = secondsSuffix;
        s.cooldownOnly = cooldownOnly;
        s.cooldownSeconds = cooldownSeconds;
        s.scale = scale;
        s.titleEnabled = titleEnabled;
        s.titleThreshold = titleThreshold;
        return s;
    }

    /** {@code modeX/modeY} already passed through the config's anchor-version check. */
    public void load(Saved s, int modeX, int modeY) {
        if (s == null) return;
        setPosition(s.x, s.y);
        anchor.load(modeX, s.anchorOffX, modeY, s.anchorOffY);
        setEnabled(s.enabled);
        if (s.cooldownLabelColor != null && s.cooldownLabelColor.length >= 3) setColor(cooldownLabelColor, s.cooldownLabelColor[0], s.cooldownLabelColor[1], s.cooldownLabelColor[2]);
        if (s.cooldownValueColor != null && s.cooldownValueColor.length >= 3) setColor(cooldownValueColor, s.cooldownValueColor[0], s.cooldownValueColor[1], s.cooldownValueColor[2]);
        if (s.readyLabelColor != null && s.readyLabelColor.length >= 3) setColor(readyLabelColor, s.readyLabelColor[0], s.readyLabelColor[1], s.readyLabelColor[2]);
        if (s.readyValueColor != null && s.readyValueColor.length >= 3) setColor(readyValueColor, s.readyValueColor[0], s.readyValueColor[1], s.readyValueColor[2]);
        setHideWithF1(s.hideWithF1);
        setTextAlign(s.textAlign);
        setSecondsSuffix(s.secondsSuffix);
        setCooldownOnly(s.cooldownOnly);
        setCooldownSeconds(s.cooldownSeconds > 0 ? s.cooldownSeconds : defaultSeconds);
        setScale(s.scale > 0 ? s.scale : 1.0f);
        setTitleEnabled(s.titleEnabled);
        setTitleThreshold(s.titleThreshold);
    }
}
