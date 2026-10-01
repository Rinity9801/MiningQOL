package forfun.miningqol.client.summary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import forfun.miningqol.client.MayhemHUD;
import forfun.miningqol.client.MqoChat;
import forfun.miningqol.client.party.CorpseType;
import forfun.miningqol.client.party.MineshaftAutoParty;
import forfun.miningqol.client.party.ShaftType;
import forfun.miningqol.mixin.client.ChatComponentAccessor;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prints a summary of every tracked gemstone mineshaft run once it ends: fines mined,
 * coins, time, corpses, campfires, the buffs that were up, and how the run ranks against
 * the saved history for that shaft type.
 *
 * <p>A run starts when the scoreboard settles on a tracked shaft id and ends the moment
 * the mineshaft sidebar has been gone for a few scans (or the shaft id changes). Fines
 * are counted live off PRISTINE lines and corrected by the {@code [Sacks]} messages; if
 * the run ends with gems still unreconciled the total is extrapolated, printed, and
 * rewritten in place should the final sack message arrive within 35 seconds.
 */
public final class ShaftSummary {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningQOL");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File HISTORY_FILE = new File("config/miningqol_shaft_runs.json");
    private static final int PROTECTED_RECENT_RUN_COUNT = 10;

    private static final long SCAN_INTERVAL_MS = 500L;
    private static final int SHAFT_EXIT_SCANS = 4;
    private static final long FINAL_SACK_QUIET_PERIOD_MS = 400L;
    private static final long CORRECTION_TIMEOUT_MS = 35_000L;

    private static final Pattern EVENT_LINE = Pattern.compile("Event:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final List<String> ALLOWED_EVENTS = List.of("Better Together", "Gone with the Wind", "Fortunate Freezing", "2x Powder");
    public static final List<String> RANK_CRITERIA = List.of("lapis", "mayhem", "skymall", "event", "campfires");

    private enum State { IDLE, IN_SHAFT, COUNTING, AWAITING_CORRECTION }

    // ---- settings ----
    private static boolean enabled = true;
    private static boolean frontLoaded = false;
    private static boolean trackAllShaftSummaries = true;
    private static boolean showBuffsInSummary = false;
    private static boolean trackEfficiencyInSummary = true;
    private static int efficiencyPauseSeconds = 15;
    private static boolean autoSave = true;
    private static boolean printToChat = true;
    private static boolean printToParty = false;
    private static int maxSavedRuns = 100;
    private static final Set<String> summaryShaftTypes = new LinkedHashSet<>(defaultSummaryKeys());
    private static final Set<String> savedShaftTypes = new LinkedHashSet<>(defaultSavedKeys());
    private static final Set<String> ranksBy = new LinkedHashSet<>();
    private static int gemMiningSpeed = 0;
    private static boolean blueCheese = false;
    private static boolean valueAsFlawless = true;
    private static boolean sellOffer = true;

    // ---- run state ----
    private static State state = State.IDLE;
    private static boolean tracking;
    private static ShaftPbType currentType;
    private static ShaftType currentShaft;
    private static long sessionStart;
    private static long runEndAt;
    private static long finalizeRequestedAt;
    private static int lapisPeak;
    private static int lootedCorpses;
    private static int campfiresUsed;
    private static String mayhemPerk;
    private static String skyMallPerk;
    private static String event;
    private static final GemSessionTracker gems = new GemSessionTracker(
        () -> tracking,
        () -> currentType == null ? null : currentType.trackedGem(),
        () -> valueAsFlawless,
        () -> sellOffer);
    private static final ShaftEfficiency efficiency = new ShaftEfficiency();

    // ---- detection ----
    private static long lastScan;
    private static int missedScans;
    private static ShaftType lastSeenType;
    private static int typeStableScans;

    // ---- correction ----
    private static ShaftRun awaitingCorrectionRun;
    private static double awaitingCorrectionEstimatedFines;
    private static long awaitingCorrectionDeadline;

    private static final List<ShaftRun> history = new ArrayList<>();
    private static boolean initialized;
    /** Accounting lines for the last few runs, newest last; shown by /shaftsummary debug. */
    private static final List<String> runLog = new ArrayList<>();
    private static int runsStarted;

    private ShaftSummary() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        loadHistory();
        BazaarPrices.refresh();
        SkyMallTracker.init();
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (enabled && trackEfficiencyInSummary && tracking && level.isClientSide()) {
                efficiency.onAttackBlock(level.getBlockState(pos), pos);
            }
            return InteractionResult.PASS;
        });
    }

    // ---- tick ----

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        SkyMallTracker.tick(client);
        long now = System.currentTimeMillis();

        List<String> tab = null;
        if (trackEfficiencyInSummary) {
            tab = MineshaftAutoParty.tabLines(client);
            efficiency.onTick(client, tab, efficiencyPauseSeconds, blueCheese, gemMiningSpeed);
        } else {
            efficiency.reset();
        }

        if (state == State.AWAITING_CORRECTION) {
            // Keep scanning: the next shaft can be entered before the correction window closes.
            checkCorrection(now);
        }
        if (!enabled) {
            if (state != State.IDLE) cleanup();
            return;
        }
        if (state != State.IDLE && client.player.tickCount % 20 == 0) {
            scrapeTablist(client, tab != null ? tab : MineshaftAutoParty.tabLines(client));
        }

        if (now - lastScan < SCAN_INTERVAL_MS) return;
        lastScan = now;
        onDetected(detectShaft(client));
    }

    /**
     * Current shaft id, or null. Requires two agreeing scans before trusting an id and
     * several empty scans before calling the shaft left, like the auto-party does.
     */
    private static ShaftType detectShaft(Minecraft client) {
        String sidebar = MineshaftAutoParty.sidebarText(client);
        if (sidebar == null || !MineshaftAutoParty.saysMineshaft(sidebar)) {
            if (++missedScans >= SHAFT_EXIT_SCANS) {
                lastSeenType = null;
                typeStableScans = 0;
                return null;
            }
            return currentShaft;
        }
        missedScans = 0;
        ShaftType seen = ShaftType.fromScoreboard(sidebar);
        if (seen != null && seen == lastSeenType) {
            typeStableScans++;
        } else {
            lastSeenType = seen;
            typeStableScans = seen == null ? 0 : 1;
        }
        return typeStableScans >= 2 ? seen : currentShaft;
    }

    private static void onDetected(ShaftType type) {
        boolean shouldTrack = shouldTrackMineshaft(type);
        if (shouldTrack) {
            if (currentShaft == type) return;
            if (currentShaft != null) endShaft(false, true);
            startShaft(type);
        } else if (currentShaft != null) {
            endShaft(false, false);
        }
    }

    static boolean shouldTrackMineshaft(ShaftType type) {
        if (type == null || !enabled) return false;
        if (!trackAllShaftSummaries) return type == ShaftType.JASPER || type == ShaftType.JASPER_CRYSTAL;
        ShaftPbType pb = ShaftPbType.fromShaftType(type);
        return pb != null && (summaryShaftTypes.contains(pb.key()) || savedShaftTypes.contains(pb.key()));
    }

    private static void startShaft(ShaftType type) {
        if (state == State.AWAITING_CORRECTION) {
            LOGGER.info("[ShaftSummary] New shaft entered while AWAITING_CORRECTION. Accepting estimate.");
            fullCleanup();
        }
        if (state != State.IDLE) return;
        LOGGER.info("[ShaftSummary] Entered {}. Monitoring for start.", type.displayName());
        runsStarted++;
        state = State.IN_SHAFT;
        currentShaft = type;
        currentType = ShaftPbType.fromShaftType(type);
        lapisPeak = 0;
        lootedCorpses = 0;
        campfiresUsed = 0;
        mayhemPerk = null;
        skyMallPerk = null;
        event = null;
        efficiency.reset();
        gems.startSession();
        BazaarPrices.refresh();
        startCounting();
    }

    private static void startCounting() {
        if (state != State.IN_SHAFT) return;
        LOGGER.info("[ShaftSummary] Counting started.");
        state = State.COUNTING;
        sessionStart = System.currentTimeMillis();
        tracking = true;
        gems.startSession();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) client.player.sendOverlayMessage(Component.literal("§a[MQO] Shaft Run Started! GL!"));
    }

    private static void endShaft(boolean premature, boolean immediate) {
        if (state == State.IDLE) return;
        if (state == State.COUNTING) {
            long now = System.currentTimeMillis();
            runEndAt = now;
            finalizeRequestedAt = now;
            finalizeShaft();
            return;
        }
        if (state != State.AWAITING_CORRECTION) cleanup();
    }

    private static void finalizeShaft() {
        if (state == State.COUNTING) {
            long duration = ((runEndAt > 0 ? runEndAt : System.currentTimeMillis()) - sessionStart) / 1000L;
            GemSessionTracker.Estimate estimate = gems.estimatedSessionFines();
            ShaftRun run = createRun(duration, estimate.fines());
            boolean shouldPrint = shouldPrintSummary(run);
            boolean shouldSave = shouldSaveRun(run);
            LOGGER.info("[ShaftSummary] Run Ended. Profit: {}, Fines: {} (extrapolated={})", run.estimatedProfit, run.finesCount, estimate.extrapolated());
            String acct = String.format(Locale.ROOT, "run#%d %s %ds printed=%.1f%s | %s", runsStarted,
                currentType == null ? "?" : currentType.label(), duration, run.finesCount, estimate.extrapolated() ? " (est)" : "", gems.accounting());
            LOGGER.info("[ShaftSummary] Accounting: {}", acct);
            runLog.add(acct);
            while (runLog.size() > 5) runLog.remove(0);
            if (printToChat && shouldPrint) printRunToChat(run);
            if (autoSave && shouldSave) {
                history.add(run);
                saveHistory();
            }
            if (printToParty && shouldPrint) sendToParty(run);
            if (estimate.extrapolated() && shouldSave) {
                awaitingCorrectionRun = run;
                awaitingCorrectionEstimatedFines = estimate.fines();
                awaitingCorrectionDeadline = System.currentTimeMillis() + CORRECTION_TIMEOUT_MS;
                state = State.AWAITING_CORRECTION;
                // The shaft itself is over: a new one (even of the same type) must start fresh.
                currentShaft = null;
                // Kept counting so the late sack message reconciles the pending PRISTINE
                // amounts it is being waited for.
                LOGGER.info("[ShaftSummary] Entering AWAITING_CORRECTION. Deadline in {}ms.", CORRECTION_TIMEOUT_MS);
                return;
            }
        }
        fullCleanup();
    }

    private static ShaftRun createRun(long durationSeconds, double fines) {
        ShaftRun run = new ShaftRun();
        run.timestamp = System.currentTimeMillis();
        run.shaftTypeId = currentType == null ? null : currentType.key();
        run.finesCount = fines;
        run.totalGemstones = (int) (fines * 80);
        run.estimatedProfit = gems.sessionProfit();
        run.durationSeconds = durationSeconds;
        run.lapisCorpses = Math.max(lapisPeak, lootedCorpses);
        run.campfiresUsed = campfiresUsed;
        run.mineshaftMayhem = mayhemPerk != null;
        run.mayhemPerk = mayhemPerk;
        run.skyMallBuff = skyMallPerk != null;
        run.skymallPerk = skyMallPerk;
        run.mineshaftEvent = event;
        run.frontLoaded = frontLoaded;
        run.efficiencyPercent = trackEfficiencyInSummary ? efficiency.current() : null;
        return run;
    }

    static boolean shouldPrintSummary(ShaftRun run) {
        if (run.finesCount <= 0.0) return false;
        if (!trackAllShaftSummaries) return true;
        return summaryShaftTypes.contains(run.summaryType().key());
    }

    static boolean shouldSaveRun(ShaftRun run) {
        return savedShaftTypes.contains(run.summaryType().key());
    }

    private static void checkCorrection(long now) {
        long lastSack = gems.lastSackMessageTime();
        if (lastSack > finalizeRequestedAt) {
            if (now - lastSack >= FINAL_SACK_QUIET_PERIOD_MS) applyCorrectedFines();
            return;
        }
        if (now >= awaitingCorrectionDeadline) {
            LOGGER.info("[ShaftSummary] AWAITING_CORRECTION timed out. Keeping estimated fines.");
            fullCleanup();
        }
    }

    private static void applyCorrectedFines() {
        ShaftRun run = awaitingCorrectionRun;
        if (run == null) {
            fullCleanup();
            return;
        }
        double corrected = gems.sessionFines();
        double estimated = awaitingCorrectionEstimatedFines;
        double delta = corrected - estimated;
        LOGGER.info("[ShaftSummary] Correction received. estimated={} corrected={} delta={}", estimated, corrected, delta);
        if (!runLog.isEmpty()) runLog.set(runLog.size() - 1, runLog.get(runLog.size() - 1) + String.format(Locale.ROOT, " | corrected=%.2f", corrected));
        if (Math.abs(delta) > 0.05) {
            int idx = -1;
            for (int i = 0; i < history.size(); i++) {
                ShaftRun it = history.get(i);
                if (it.timestamp == run.timestamp && it.finesCount == estimated) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                run.finesCount = corrected;
                run.totalGemstones = (int) (corrected * 80);
                history.set(idx, run);
                saveHistory();
                LOGGER.info("[ShaftSummary] History entry updated at index {}.", idx);
            }
            String oldStr = String.format(Locale.ROOT, "%.1f", estimated);
            String newStr = String.format(Locale.ROOT, "%.1f", corrected);
            if (!oldStr.equals(newStr)) patchChatSummaryFines(oldStr, newStr);
        }
        fullCleanup();
    }

    private static void cleanup() {
        state = State.IDLE;
        tracking = false;
        currentShaft = null;
        currentType = null;
        lootedCorpses = 0;
        finalizeRequestedAt = 0;
        runEndAt = 0;
        efficiency.reset();
        gems.endSession();
    }

    private static void fullCleanup() {
        awaitingCorrectionRun = null;
        awaitingCorrectionDeadline = 0;
        awaitingCorrectionEstimatedFines = 0.0;
        cleanup();
        gems.fullCleanup();
    }

    // ---- scraping ----

    private static void scrapeTablist(Minecraft client, List<String> tabLines) {
        Map<CorpseType, Integer> counts = MineshaftAutoParty.corpseCounts(client);
        Integer lapis = counts.get(CorpseType.LAPIS);
        if (lapis != null && lapis > lapisPeak) lapisPeak = lapis;
        if (lootedCorpses > lapisPeak) lapisPeak = lootedCorpses;

        String mayhem = MayhemHUD.getBuffText();
        if (mayhem != null) mayhemPerk = mayhem;
        SkyMallTracker.Buff skyMall = SkyMallTracker.current();
        if (skyMall != null) skyMallPerk = skyMall.displayName;
        for (String line : tabLines) {
            Matcher m = EVENT_LINE.matcher(line);
            if (!m.find()) continue;
            String found = normalizeEvent(m.group(1));
            if (found != null) event = found;
        }
    }

    private static String normalizeEvent(String raw) {
        String clean = raw.replaceAll("\\s+", " ").trim();
        if (clean.isEmpty() || clean.equalsIgnoreCase("None")) return null;
        if (clean.equalsIgnoreCase("Year of the Seal") || clean.equalsIgnoreCase("Year of the Pig")
            || clean.equalsIgnoreCase("Year of the Witch")) return null;
        for (String allowed : ALLOWED_EVENTS) {
            if (allowed.equalsIgnoreCase(clean)) return allowed;
        }
        return null;
    }

    // ---- chat in ----

    public static void onGameMessage(Component message) {
        String clean = message.getString().replaceAll("§.", "").trim();
        SkyMallTracker.onGameMessage(clean);
        if (gems.processMessage(message, clean)) {
            if (tracking && state == State.IN_SHAFT) startCounting();
            return;
        }
        if (state != State.IDLE && (clean.equals("LAPIS CORPSE LOOT!") || clean.equals("UMBER CORPSE LOOT!") || clean.equals("TUNGSTEN CORPSE LOOT!"))) {
            lootedCorpses++;
            if (lootedCorpses > lapisPeak) lapisPeak = lootedCorpses;
            LOGGER.info("[ShaftSummary] Counted corpse loot chat '{}' (total looted: {})", clean, lootedCorpses);
        }
        if (clean.contains("You placed a Campfire")) campfiresUsed++;
        if (trackEfficiencyInSummary) efficiency.onGameMessage(clean, blueCheese);
    }

    // ---- chat out ----

    private static void printRunToChat(ShaftRun run) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Font font = client.font;
        ShaftPbType type = run.summaryType();
        int rank = rankForRun(run);
        double profitRate = run.durationSeconds > 0 ? (double) run.estimatedProfit / run.durationSeconds * 3600.0 : 0.0;
        String profitStr = formatMoney(run.estimatedProfit);
        String profitRateStr = formatMoney((long) profitRate);
        String finesStr = String.format(Locale.ROOT, "%.1f", run.finesCount);
        String finesLeft = "Fines: " + finesStr;
        String timeLeft = "Time: " + run.durationSeconds + "s";
        int rightColumn = Math.max(font.width(finesLeft), font.width(timeLeft)) + font.width("   ");

        MqoChat.reply(type.colorCode() + "§l========================================");
        MqoChat.reply(type.colorCode() + "§l       " + type.summaryTitle());
        MqoChat.reply("");
        MqoChat.reply(finesComponent(font, finesStr, rank, rightColumn));
        MqoChat.reply("§aMoney: §6" + profitStr + " §e(" + profitRateStr + "/hr)");

        MutableComponent time = Component.literal("Time: ").withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(run.durationSeconds + "s").withStyle(ChatFormatting.WHITE));
        if (run.efficiencyPercent != null) {
            time.append(Component.literal(padding(font, timeLeft, rightColumn)))
                .append(Component.literal("Eff: ").withStyle(ChatFormatting.DARK_GREEN))
                .append(Component.literal(formatEfficiency(run.efficiencyPercent)).withColor(efficiencyColor(run.efficiencyPercent)));
        }
        MqoChat.reply(time);
        MqoChat.reply("§9Corpses: §f" + run.lapisCorpses);
        MqoChat.reply("§6Campfires Used: §f" + run.campfiresUsed);

        if (showBuffsInSummary) {
            StringBuilder buffs = new StringBuilder();
            if (run.frontLoaded) buffs.append("§aFront Loaded ");
            if (run.mineshaftMayhem) buffs.append("§cMayhem (").append(run.mayhemPerk == null ? "Active" : run.mayhemPerk).append(") ");
            if (run.skyMallBuff) buffs.append("§bSkyMall (").append(run.skymallPerk == null ? "Active" : run.skymallPerk).append(") ");
            if (run.mineshaftEvent != null) buffs.append("§e").append(run.mineshaftEvent).append(' ');
            String text = buffs.toString().trim();
            if (!text.isEmpty()) {
                MqoChat.reply("");
                MqoChat.reply(type.colorCode() + "Buffs: " + text);
            }
        }
        MqoChat.reply(type.colorCode() + "§l========================================");
    }

    private static MutableComponent finesComponent(Font font, String finesStr, int rank, int rightColumn) {
        return Component.literal("Fines: ").withStyle(ChatFormatting.AQUA)
            .append(Component.literal(finesStr).withStyle(ChatFormatting.WHITE))
            .append(Component.literal(padding(font, "Fines: " + finesStr, rightColumn)))
            .append(Component.literal("Rank: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("#" + rank).withStyle(ChatFormatting.YELLOW));
    }

    /**
     * Rewrites the printed Fines line in place once the late sack message corrects it.
     * Only the number sibling is swapped; the padding and rank stay as printed.
     */
    private static void patchChatSummaryFines(String oldFinesStr, String newFinesStr) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui == null) return;
        try {
            ChatComponentAccessor chat = (ChatComponentAccessor) client.gui.getChat();
            List<GuiMessage> all = chat.miningqol$allMessages();
            String needle = "Fines: " + oldFinesStr;
            for (int i = 0; i < all.size(); i++) {
                GuiMessage msg = all.get(i);
                Component content = msg.content();
                if (!content.getString().replaceAll("§.", "").contains(needle)) continue;
                MutableComponent replacement = Component.literal(content.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents plain ? plain.text() : "")
                    .setStyle(content.getStyle());
                for (Component sibling : content.getSiblings()) {
                    String sib = sibling.getString().replaceAll("§.", "");
                    if (sib.equals(oldFinesStr)) {
                        replacement.append(Component.literal(newFinesStr).setStyle(sibling.getStyle()));
                    } else {
                        replacement.append(sibling);
                    }
                }
                all.set(i, new GuiMessage(msg.addedTime(), replacement, msg.signature(), msg.source(), msg.tag()));
                chat.miningqol$refreshTrimmedMessages();
                LOGGER.info("[ShaftSummary] Chat patched: Fines {} -> {}", oldFinesStr, newFinesStr);
                return;
            }
        } catch (Exception e) {
            LOGGER.warn("[ShaftSummary] Failed to patch chat summary", e);
        }
    }

    public static String buildPartyMessage(ShaftRun run) {
        String finesStr = String.format(Locale.ROOT, "%.1f", run.finesCount);
        double profitRate = run.durationSeconds > 0 ? (double) run.estimatedProfit / run.durationSeconds * 3600.0 : 0.0;
        String rateStr = String.format(Locale.ROOT, "%.2f", profitRate / 1_000_000.0);
        return "I mined " + finesStr + " Fines ($" + rateStr + "M/h) in a " + run.lapisCorpses + "l "
            + run.campfiresUsed + "c " + run.summaryType().partyLabel() + "!";
    }

    private static void sendToParty(ShaftRun run) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.connection.sendCommand("pc " + buildPartyMessage(run));
    }

    static String formatMoney(long amount) {
        if (amount >= 1_000_000L) return String.format(Locale.ROOT, "%.2fM", amount / 1_000_000.0);
        if (amount >= 1_000L) return String.format(Locale.ROOT, "%.0fk", amount / 1_000.0);
        return String.valueOf(amount);
    }

    private static String formatEfficiency(float value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private static int efficiencyColor(float value) {
        float clamped = Math.max(0f, Math.min(100f, value));
        float hue = 1.2f * clamped / 360f;
        float curve = 50f - (clamped - 50f) * (clamped - 50f) / 125f;
        float brightness = Math.max(0.4f, Math.min(1f, curve / 50f));
        return Color.HSBtoRGB(hue, 1f, brightness) & 0xFFFFFF;
    }

    private static String padding(Font font, String leftText, int targetWidth) {
        int spaceWidth = Math.max(font.width(" "), 1);
        int paddingWidth = Math.max(targetWidth - font.width(leftText), spaceWidth);
        int spaces = (paddingWidth + spaceWidth - 1) / spaceWidth;
        return " ".repeat(spaces);
    }

    // ---- history & ranking ----

    private static int rankForRun(ShaftRun run) {
        List<ShaftRun> comparable = new ArrayList<>();
        for (ShaftRun other : history) {
            if (isComparable(other, run)) comparable.add(other);
        }
        comparable.add(run);
        comparable.sort(Comparator.comparingDouble((ShaftRun r) -> r.finesCount).reversed());
        return comparable.indexOf(run) + 1;
    }

    private static boolean isComparable(ShaftRun r1, ShaftRun r2) {
        if (r1.summaryType() != r2.summaryType()) return false;
        if (ranksBy.isEmpty()) return true;
        if (ranksBy.contains("lapis") && r1.lapisCorpses != r2.lapisCorpses) return false;
        if (ranksBy.contains("mayhem") && !java.util.Objects.equals(r1.mayhemPerk, r2.mayhemPerk)) return false;
        if (ranksBy.contains("skymall") && !java.util.Objects.equals(r1.skymallPerk, r2.skymallPerk)) return false;
        if (ranksBy.contains("event") && !java.util.Objects.equals(r1.mineshaftEvent, r2.mineshaftEvent)) return false;
        return !ranksBy.contains("campfires") || r1.campfiresUsed == r2.campfiresUsed;
    }

    private static void loadHistory() {
        history.clear();
        if (!HISTORY_FILE.exists()) return;
        try (FileReader reader = new FileReader(HISTORY_FILE)) {
            List<ShaftRun> runs = GSON.fromJson(reader, new TypeToken<List<ShaftRun>>() {}.getType());
            if (runs != null) {
                for (ShaftRun run : runs) {
                    if (run != null) history.add(run);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[ShaftSummary] Failed to load run history", e);
        }
    }

    private static void saveHistory() {
        trimHistory();
        try {
            HISTORY_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
                GSON.toJson(history, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[ShaftSummary] Failed to save run history", e);
        }
    }

    /** Drops the weakest runs first, never touching the ten most recent. */
    private static void trimHistory() {
        if (maxSavedRuns <= 0) {
            history.clear();
            return;
        }
        while (history.size() > maxSavedRuns) {
            Integer idx = selectTrimIndex();
            if (idx == null) break;
            history.remove((int) idx);
        }
    }

    private static Integer selectTrimIndex() {
        List<Integer> byNewest = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) byNewest.add(i);
        byNewest.sort(Comparator.comparingLong((Integer i) -> history.get(i).timestamp).reversed());
        Set<Integer> protectedIdx = new LinkedHashSet<>(byNewest.subList(0, Math.min(PROTECTED_RECENT_RUN_COUNT, byNewest.size())));
        Integer best = null;
        for (int i = 0; i < history.size(); i++) {
            if (protectedIdx.contains(i)) continue;
            if (best == null) {
                best = i;
                continue;
            }
            ShaftRun a = history.get(i), b = history.get(best);
            int cmp = Double.compare(a.finesCount, b.finesCount);
            if (cmp == 0) cmp = Long.compare(a.timestamp, b.timestamp);
            if (cmp < 0) best = i;
        }
        return best;
    }

    public static int savedRunCount() {
        return history.size();
    }

    // ---- commands ----

    public static void printStatus() {
        Minecraft client = Minecraft.getInstance();
        String sidebar = MineshaftAutoParty.sidebarText(client);
        boolean says = MineshaftAutoParty.saysMineshaft(sidebar);
        ShaftType onBoard = sidebar == null ? null : ShaftType.fromScoreboard(sidebar);
        MqoChat.reply("§6[Shaft Summary] §7enabled=§f" + enabled + " §7sidebar=§f" + (sidebar == null ? "none" : "yes")
            + " §7saysMineshaft=§f" + says + " §7idOnBoard=§f" + (onBoard == null ? "-" : onBoard.name())
            + " §7stable=§f" + typeStableScans + " §7missed=§f" + missedScans
            + " §7wouldTrack=§f" + shouldTrackMineshaft(onBoard));
        MqoChat.reply("§6[Shaft Summary] §7state=§f" + state + " §7shaft=§f" + (currentShaft == null ? "-" : currentShaft.displayName())
            + " §7fines=§f" + String.format(Locale.ROOT, "%.2f", gems.sessionFines()) + " §7coins=§f" + formatMoney(gems.sessionProfit())
            + " §7corpses=§f" + Math.max(lapisPeak, lootedCorpses) + " §7campfires=§f" + campfiresUsed);
        MqoChat.reply("§6[Shaft Summary] §7mayhem=§f" + mayhemPerk + " §7skymall=§f" + SkyMallTracker.current()
            + " §7event=§f" + event + " §7eff=§f" + efficiency.current() + " §7prices=§f" + BazaarPrices.hasPrices()
            + " §7history=§f" + history.size());
    }

    /** {@code /shaftsummary debug}: how the fines of the current and recent runs were reached. */
    public static void printDebug() {
        MqoChat.reply("§6[Shaft Summary] §7runsStarted=§f" + runsStarted + " §7state=§f" + state);
        if (state != State.IDLE) MqoChat.reply("§6[Shaft Summary] §7live: §f" + gems.accounting());
        if (runLog.isEmpty()) MqoChat.reply("§6[Shaft Summary] §7No finished runs this session.");
        for (String line : runLog) MqoChat.reply("§6[Shaft Summary] §f" + line);
    }

    public static void printSample(Minecraft client) {
        ShaftRun run = new ShaftRun();
        run.timestamp = System.currentTimeMillis();
        run.shaftTypeId = ShaftPbType.JASPER.key();
        run.finesCount = 12.3;
        run.totalGemstones = (int) (run.finesCount * 80);
        run.estimatedProfit = 4_200_000L;
        run.durationSeconds = 312;
        run.lapisCorpses = 2;
        run.campfiresUsed = 1;
        run.mineshaftMayhem = true;
        run.mayhemPerk = "+200 Mining Speed";
        run.skyMallBuff = true;
        run.skymallPerk = "Mining Fortune";
        run.frontLoaded = frontLoaded;
        run.efficiencyPercent = trackEfficiencyInSummary ? 87.5f : null;
        printRunToChat(run);
    }

    public static void sendLastToParty() {
        ShaftRun run = awaitingCorrectionRun != null ? awaitingCorrectionRun : (history.isEmpty() ? null : history.get(history.size() - 1));
        if (run == null) {
            MqoChat.reply("§6[Shaft Summary] §7No finished run yet.");
            return;
        }
        sendToParty(run);
    }

    // ---- settings ----

    public static Set<String> defaultSummaryKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (ShaftPbType t : ShaftPbType.values()) keys.add(t.key());
        return keys;
    }

    public static Set<String> defaultSavedKeys() {
        return new LinkedHashSet<>(List.of(ShaftPbType.JASPER.key(), ShaftPbType.JASPER_CRYSTAL.key()));
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isFrontLoaded() { return frontLoaded; }
    public static void setFrontLoaded(boolean value) { frontLoaded = value; }
    public static boolean isTrackAllShaftSummaries() { return trackAllShaftSummaries; }
    public static void setTrackAllShaftSummaries(boolean value) { trackAllShaftSummaries = value; }
    public static boolean isShowBuffsInSummary() { return showBuffsInSummary; }
    public static void setShowBuffsInSummary(boolean value) { showBuffsInSummary = value; }
    public static boolean isTrackEfficiencyInSummary() { return trackEfficiencyInSummary; }
    public static void setTrackEfficiencyInSummary(boolean value) { trackEfficiencyInSummary = value; }
    public static int getEfficiencyPauseSeconds() { return efficiencyPauseSeconds; }
    public static void setEfficiencyPauseSeconds(int value) { efficiencyPauseSeconds = Math.max(1, Math.min(120, value)); }
    public static boolean isAutoSave() { return autoSave; }
    public static void setAutoSave(boolean value) { autoSave = value; }
    public static boolean isPrintToChat() { return printToChat; }
    public static void setPrintToChat(boolean value) { printToChat = value; }
    public static boolean isPrintToParty() { return printToParty; }
    public static void setPrintToParty(boolean value) { printToParty = value; }
    public static int getMaxSavedRuns() { return maxSavedRuns; }
    public static void setMaxSavedRuns(int value) { maxSavedRuns = Math.max(0, Math.min(1000, value)); }
    public static Set<String> getSummaryShaftTypes() { return new LinkedHashSet<>(summaryShaftTypes); }
    public static void setSummaryShaftTypes(java.util.Collection<String> keys) { summaryShaftTypes.clear(); if (keys != null) summaryShaftTypes.addAll(keys); }
    public static boolean isSummaryType(ShaftPbType type) { return summaryShaftTypes.contains(type.key()); }
    public static void setSummaryType(ShaftPbType type, boolean on) { if (on) summaryShaftTypes.add(type.key()); else summaryShaftTypes.remove(type.key()); }
    public static Set<String> getSavedShaftTypes() { return new LinkedHashSet<>(savedShaftTypes); }
    public static void setSavedShaftTypes(java.util.Collection<String> keys) { savedShaftTypes.clear(); if (keys != null) savedShaftTypes.addAll(keys); }
    public static boolean isSavedType(ShaftPbType type) { return savedShaftTypes.contains(type.key()); }
    public static void setSavedType(ShaftPbType type, boolean on) { if (on) savedShaftTypes.add(type.key()); else savedShaftTypes.remove(type.key()); }
    public static Set<String> getRanksBy() { return new LinkedHashSet<>(ranksBy); }
    public static void setRanksBy(java.util.Collection<String> keys) { ranksBy.clear(); if (keys != null) ranksBy.addAll(keys); }
    public static boolean isRankBy(String criterion) { return ranksBy.contains(criterion); }
    public static void setRankBy(String criterion, boolean on) { if (on) ranksBy.add(criterion); else ranksBy.remove(criterion); }
    public static int getGemMiningSpeed() { return gemMiningSpeed; }
    public static void setGemMiningSpeed(int value) { gemMiningSpeed = Math.max(0, Math.min(50_000, value)); }
    public static boolean isBlueCheese() { return blueCheese; }
    public static void setBlueCheese(boolean value) { blueCheese = value; }
    public static boolean isValueAsFlawless() { return valueAsFlawless; }
    public static void setValueAsFlawless(boolean value) { valueAsFlawless = value; }
    public static boolean isSellOffer() { return sellOffer; }
    public static void setSellOffer(boolean value) { sellOffer = value; }
}
