package forfun.miningqol.client.summary;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fines and coins mined during one shaft.
 *
 * <p>{@code PRISTINE!} lines count immediately so the total is live, and are remembered
 * per gem as "pending". The {@code [Sacks]} message that follows is authoritative: its
 * total for that gem replaces the pending amount, and the pair (authoritative, pending)
 * is kept as a sample. If the run ends with pending gems still unreconciled, the
 * samples' average ratio extrapolates what the final sack would have said.
 */
public final class GemSessionTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningQOL");
    private static final Pattern SACK_CHANGE = Pattern.compile("(?<amount>[+-][\\d,]+) (?<item>.+?) \\((?<sacks>.+?)\\)");
    private static final Pattern INLINE_CHANGE = Pattern.compile("^(?<amount>[+-][\\d,]+)\\s+(?<item>.+?)(?:\\s*\\(.*\\))?$");
    private static final Pattern PRISTINE = Pattern.compile("PRISTINE! You found .* (?<quality>Rough|Flawed|Fine|Flawless|Perfect) (?<gem>.*) Gemstone x(?<amount>[\\d,]+)!");
    private static final Pattern ITEM_NAME = Pattern.compile("(?<quality>Rough|Flawed|Fine|Flawless|Perfect) (?<gem>.*?) Gemstones?");
    private static final Pattern ITEM_NAME_NO_QUALITY = Pattern.compile("(?<gem>.*?) Gemstones?");
    private static final Pattern LEADING_SYMBOLS = Pattern.compile("^[^A-Za-z]+");

    public record GemDrop(String gem, String quality, int amount) {}

    /** Result of {@link #estimatedSessionFines()}: the value and whether it was extrapolated. */
    public record Estimate(double fines, boolean extrapolated) {}

    private static final class PendingTotals {
        double sessionProfit;
        double sessionFines;
    }

    private boolean sessionStarted;
    private double sessionProfit;
    private double sessionFines;
    private long lastSackMessageTime;
    private final Map<String, PendingTotals> pendingByGem = new LinkedHashMap<>();
    private final List<double[]> reconciliationSamples = new ArrayList<>();

    // ---- per-run accounting, for /shaftsummary debug ----
    private int pristineLines;
    private double pristineFines;
    private int sackMessages;
    private int sackMessagesWithRemovals;
    private int sackMessagesNoHover;
    private double sackAuthoritativeFines;
    private double negativeReconcileTotal;
    private double droppedNonRoughFines;
    private int trackedGemDropsWhileNotTracking;

    /** Whether a run is being counted right now; supplied by the run tracker. */
    private final Supplier<Boolean> tracking;
    /** The gem whose fines count for the current run ("Jasper"), or null; supplied by the run tracker. */
    private final Supplier<String> trackedGem;
    private final Supplier<Boolean> valueAsFlawless;
    private final Supplier<Boolean> sellOffer;

    public GemSessionTracker(Supplier<Boolean> tracking, Supplier<String> trackedGem,
                             Supplier<Boolean> valueAsFlawless, Supplier<Boolean> sellOffer) {
        this.tracking = tracking;
        this.trackedGem = trackedGem;
        this.valueAsFlawless = valueAsFlawless;
        this.sellOffer = sellOffer;
    }

    public void startSession() {
        if (sessionStarted) {
            LOGGER.info("[ShaftSummary] Session start ignored; tracker session already active.");
            return;
        }
        sessionStarted = true;
        sessionProfit = 0.0;
        sessionFines = 0.0;
        pendingByGem.clear();
        reconciliationSamples.clear();
        pristineLines = 0;
        pristineFines = 0.0;
        sackMessages = 0;
        sackMessagesWithRemovals = 0;
        sackMessagesNoHover = 0;
        sackAuthoritativeFines = 0.0;
        negativeReconcileTotal = 0.0;
        droppedNonRoughFines = 0.0;
        trackedGemDropsWhileNotTracking = 0;
        LOGGER.info("[ShaftSummary] Session started. Fines reset to 0.");
    }

    public void endSession() {
        sessionStarted = false;
    }

    public void fullCleanup() {
        sessionStarted = false;
        pendingByGem.clear();
        reconciliationSamples.clear();
    }

    public double sessionFines() {
        return sessionFines;
    }

    public long sessionProfit() {
        return Math.round(sessionProfit);
    }

    public long lastSackMessageTime() {
        return lastSackMessageTime;
    }

    /** Called for every game chat line; returns true when the line was a gem event. */
    public boolean processMessage(Component message, String cleanText) {
        Matcher pristine = PRISTINE.matcher(cleanText);
        if (pristine.find()) {
            GemDrop drop = new GemDrop(pristine.group("gem").trim(), pristine.group("quality").trim(), parseAmount(pristine.group("amount"), 0));
            LOGGER.info("[ShaftSummary] PRISTINE parsed: gem={} quality={} amount={}", drop.gem(), drop.quality(), drop.amount());
            addGemValue(drop, false);
            return true;
        }
        if (!cleanText.startsWith("[Sacks]")) return false;
        lastSackMessageTime = System.currentTimeMillis();
        sackMessages++;
        List<String> added = collectHoverTexts(message, "Added");
        List<String> removed = collectHoverTexts(message, "Removed");
        String addText = String.join("\n", added);
        boolean hasRemovalSignals = !removed.isEmpty() || cleanText.contains(" -");
        if (hasRemovalSignals) sackMessagesWithRemovals++;
        if (addText.isEmpty()) {
            sackMessagesNoHover++;
            List<GemDrop> drops = parseSackChanges(cleanText);
            if (drops.isEmpty()) return true;
            reconcileSackDrops(drops, "Base text", !hasRemovalSignals);
            return true;
        }
        reconcileSackDrops(parseSackChanges(addText), "Hover", !hasRemovalSignals);
        return true;
    }

    private void reconcileSackDrops(List<GemDrop> sackDrops, String source, boolean allowNonRoughAuthoritative) {
        Map<String, List<GemDrop>> byGem = new LinkedHashMap<>();
        boolean isTracking = tracking.get();
        String tracked = trackedGem.get();
        for (GemDrop drop : sackDrops) {
            if (drop.amount() <= 0) continue;
            if (!allowNonRoughAuthoritative && !drop.quality().equalsIgnoreCase("Rough")) {
                if (countsForFines(drop.gem(), tracked)) droppedNonRoughFines += fineEquivalent(drop.quality(), drop.amount());
                continue;
            }
            if (!isTracking && countsForFines(drop.gem(), tracked)) trackedGemDropsWhileNotTracking++;
            byGem.computeIfAbsent(normalizeGem(drop.gem()), k -> new ArrayList<>()).add(drop);
        }
        if (byGem.isEmpty()) return;

        for (Map.Entry<String, List<GemDrop>> entry : byGem.entrySet()) {
            String gemKey = entry.getKey();
            List<GemDrop> drops = entry.getValue();
            boolean hasAuthoritativeNonRough = false;
            if (allowNonRoughAuthoritative) {
                for (GemDrop d : drops) {
                    if (!d.quality().equalsIgnoreCase("Rough")) {
                        hasAuthoritativeNonRough = true;
                        break;
                    }
                }
            }
            PendingTotals stored = pendingByGem.remove(gemKey);
            if (stored == null) stored = new PendingTotals();
            PendingTotals pending = hasAuthoritativeNonRough ? stored : new PendingTotals();

            double authoritativeProfit = 0.0;
            double authoritativeFines = 0.0;
            for (GemDrop drop : drops) {
                LOGGER.info("[ShaftSummary] {} parsed: gem={} quality={} amount={}", source, drop.gem(), drop.quality(), drop.amount());
                if (isTracking) {
                    authoritativeProfit += value(drop);
                    if (countsForFines(drop.gem(), tracked)) authoritativeFines += fineEquivalent(drop.quality(), drop.amount());
                }
            }
            if (isTracking) {
                sackAuthoritativeFines += authoritativeFines;
                sessionProfit += authoritativeProfit - pending.sessionProfit;
                double fineDelta = authoritativeFines - pending.sessionFines;
                if (fineDelta < 0.0) negativeReconcileTotal += -fineDelta;
                if (fineDelta != 0.0) {
                    sessionFines += fineDelta;
                    LOGGER.info("[ShaftSummary] sessionFines reconcile gem={} authoritative={} pending={} delta={} -> total={}",
                        gemKey, authoritativeFines, pending.sessionFines, fineDelta, sessionFines);
                }
                if (pending.sessionFines > 0.0 && authoritativeFines > 0.0) {
                    reconciliationSamples.add(new double[]{authoritativeFines, pending.sessionFines});
                }
            }
        }
    }

    private void addGemValue(GemDrop drop, boolean authoritativeSack) {
        if (!tracking.get()) {
            if (countsForFines(drop.gem(), trackedGem.get())) trackedGemDropsWhileNotTracking++;
            return;
        }
        boolean trackPending = !authoritativeSack && !drop.quality().equalsIgnoreCase("Rough");
        String gemKey = normalizeGem(drop.gem());
        double totalValue = value(drop);
        if (countsForFines(drop.gem(), trackedGem.get())) {
            double fineEq = fineEquivalent(drop.quality(), drop.amount());
            pristineLines++;
            pristineFines += fineEq;
            sessionFines += fineEq;
            LOGGER.info("[ShaftSummary] sessionFines += {} (quality={}, amount={}) -> total={}", fineEq, drop.quality(), drop.amount(), sessionFines);
            if (trackPending) pendingByGem.computeIfAbsent(gemKey, k -> new PendingTotals()).sessionFines += fineEq;
        }
        sessionProfit += totalValue;
        if (trackPending) pendingByGem.computeIfAbsent(gemKey, k -> new PendingTotals()).sessionProfit += totalValue;
    }

    private static boolean countsForFines(String gem, String tracked) {
        if (tracked == null) return gem.toLowerCase(Locale.ROOT).contains("jasper");
        return gem.equalsIgnoreCase(tracked);
    }

    /**
     * The fines total, extrapolated for gems whose PRISTINE lines have no sack message
     * yet using how earlier pending amounts compared to their sack totals.
     */
    public Estimate estimatedSessionFines() {
        if (reconciliationSamples.isEmpty()) return new Estimate(sessionFines, false);
        double currentPending = 0.0;
        for (PendingTotals p : pendingByGem.values()) currentPending += p.sessionFines;
        if (currentPending <= 0.0) return new Estimate(sessionFines, false);
        double sumAuthoritative = 0.0, sumPending = 0.0;
        for (double[] sample : reconciliationSamples) {
            sumAuthoritative += sample[0];
            sumPending += sample[1];
        }
        double avgRatio = sumAuthoritative / sumPending;
        double delta = currentPending * (avgRatio - 1.0);
        double estimated = sessionFines + delta;
        LOGGER.info("[ShaftSummary] Extrapolation: samples={} avgRatio={} pending={} delta={} raw={} estimated={}",
            reconciliationSamples.size(), String.format(Locale.ROOT, "%.4f", avgRatio), currentPending,
            String.format(Locale.ROOT, "%.4f", delta), sessionFines, estimated);
        return new Estimate(estimated, true);
    }

    /** One-line accounting of how the current total was reached. */
    public String accounting() {
        double pendingNow = 0.0;
        for (PendingTotals p : pendingByGem.values()) pendingNow += p.sessionFines;
        return String.format(Locale.ROOT,
            "total=%.2f pristine=%d lines/%.2f fines | sacks=%d (noHover=%d, withRemovals=%d) auth=%.2f | reconcileMinus=%.2f droppedNonRough=%.2f | pendingNow=%.2f samples=%d | trackedDropsWhileIdle=%d",
            sessionFines, pristineLines, pristineFines, sackMessages, sackMessagesNoHover, sackMessagesWithRemovals,
            sackAuthoritativeFines, negativeReconcileTotal, droppedNonRoughFines, pendingNow, reconciliationSamples.size(),
            trackedGemDropsWhileNotTracking);
    }

    private double value(GemDrop drop) {
        boolean flawless = valueAsFlawless.get();
        double unit = BazaarPrices.price(BazaarPrices.productId(drop.gem(), flawless ? "Flawless" : "Fine"), sellOffer.get());
        return unit * equivalentAmount(drop.quality(), drop.amount(), flawless);
    }

    /** How many Fine gems this stack is worth. */
    public static double fineEquivalent(String quality, int amount) {
        return switch (quality) {
            case "Rough" -> amount / 6400.0;
            case "Flawed" -> amount / 80.0;
            case "Fine" -> amount;
            case "Flawless" -> amount * 80.0;
            default -> 0.0;
        };
    }

    public static double equivalentAmount(String quality, int amount, boolean asFlawless) {
        if (asFlawless) {
            return switch (quality) {
                case "Rough" -> amount / 512000.0;
                case "Flawed" -> amount / 6400.0;
                case "Fine" -> amount / 80.0;
                case "Flawless" -> amount;
                case "Perfect" -> amount * 80.0;
                default -> 0.0;
            };
        }
        return switch (quality) {
            case "Rough" -> amount / 6400.0;
            case "Flawed" -> amount / 80.0;
            case "Fine" -> amount;
            case "Flawless" -> amount * 80.0;
            case "Perfect" -> amount * 6400.0;
            default -> 0.0;
        };
    }

    // ---- parsing ----

    private static List<String> collectHoverTexts(Component component, String prefix) {
        Set<String> texts = new LinkedHashSet<>();
        collectHoverTexts(component, prefix, texts);
        return new ArrayList<>(texts);
    }

    private static void collectHoverTexts(Component component, String prefix, Set<String> into) {
        HoverEvent hover = component.getStyle().getHoverEvent();
        if (hover instanceof HoverEvent.ShowText show) {
            String plain = show.value().getString().replaceAll("§.", "").trim();
            if (plain.startsWith(prefix)) into.add(plain);
        }
        for (Component sibling : component.getSiblings()) {
            collectHoverTexts(sibling, prefix, into);
        }
    }

    static GemDrop parseItemName(String itemName) {
        String cleaned = LEADING_SYMBOLS.matcher(itemName.trim()).replaceFirst("").trim();
        Matcher m = ITEM_NAME.matcher(cleaned);
        if (m.find()) return new GemDrop(m.group("gem").trim(), m.group("quality").trim(), 0);
        Matcher rough = ITEM_NAME_NO_QUALITY.matcher(cleaned);
        if (!rough.find()) return null;
        return new GemDrop(rough.group("gem").trim(), "Rough", 0);
    }

    static List<GemDrop> parseSackChanges(String text) {
        List<GemDrop> results = new ArrayList<>();
        Matcher m = SACK_CHANGE.matcher(text);
        while (m.find()) {
            Integer amount = parseAmount(m.group("amount"), null);
            if (amount == null) continue;
            GemDrop parsed = parseItemName(m.group("item"));
            if (parsed != null) results.add(new GemDrop(parsed.gem(), parsed.quality(), amount));
        }
        if (!results.isEmpty()) return results;

        String normalized = text.replace("[Sacks]", "").replace("Added:", "").replace("Removed:", "")
            .replaceAll("\\s+(?=[+-][\\d,]+\\s)", "\n");
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            Matcher im = INLINE_CHANGE.matcher(trimmed);
            if (!im.find()) continue;
            Integer amount = parseAmount(im.group("amount"), null);
            if (amount == null) continue;
            GemDrop parsed = parseItemName(im.group("item").trim());
            if (parsed != null) results.add(new GemDrop(parsed.gem(), parsed.quality(), amount));
        }
        return results;
    }

    private static Integer parseAmount(String raw, Integer fallback) {
        try {
            return Integer.parseInt(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static String normalizeGem(String gem) {
        return gem.trim().toLowerCase(Locale.ROOT);
    }
}
