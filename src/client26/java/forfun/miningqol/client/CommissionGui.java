package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Odin-terminal-style replacement for the Royal Pigeon commissions menu.
 *
 * <p>While enabled and a commissions menu is open, the vanilla container GUI is not drawn at all
 * ({@code ContainerScreenHideMixin} cancels its render) and this draws the menu the way Odin's
 * custom terminals do: a dark rounded panel holding one rounded square per menu item, laid out
 * exactly like the container's slots (filler panes skipped). Commissions are accent-tinted with
 * their progress on the square; claimable ones light up fully. Clicking a square clicks the real
 * slot; every other click is swallowed so the invisible vanilla slots cannot be hit. The screen
 * itself stays open and interactive, so ESC closes it as normal.
 */
public final class CommissionGui {
    private static boolean enabled = false;
    private static float scale = 1.0f;
    private static float buttonSize = 24f;
    private static boolean showProgress = true;
    /** Commission total + session count above the menu, custom GUI or Hypixel's own. */
    private static boolean statsInMenu = true;
    /**
     * The menu stats line sits centred above whichever menu is open until it is dragged in the
     * HUD editor; after that it stays where it was put, like any other HUD.
     */
    private static boolean statsMoved = false;
    private static int statsW = 120;
    private static int statsH = 10;
    private static final HudAnchor STATS_ANCHOR = new HudAnchor(0, 0, () -> statsW, () -> statsH);
    private static final float[] accentColor = {122f / 255f, 162f / 255f, 247f / 255f};
    /** Colour of a completed (claimable) commission's square. */
    private static final float[] doneColor = {122f / 255f, 162f / 255f, 247f / 255f};

    // Odin's terminal metrics, in gui-scaled units before [scale]; the square size is a setting.
    private static final float GAP = 6f;
    private static final float PAD = 10f;
    private static final int COLS = 9;

    /** Mouse in gui-scaled coords, recorded by the render-cancel mixin each frame. */
    private static int mouseX = -1;
    private static int mouseY = -1;

    /** {x, y, w, h, slotId} hit boxes in gui-scaled coords, rebuilt every frame. */
    private static final List<float[]> slotBoxes = new ArrayList<>();

    /**
     * Last drawn cell per slot. A PICKUP click "lifts" the item client-side until Hypixel
     * resyncs the slot, which made a clicked square blink out — while a slot is momentarily
     * empty its cached cell keeps drawing instead.
    */
    private static final java.util.Map<Integer, Cell> cellCache = new java.util.HashMap<>();
    private static int cacheContainerId = -1;

    private record Cell(int slotId, int row, int col, boolean commission, boolean claimable, double progress) {}

    private CommissionGui() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static float getScale() {
        return scale;
    }

    public static void setScale(float value) {
        scale = Math.max(0.5f, Math.min(2.0f, value));
    }

    public static float getButtonSize() {
        return buttonSize;
    }

    public static void setButtonSize(float value) {
        buttonSize = Math.max(12f, Math.min(48f, value));
    }

    public static boolean isShowProgress() {
        return showProgress;
    }

    public static void setShowProgress(boolean value) {
        showProgress = value;
    }

    public static boolean isStatsInMenu() {
        return statsInMenu;
    }

    public static void setStatsInMenu(boolean value) {
        statsInMenu = value;
    }

    /** A secondary toggle: only while the Commission Stats module itself is on. */
    private static boolean showStatsLine() {
        return statsInMenu && CommTracker.isModuleEnabled();
    }

    /** Whether the HUD editor should offer the menu stats line. */
    public static boolean isStatsLineShown() {
        return showStatsLine();
    }

    public static boolean isStatsMoved() { return statsMoved; }

    /** False puts the line back to its automatic spot above the menu. */
    public static void setStatsMoved(boolean value) { statsMoved = value; }

    public static HudAnchor statsAnchor() { return STATS_ANCHOR; }

    /** From the HUD editor: from now on the line stays here. */
    public static void setStatsPosition(int x, int y) {
        statsMoved = true;
        STATS_ANCHOR.set(x, y);
    }

    /** Top-left in gui units; until moved, where it lands above a full-size (6-row) menu. */
    public static int getStatsX() {
        if (statsMoved) return STATS_ANCHOR.x();
        return (Minecraft.getInstance().getWindow().getGuiScaledWidth() - statsW) / 2;
    }

    public static int getStatsY() {
        if (statsMoved) return STATS_ANCHOR.y();
        return (Minecraft.getInstance().getWindow().getGuiScaledHeight() - 222) / 2 - statsH - 4;
    }

    public static int getStatsWidth() { return statsW; }
    public static int getStatsHeight() { return statsH; }

    /**
     * Draws the stats line: at its saved spot once moved, otherwise centred on {@code autoCentreX}
     * at {@code autoY} (NVG pixels). Records its size for the editor's drag box.
     */
    private static void drawStatsLine(xyz.meowing.vexel.api.RenderAPI r, xyz.meowing.vexel.api.style.Font font,
                                      float f, float size, float autoCentreX, float autoY, float u) {
        String label = statsLabel();
        float tw = r.textWidth(label, size, font);
        statsW = Math.max(1, Math.round(tw / f));
        statsH = Math.max(1, Math.round(size / f) + 1);
        if (statsMoved) {
            centredLabel(r, font, label, STATS_ANCHOR.x() * f + tw / 2f, STATS_ANCHOR.y() * f, size, u);
        } else {
            centredLabel(r, font, label, autoCentreX, autoY, size, u);
        }
    }

    /** Preview + frame in the HUD editor, which has no menu open to sit above. */
    private static void renderStatsPreview(Minecraft mc) {
        xyz.meowing.vexel.api.RenderAPI r = xyz.meowing.vexel.Vexel.getRenderer();
        xyz.meowing.vexel.api.style.Font font = xyz.meowing.vexel.Vexel.getDefaultFont();
        float f = (float) mc.getWindow().getScreenWidth() / Math.max(1, mc.getWindow().getGuiScaledWidth());
        float size = 9f * f;
        float x = getStatsX() * f;
        float y = getStatsY() * f;
        String label = statsLabel();
        float tw = r.textWidth(label, size, font);
        statsW = Math.max(1, Math.round(tw / f));
        statsH = Math.max(1, Math.round(size / f) + 1);
        centredLabel(r, font, label, x + tw / 2f, y, size, f);
        r.hollowRect(x - 2f * f, y - 2f * f, tw + 4f * f, statsH * f + 4f * f, Math.max(1f, f), 0xFF88AAFF, 3f * f);
    }

    private static String statsLabel() {
        return String.format(Locale.US, "Total: %,d  |  Session: %,d",
            CommTracker.getTotalCompleted(), CommTracker.getSessionCompleted());
    }

    /** One shadowed, centred line of text in NVG pixels. */
    private static void centredLabel(xyz.meowing.vexel.api.RenderAPI r, xyz.meowing.vexel.api.style.Font font,
                                     String label, float centreX, float y, float size, float u) {
        float tw = r.textWidth(label, size, font);
        float x = centreX - tw / 2f;
        float o = Math.max(1f, u);
        r.text(label, x + o, y + o, size, 0xE0101018, font);
        r.text(label, x, y, size, 0xFFF3F4F6, font);
    }

    /**
     * The stats line over Hypixel's own Commissions menu, for when the custom GUI is off.
     * Vexel draws after the whole frame, so this sits on top of the dimmed backdrop. The
     * chest's position follows vanilla's centring (176 wide, 114 + 18 per row tall).
     */
    private static void renderVanillaMenuStats(Minecraft mc) {
        if (!showStatsLine() || !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen chest)
                || !CommissionHUD.isCommissionMenuOpen(mc)) {
            return;
        }
        xyz.meowing.vexel.api.RenderAPI r = xyz.meowing.vexel.Vexel.getRenderer();
        xyz.meowing.vexel.api.style.Font font = xyz.meowing.vexel.Vexel.getDefaultFont();
        float f = (float) mc.getWindow().getScreenWidth() / Math.max(1, mc.getWindow().getGuiScaledWidth());
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        int imageHeight = 114 + chest.getMenu().getRowCount() * 18;
        int topPos = (guiH - imageHeight) / 2;
        float size = 9f * f;
        drawStatsLine(r, font, f, size, guiW / 2f * f, topPos * f - size - 4f * f, f);
    }

    public static float[] getAccentColor() {
        return accentColor.clone();
    }

    public static void setAccentColor(float red, float green, float blue) {
        accentColor[0] = clamp01(red);
        accentColor[1] = clamp01(green);
        accentColor[2] = clamp01(blue);
    }

    public static float[] getDoneColor() {
        return doneColor.clone();
    }

    public static void setDoneColor(float red, float green, float blue) {
        doneColor[0] = clamp01(red);
        doneColor[1] = clamp01(green);
        doneColor[2] = clamp01(blue);
    }

    /** Whether the custom GUI is replacing the currently open screen. */
    public static boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return enabled
            && mc.player != null
            && mc.screen instanceof AbstractContainerScreen<?>
            && CommissionHUD.isCommissionMenuOpen(mc);
    }

    /**
     * Mixin seam — called at the head of the container screen's render extraction. Returning
     * true cancels the vanilla visuals for this frame; the mouse position is kept for hover.
     */
    public static boolean renderReplacing(Screen screen, int mx, int my) {
        if (!isReplacing(screen)) {
            return false;
        }
        mouseX = mx;
        mouseY = my;
        return true;
    }

    /** Whether this screen's vanilla visuals should be suppressed right now. */
    public static boolean isReplacing(Screen screen) {
        return isActive() && Minecraft.getInstance().screen == screen;
    }

    /**
     * Click interception (fabric screen events). Returns true when the click was consumed —
     * either sent to a menu slot or swallowed to protect the invisible vanilla slots.
     */
    public static boolean handleMouseClick(Screen screen, double x, double y, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (!isActive() || mc.screen != screen) {
            return false;
        }
        if (button == 0 && mc.gameMode != null
                && screen instanceof AbstractContainerScreen<?> container) {
            for (float[] box : slotBoxes) {
                if (x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3]) {
                    mc.gameMode.handleContainerInput(
                        container.getMenu().containerId, (int) box[4], 0,
                        ContainerInput.PICKUP, mc.player);
                    break;
                }
            }
        }
        return true;   // never let a click through to the hidden vanilla slots
    }

    /** Called every frame from CommissionHUD's Vexel render hook (inside beginFrame/endFrame). */
    public static void renderNvg() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof forfun.miningqol.client.gui.HudPositionScreen && showStatsLine()) {
            renderStatsPreview(mc);
        }
        if (!isActive() || !(mc.screen instanceof AbstractContainerScreen<?> container)) {
            slotBoxes.clear();
            if (mc.player != null) renderVanillaMenuStats(mc);
            return;
        }
        slotBoxes.clear();

        if (container.getMenu().containerId != cacheContainerId) {
            cellCache.clear();
            cacheContainerId = container.getMenu().containerId;
        }
        List<Cell> cells = readCells(mc, container.getMenu());
        if (cells.isEmpty()) return;
        // Packed layout: the commissions sit side by side in slot order, ignoring the
        // menu's spacer columns — no gap between the two pairs.
        int cols = cells.size();
        int rows = 1;

        xyz.meowing.vexel.api.RenderAPI r = xyz.meowing.vexel.Vexel.getRenderer();
        xyz.meowing.vexel.api.style.Font font = xyz.meowing.vexel.Vexel.getDefaultFont();

        // NVG draws in logical window pixels; the layout is in gui-scaled units.
        float f = (float) mc.getWindow().getScreenWidth() / Math.max(1, mc.getWindow().getGuiScaledWidth());
        int guiW = mc.getWindow().getGuiScaledWidth();
        int guiH = mc.getWindow().getGuiScaledHeight();
        float u = f * scale;

        float slot = buttonSize * scale;
        float gap = GAP * scale;
        float pad = PAD * scale;
        float gridW = cols * slot + (cols - 1) * gap;
        float gridH = rows * slot + (rows - 1) * gap;
        float panelW = gridW + pad * 2f;
        float panelH = gridH + pad * 2f;
        float panelX = (guiW - panelW) / 2f;
        float panelY = (guiH - panelH) / 2f;

        int accent = toRgb(accentColor);
        int done = toRgb(doneColor);
        int squareColor = 0xFF000000 | blend(accent, 0xFFFFFF, 0.55f);
        int textOnSquare = 0xFF14141B;

        // No scrim: the vanilla HUD is cancelled outright (GuiHideMixin), so the grid
        // floats over the bare world like Odin's terminals.
        r.rect(panelX * f, panelY * f, panelW * f, panelH * f, 0xF01C1C1C, 12f * u);

        // Lifetime total (Current Comms / /commtrack set, plus completions since) and this
        // session's count, centred just above the panel.
        if (showStatsLine()) {
            float size = 9f * u;
            drawStatsLine(r, font, f, size, (panelX + panelW / 2f) * f, panelY * f - size - 5f * u, u);
        }

        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            float x = panelX + pad + i * (slot + gap);
            float y = panelY + pad;
            boolean hovered = mouseX >= x && mouseX < x + slot && mouseY >= y && mouseY < y + slot;
            slotBoxes.add(new float[] {x, y, slot, slot, cell.slotId()});

            int fill = cell.claimable()
                ? 0xFF000000 | (hovered ? blend(done, 0xFFFFFF, 0.2f) : done & 0xFFFFFF)
                : hovered ? (0xFF000000 | blend(accent, 0xFFFFFF, 0.7f)) : squareColor;
            r.rect(x * f, y * f, slot * f, slot * f, fill, 6f * u);

            if (showProgress && cell.commission()) {
                float cx = (x + slot / 2f) * f;
                float cy = (y + slot / 2f) * f;
                if (cell.claimable()) {
                    // Drawn check mark — the bundled fonts have no ✓ glyph.
                    r.line(cx - 4.5f * u, cy + 0.5f * u, cx - 1.5f * u, cy + 3.5f * u, 1.8f * u, textOnSquare);
                    r.line(cx - 1.5f * u, cy + 3.5f * u, cx + 4.5f * u, cy - 3.5f * u, 1.8f * u, textOnSquare);
                } else {
                    String label = String.format(Locale.US, "%.0f%%", Math.floor(cell.progress()));
                    float size = 7.5f * u;
                    float tw = r.textWidth(label, size, font);
                    r.text(label, cx - tw / 2f, cy - size * 0.55f, size, textOnSquare, font);
                }
            }
        }
    }

    /** The drawable menu slots: everything with a real item, filler panes skipped. */
    private static List<Cell> readCells(Minecraft mc, AbstractContainerMenu menu) {
        List<CommissionHUD.MenuCommission> commissions = CommissionHUD.menuCommissions();
        List<Cell> cells = new ArrayList<>();
        int containerSlots = Math.max(0, menu.slots.size() - 36);
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;   // the player's own rows
            if (slot.index >= containerSlots) continue;          // belt and braces
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                // Mid-click prediction gap: keep the square until the server resyncs.
                Cell cached = cellCache.get(slot.index);
                if (cached != null) cells.add(cached);
                continue;
            }
            CommissionHUD.MenuCommission match = null;
            for (CommissionHUD.MenuCommission c : commissions) {
                if (c.slotId() == slot.index) {
                    match = c;
                    break;
                }
            }
            if (match == null) {
                cellCache.remove(slot.index);   // slot now holds something else
                continue;
            }
            Cell cell = new Cell(slot.index, slot.index / COLS, slot.index % COLS,
                true, match.claimable(), match.progress());
            cellCache.put(slot.index, cell);
            cells.add(cell);
        }
        return cells;
    }

    /** Mixes rgb toward {@code toward} by {@code t}. */
    private static int blend(int rgb, int toward, float t) {
        int r1 = (rgb >> 16) & 255, g1 = (rgb >> 8) & 255, b1 = rgb & 255;
        int r2 = (toward >> 16) & 255, g2 = (toward >> 8) & 255, b2 = toward & 255;
        return (Math.round(r1 + (r2 - r1) * t) << 16)
            | (Math.round(g1 + (g2 - g1) * t) << 8)
            | Math.round(b1 + (b2 - b1) * t);
    }

    private static int toRgb(float[] color) {
        int red = Math.round(color[0] * 255f);
        int green = Math.round(color[1] * 255f);
        int blue = Math.round(color[2] * 255f);
        return (red << 16) | (green << 8) | blue;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
