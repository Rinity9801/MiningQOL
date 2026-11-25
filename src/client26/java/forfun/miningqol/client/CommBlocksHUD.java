package forfun.miningqol.client;

import forfun.miningqol.client.gui.HudPositionScreen;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Blocks left on the commission you're mining, beside the crosshair (Commission Helper's counter).
 *
 * <p>The tab list's percent is the truth, but it lags and moves in steps, so the estimate runs
 * ahead of it: every block you break adds what a block is worth, and every tab update pulls the
 * estimate back to where the tab says it can be. That also squares up anything miscounted, like
 * another player's break nearby or a Pickobulus landing out of range.
 *
 * <p>Dwarven Mines commissions have fixed totals. A Glacite collector's percent per block varies
 * by player, so it's learned from how many blocks each finished commission took, and kept in the
 * config.
 */
public final class CommBlocksHUD {
    private static final Identifier HUD_ID =
        Identifier.fromNamespaceAndPath("miningqol", "comm_blocks_hud");
    /** Shown while you've broken a block for the commission, or it moved, this recently. */
    private static final long ACTIVE_MS = 20_000L;
    /** Breaks this recent may not have reached the tab list yet. */
    private static final long TAB_LAG_MS = 4_000L;
    /** The tab shows one decimal, so the real percent is up to this much above it. */
    private static final double TAB_STEP = 0.1;
    /** Breaks the server reports this close count as yours: reach, plus Efficient Miner's spread. */
    private static final double OWN_BREAK_RANGE_SQR = 49.0;
    /** Learned rates are trusted past this many units; below it, the defaults. */
    private static final double MIN_LEARNED_UNITS = 10.0;
    /** A commission mostly finished before it was first seen is too short to learn from. */
    private static final double MIN_SAMPLE_UNITS = 10.0;
    /** The tab running this many blocks past the estimate means breaks went uncounted. */
    private static final double UNSEEN_BLOCKS = 3.0;
    private static final double LEARN_DECAY = 0.995;
    private static final int MAX_RECENT = 64;
    private static final int CHECK_EVERY_TICKS = 10;
    private static final double DEFAULT_PCT_PER_UNIT = 1.73;
    private static final double DEFAULT_GEM_PCT_PER_UNIT = 7.5;

    private static final Set<Block> MITHRIL = Set.of(Blocks.WOOL.gray(), Blocks.DYED_TERRACOTTA.cyan(),
        Blocks.WOOL.lightBlue(), Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE);
    private static final Block TITANIUM = Blocks.POLISHED_DIORITE;
    /** What each Glacite collector commission is mined from. */
    private static final Map<String, Set<Block>> COLLECTORS = Map.of(
        "glacite collector", Set.of(Blocks.PACKED_ICE),
        "umber collector", Set.of(Blocks.TERRACOTTA, Blocks.DYED_TERRACOTTA.brown(), Blocks.SMOOTH_RED_SANDSTONE),
        "tungsten collector", Set.of(Blocks.INFESTED_COBBLESTONE, Blocks.CLAY),
        "onyx gemstone collector", Set.of(Blocks.STAINED_GLASS.black(), Blocks.STAINED_GLASS_PANE.black()),
        "aquamarine gemstone collector", Set.of(Blocks.STAINED_GLASS.blue(), Blocks.STAINED_GLASS_PANE.blue()),
        "peridot gemstone collector", Set.of(Blocks.STAINED_GLASS.green(), Blocks.STAINED_GLASS_PANE.green()),
        "citrine gemstone collector", Set.of(Blocks.STAINED_GLASS.brown(), Blocks.STAINED_GLASS_PANE.brown()));

    private static boolean registered;
    private static boolean enabled = false;
    private static boolean hideWithF1 = false;
    /** "46 left" instead of "12/58". */
    private static boolean showLeft = false;
    private static final HudAnchor ANCHOR = new HudAnchor(0, 0, CommBlocksHUD::getWidth, CommBlocksHUD::getHeight);

    /** Lower-cased commission name -> {percent, units} learned for it, decayed so recent mining leads. */
    private static final Map<String, double[]> rates = new HashMap<>();
    private static final Map<String, Tracked> tracked = new LinkedHashMap<>();
    private static int ticks = 0;

    private CommBlocksHUD() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_ID,
            (context, tickCounter) -> render(context)
        );
    }

    // ===== What counts =====

    /** Blocks (or kills) a commission takes in total, or 0 when that isn't fixed. */
    private static int fixedTotal(String key) {
        return switch (key) {
            case "mithril miner" -> 350;
            case "titanium miner" -> 15;
            case "goblin slayer" -> 100;
            case "glacite walker slayer" -> 50;
            default -> key.endsWith(" mithril") ? 250 : key.endsWith(" titanium") ? 10 : 0;
        };
    }

    private static boolean isCounted(String key) {
        return fixedTotal(key) > 0 || COLLECTORS.containsKey(key);
    }

    private static boolean counts(String key, Block block) {
        Set<Block> collected = COLLECTORS.get(key);
        if (collected != null) return collected.contains(block);
        if (key.equals("mithril miner") || key.endsWith(" mithril")) return MITHRIL.contains(block);
        return (key.equals("titanium miner") || key.endsWith(" titanium")) && block == TITANIUM;
    }

    /** "Royal Mines Mithril" only counts in the Royal Mines. */
    private static boolean countsHere(String key) {
        if (!key.endsWith(" mithril") && !key.endsWith(" titanium")) return true;
        return CommissionHUD.isInArea(Minecraft.getInstance(), key.substring(0, key.lastIndexOf(' ')));
    }

    /** Clay and red sandstone count three times, brown terracotta twice. */
    private static double unitsOf(Block block) {
        if (block == Blocks.CLAY || block == Blocks.SMOOTH_RED_SANDSTONE) return 3.0;
        return block == Blocks.DYED_TERRACOTTA.brown() ? 2.0 : 1.0;
    }

    private static boolean isOre(BlockState state) {
        return MITHRIL.contains(state.getBlock()) || state.getBlock() == TITANIUM;
    }

    private static double pctPerUnit(String key) {
        int total = fixedTotal(key);
        if (total > 0) return 100.0 / total;
        if (isLearned(key)) return rates.get(key)[0] / rates.get(key)[1];
        return key.endsWith("gemstone collector") ? DEFAULT_GEM_PCT_PER_UNIT : DEFAULT_PCT_PER_UNIT;
    }

    private static boolean isLearned(String key) {
        double[] rate = rates.get(key);
        return rate != null && rate[1] >= MIN_LEARNED_UNITS;
    }

    // ===== Breaks =====

    /** Mixin entry: the client finished breaking a block itself. */
    public static void onOwnBreak(BlockState state) {
        if (enabled) count(state.getBlock());
    }

    /**
     * Mixin entry: the server changed a block. A client-side break already left air here, so this
     * only sees the ones the client didn't do itself: Efficient Miner's extras and Pickobulus.
     */
    public static void onBlockChange(BlockPos pos, BlockState old, BlockState now) {
        if (!enabled || tracked.isEmpty()) return;
        if (!now.isAir() && !now.is(Blocks.BEDROCK) && !(isOre(old) && !isOre(now))) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= OWN_BREAK_RANGE_SQR) {
            count(old.getBlock());
        }
    }

    private static void count(Block block) {
        long now = System.currentTimeMillis();
        double units = unitsOf(block);
        for (Map.Entry<String, Tracked> entry : tracked.entrySet()) {
            String key = entry.getKey();
            Tracked t = entry.getValue();
            if (t.pct >= 100.0 || !counts(key, block) || !countsHere(key)) continue;
            t.estimate += units * pctPerUnit(key);
            t.units += units;
            t.unitsPerBlock = units;
            t.lastBreakAt = now;
            t.recent.addLast(new Break(now, units));
            while (t.recent.size() > MAX_RECENT) t.recent.removeFirst();
        }
    }

    // ===== Tab list =====

    /** Client tick: follows the Commission HUD's list, which is the tab list and the pigeon menu. */
    public static void tick() {
        if (!enabled || ++ticks % CHECK_EVERY_TICKS != 0) return;
        long now = System.currentTimeMillis();
        Set<String> seen = new HashSet<>();
        for (CommissionHUD.CommissionEntry entry : CommissionHUD.commissions()) {
            String key = entry.name().toLowerCase(Locale.ROOT);
            if (!isCounted(key)) continue;
            seen.add(key);
            Tracked t = tracked.get(key);
            if (t == null || entry.progress() < t.pct) {
                // First sight, or the same commission handed out again after a claim.
                tracked.put(key, new Tracked(entry.progress()));
            } else if (entry.progress() > t.pct) {
                onProgress(key, t, entry.progress(), now);
            }
        }
        tracked.keySet().retainAll(seen);
    }

    private static void onProgress(String key, Tracked t, double pct, long now) {
        // Further ahead than the breaks can explain: a Pickobulus landed out of range.
        if (isLearned(key) && pct > t.estimate + UNSEEN_BLOCKS * pctPerUnit(key) * t.unitsPerBlock) {
            t.sawEveryBreak = false;
        }
        // Learned per finished commission, not per update: the tab lags, so an update's percent
        // belongs to blocks from seconds ago, and a commission can finish before it catches up.
        // The last block overshoots 100%, by half a block on average.
        if (pct >= 100.0 && t.sawEveryBreak && t.units >= MIN_SAMPLE_UNITS && fixedTotal(key) == 0) {
            learn(key, 100.0 - t.startPct, t.units - t.unitsPerBlock / 2.0);
        }
        t.pct = pct;
        t.progressAt = now;

        double fresh = 0;
        for (Break recent : t.recent) {
            if (now - recent.at() <= TAB_LAG_MS) fresh += recent.units();
        }
        t.estimate = Math.max(pct, Math.min(t.estimate, pct + TAB_STEP + fresh * pctPerUnit(key)));
    }

    private static void learn(String key, double pct, double units) {
        double[] rate = rates.computeIfAbsent(key, k -> new double[2]);
        if (rate[1] >= MIN_LEARNED_UNITS) {
            double current = rate[0] / rate[1];
            double sample = pct / units;
            if (sample < current * 0.4 || sample > current * 2.5) return;
        }
        rate[0] = rate[0] * LEARN_DECAY + pct;
        rate[1] = rate[1] * LEARN_DECAY + units;
    }

    // ===== Rendering =====

    public static void render(GuiGraphicsExtractor context) {
        if (!enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (hideWithF1 && client.gui.hud.isHidden()) return;
        if (client.gui.screen() instanceof HudPositionScreen) return;

        long now = System.currentTimeMillis();
        int y = ANCHOR.y();
        for (Map.Entry<String, Tracked> entry : tracked.entrySet()) {
            Tracked t = entry.getValue();
            if (t.pct >= 100.0 || !t.isActive(now)) continue;
            double perBlock = pctPerUnit(entry.getKey()) * t.unitsPerBlock;
            int total = blocksFor(100.0, perBlock);
            int left = Math.max(1, blocksFor(100.0 - t.estimate, perBlock));
            draw(context, Math.max(0, total - left), Math.max(total, left), left, y);
            y += client.font.lineHeight;
        }
    }

    /** The HUD editor's preview, drawn at the live position. */
    public static void drawPreview(GuiGraphicsExtractor context) {
        draw(context, 12, 58, 46, ANCHOR.y());
    }

    private static void draw(GuiGraphicsExtractor context, int done, int total, int left, int y) {
        String text = showLeft ? left + " left" : done + "/" + total;
        context.text(Minecraft.getInstance().font, Component.literal(text), ANCHOR.x(), y,
            progressColor((double) done / total), true);
    }

    private static int blocksFor(double pct, double perBlock) {
        return (int) Math.ceil(Math.max(0.0, pct) / perBlock - 0.1);
    }

    private static int progressColor(double fraction) {
        if (fraction <= 0.35) return 0xFFFF5555;
        if (fraction <= 0.65) return mix(0xFF5555, 0xFFFF55, (fraction - 0.35) / 0.3);
        return mix(0xFFFF55, 0x55FF55, (fraction - 0.65) / 0.2);
    }

    private static int mix(int from, int to, double amount) {
        double k = Math.max(0.0, Math.min(1.0, amount));
        int red = (int) ((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * k);
        int green = (int) ((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * k);
        int blue = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * k);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public static int getWidth() {
        Minecraft client = Minecraft.getInstance();
        return client.font == null ? 40 : client.font.width(showLeft ? "000 left" : "000/000");
    }

    public static int getHeight() {
        return 9;
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static boolean isHideWithF1() { return hideWithF1; }
    public static void setHideWithF1(boolean value) { hideWithF1 = value; }

    public static boolean isShowLeft() { return showLeft; }
    public static void setShowLeft(boolean value) { showLeft = value; }

    public static void setPosition(int x, int y) { ANCHOR.set(x, y); }

    /** Edge anchor for the config — see {@link HudAnchor}. */
    public static HudAnchor anchor() { return ANCHOR; }

    public static int getX() { return ANCHOR.x(); }
    public static int getY() { return ANCHOR.y(); }

    /** The learned rates, for the config. */
    public static Map<String, double[]> getRates() {
        return new HashMap<>(rates);
    }

    public static void setRates(Map<String, double[]> saved) {
        rates.clear();
        saved.forEach((key, rate) -> {
            if (rate != null && rate.length == 2 && rate[0] > 0 && rate[1] > 0) rates.put(key, rate);
        });
    }

    private record Break(long at, double units) {}

    private static final class Tracked {
        /** Where this commission was when first seen. */
        final double startPct;
        /** What the tab list last said. */
        double pct;
        /** The tab's percent plus the breaks it hasn't shown yet. */
        double estimate;
        /** Everything counted toward this commission so far. */
        double units = 0;
        /** Of the last block counted: what's left is told in blocks like the one you're on. */
        double unitsPerBlock = 1.0;
        boolean sawEveryBreak = true;
        long lastBreakAt = 0;
        long progressAt = 0;
        final ArrayDeque<Break> recent = new ArrayDeque<>();

        Tracked(double pct) {
            this.startPct = pct;
            this.pct = pct;
            this.estimate = pct;
        }

        boolean isActive(long now) {
            return now - lastBreakAt <= ACTIVE_MS || now - progressAt <= ACTIVE_MS;
        }
    }
}
