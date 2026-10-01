package forfun.miningqol.client.summary;

import forfun.miningqol.client.party.MineshaftAutoParty;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which Sky Mall buff is in force, read off the chat lines Hypixel prints when it rolls
 * and off the Heart of the Mountain menu.
 *
 * <p>A buff lasts until the next SkyBlock midnight: the scoreboard clock says how far
 * off that is (a SkyBlock day is 20 real minutes, so at most 20 minutes), and the
 * expiry is resynced against it while the clock is visible. It is also dropped five
 * minutes after the player leaves every mining island, and only reported while the
 * scoreboard confirms a mining island.
 */
public final class SkyMallTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningQOL");
    private static final long DEFAULT_MS_UNTIL_MIDNIGHT = 1_200_000L;
    private static final long ISLAND_CHECK_MS = 5_000L;
    private static final long LEAVE_ISLAND_GRACE_MS = 300_000L;
    private static final long RESYNC_TOLERANCE_MS = 5_000L;
    private static final long MENU_SCAN_INTERVAL_MS = 10_000L;
    /** Real seconds per SkyBlock minute: 1200 s / 1440 min. */
    private static final double SECONDS_PER_GAME_MINUTE = 1200.0 / 1440.0;
    private static final Pattern TIME_LINE = Pattern.compile("(?:Time:\\s*)?(\\d{1,2}):(\\d{2})\\s*(am|pm)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COOLDOWN_PCT = Pattern.compile("(?:20|25)\\s*%");
    private static final List<String> MINING_ISLAND_WORDS = List.of(
        "dwarven mines", "dwarven village", "the lift", "the forge", "crystal hollows", "mithril deposits",
        "jungle", "precursor ruins", "goblin holdout", "crystal nucleus", "glacite tunnels", "dwarven base camp",
        "mineshaft", "gold mine", "spider's den", "the end", "crimson isle", "mining");

    public enum Buff {
        MINING_SPEED("Mining Speed"),
        MINING_FORTUNE("Mining Fortune"),
        MORE_POWDER("More Powder"),
        ABILITY_COOLDOWN("Ability Cooldown"),
        GOBLIN_CHANCE("Goblin Chance"),
        TITANIUM_DROPS("Titanium Drops");

        public final String displayName;

        Buff(String displayName) {
            this.displayName = displayName;
        }
    }

    private static long activeUntilMs;
    private static Buff currentBuff;
    private static boolean initialized;
    private static int tickCounter;
    private static long lastMiningIslandMs;
    private static long currentMsUntilMidnight = DEFAULT_MS_UNTIL_MIDNIGHT;
    private static boolean checkingIsland;
    private static long checkIslandUntilMs;
    private static boolean onMiningIsland;
    private static boolean hideUntilMiningConfirmed;
    private static Buff sessionCarryBuff;
    private static long sessionCarryUntilMs;
    private static long lastMenuScanMs;

    private SkyMallTracker() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        triggerIslandCheck(false);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            long now = System.currentTimeMillis();
            Buff carry = now < sessionCarryUntilMs ? sessionCarryBuff : null;
            clearRuntimeState();
            if (carry != null) {
                currentBuff = carry;
                activeUntilMs = sessionCarryUntilMs;
                lastMiningIslandMs = now;
            }
            triggerIslandCheck(true);
        });
    }

    public static void tick(Minecraft client) {
        if (++tickCounter >= 20) {
            tickCounter = 0;
            checkScoreboard(client, MineshaftAutoParty.isInMineshaft());
        }
        long now = System.currentTimeMillis();
        if (checkingIsland && now > checkIslandUntilMs) {
            checkingIsland = false;
            onMiningIsland = false;
        }
        if (onMiningIsland) lastMiningIslandMs = now;
        if (activeUntilMs > 0) {
            if (now > activeUntilMs) {
                reset();
            } else if (!onMiningIsland && now - lastMiningIslandMs > LEAVE_ISLAND_GRACE_MS) {
                reset();
            }
        }
        scanMenu(client, now);
    }

    /** Set by "Your Sky Mall buff changed!"; the next "New buff:" line is the Sky Mall one. */
    private static long expectingBuffUntilMs;
    private static final long EXPECT_WINDOW_MS = 15_000L;

    public static void onGameMessage(String cleanLine) {
        String lower = cleanLine.toLowerCase(Locale.ROOT);
        if (lower.contains("sending to server")) triggerIslandCheck(true);
        long now = System.currentTimeMillis();

        // Hypixel announces a reroll as two lines: "Your Sky Mall buff changed!" and then
        // "New buff: Gain +100 Mining Speed." The second also announces Lottery and
        // Beekeeper buffs, so it only counts as Sky Mall right after the first, or when
        // it plainly names a mining buff.
        if (lower.contains("your sky mall buff changed")) {
            expectingBuffUntilMs = now + EXPECT_WINDOW_MS;
            LOGGER.info("[ShaftSummary] Sky Mall buff changed; waiting for the New buff line.");
            return;
        }
        if (lower.startsWith("new buff:")) {
            boolean expected = now < expectingBuffUntilMs;
            Buff detected = detectBuffKeywords(lower);
            if (detected != null && (expected || !looksLikeOtherPerk(lower))) {
                expectingBuffUntilMs = 0;
                LOGGER.info("[ShaftSummary] Skymall detection method=chat buff={} text={}", detected.name(), cleanLine);
                forceSetBuff(detected);
            } else if (expected) {
                expectingBuffUntilMs = 0;
                LOGGER.info("[ShaftSummary] Sky Mall New buff line not recognised: '{}'", cleanLine);
            }
            return;
        }

        if (isSkyMallLine(lower)) LOGGER.info("[ShaftSummary] Sky Mall line: '{}'", cleanLine);
        Buff detected = detectBuff(lower);
        if (detected != null) {
            LOGGER.info("[ShaftSummary] Skymall detection method=chat buff={} text={}", detected.name(), cleanLine);
            forceSetBuff(detected);
            return;
        }
        if (isRerollLine(lower)) reset();
    }

    /** Lottery / Beekeeper "New buff" lines name foraging stats, never mining ones. */
    private static boolean looksLikeOtherPerk(String lower) {
        return lower.contains("foraging") || lower.contains("lottery") || lower.contains("beekeeper") || lower.contains(" bee");
    }

    /** The buff in force, or null when expired or not confirmed to be on a mining island. */
    public static Buff current() {
        long now = System.currentTimeMillis();
        if (now >= activeUntilMs) return null;
        if (!isBuffVisible(now)) return null;
        return currentBuff;
    }

    private static Buff rawCurrent() {
        return System.currentTimeMillis() < activeUntilMs ? currentBuff : null;
    }

    private static boolean isBuffVisible(long now) {
        if (hideUntilMiningConfirmed) return onMiningIsland;
        return onMiningIsland || (checkingIsland && now <= checkIslandUntilMs);
    }

    private static void forceSetBuff(Buff buff) {
        long now = System.currentTimeMillis();
        currentBuff = buff;
        activeUntilMs = now + currentMsUntilMidnight;
        onMiningIsland = true;
        lastMiningIslandMs = now;
        hideUntilMiningConfirmed = false;
        sessionCarryBuff = buff;
        sessionCarryUntilMs = activeUntilMs;
    }

    private static void reset() {
        clearRuntimeState();
        sessionCarryBuff = null;
        sessionCarryUntilMs = 0;
    }

    private static void clearRuntimeState() {
        activeUntilMs = 0;
        currentBuff = null;
        onMiningIsland = false;
        lastMiningIslandMs = 0;
        hideUntilMiningConfirmed = false;
    }

    private static void triggerIslandCheck(boolean hideUntilConfirmed) {
        checkingIsland = true;
        checkIslandUntilMs = System.currentTimeMillis() + ISLAND_CHECK_MS;
        onMiningIsland = false;
        hideUntilMiningConfirmed = hideUntilConfirmed;
    }

    private static void checkScoreboard(Minecraft client, boolean fallbackMiningIsland) {
        String sidebar = MineshaftAutoParty.sidebarText(client);
        boolean foundTimeLine = false;
        boolean foundMiningIsland = false;
        Long msUntilMidnight = null;
        if (sidebar != null) {
            for (String raw : sidebar.split("\n")) {
                String line = raw.toLowerCase(Locale.ROOT);
                if (checkingIsland && (isMiningIsland(line) || containsShaftId(raw))) foundMiningIsland = true;
                Matcher m = TIME_LINE.matcher(line);
                if (!m.find()) continue;
                foundTimeLine = true;
                int hour, minute;
                try {
                    hour = Integer.parseInt(m.group(1));
                    minute = Integer.parseInt(m.group(2));
                } catch (NumberFormatException e) {
                    continue;
                }
                boolean pm = m.group(3).equalsIgnoreCase("pm");
                int hr24 = hour;
                if (!pm && hour == 12) hr24 = 0;
                if (pm && hour != 12) hr24 += 12;
                int minutesUntilMidnight = 1440 - (hr24 * 60 + minute);
                msUntilMidnight = (long) (minutesUntilMidnight * SECONDS_PER_GAME_MINUTE * 1000.0);
            }
        }
        if (msUntilMidnight != null) currentMsUntilMidnight = msUntilMidnight;
        if (checkingIsland && (foundMiningIsland || fallbackMiningIsland)) {
            onMiningIsland = true;
            checkingIsland = false;
            hideUntilMiningConfirmed = false;
        }
        if (foundTimeLine && activeUntilMs > 0) {
            long synced = System.currentTimeMillis() + currentMsUntilMidnight;
            if (Math.abs(activeUntilMs - synced) > RESYNC_TOLERANCE_MS) activeUntilMs = synced;
        }
    }

    private static boolean isMiningIsland(String lower) {
        for (String word : MINING_ISLAND_WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    private static boolean containsShaftId(String line) {
        return forfun.miningqol.client.party.ShaftType.fromScoreboard(line) != null;
    }

    // ---- chat parsing ----

    private static boolean isSkyMallLine(String lower) {
        return lower.contains("skymall") || lower.contains("sky mall") || lower.contains("new buff");
    }

    private static Buff detectBuff(String lower) {
        if (!isSkyMallLine(lower)) return null;
        return detectBuffKeywords(lower);
    }

    private static Buff detectBuffKeywords(String lower) {
        if (lower.contains("mining speed")) return Buff.MINING_SPEED;
        if (lower.contains("mining fortune")) return Buff.MINING_FORTUNE;
        if (lower.contains("more powder")) return Buff.MORE_POWDER;
        if (isAbilityCooldown(lower)) return Buff.ABILITY_COOLDOWN;
        if (lower.contains("goblins")) return Buff.GOBLIN_CHANCE;
        if (lower.contains("titanium drops")) return Buff.TITANIUM_DROPS;
        return null;
    }

    private static boolean isAbilityCooldown(String lower) {
        return lower.contains("pickaxe ability cooldown") || lower.contains("ability cooldown")
            || lower.contains("pickaxe ability cd") || lower.contains("ability cd");
    }

    private static boolean isRerollLine(String lower) {
        if (!isSkyMallLine(lower)) return false;
        if (isAbilityCooldown(lower) && COOLDOWN_PCT.matcher(lower).find()) return false;
        return lower.contains("reroll") || lower.contains("new buff") || lower.contains("active buff")
            || lower.contains("today's buff") || lower.contains("todays buff");
    }

    // ---- Heart of the Mountain menu ----

    private static void scanMenu(Minecraft client, long now) {
        Screen screen = client.screen;
        if (!(screen instanceof AbstractContainerScreen<?> container)) return;
        if (now - lastMenuScanMs < MENU_SCAN_INTERVAL_MS) return;
        String title = container.getTitle().getString().replaceAll("§.", "");
        if (!title.toLowerCase(Locale.ROOT).contains("heart of the mountain")) return;
        int scanned = 0;
        String skyMallItem = null;
        for (Slot slot : container.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            scanned++;
            List<String> texts = searchText(stack);
            boolean isSkyMall = texts.get(0).replaceAll("§.", "").toLowerCase(Locale.ROOT).contains("sky mall");
            if (isSkyMall) skyMallItem = String.join(" | ", texts);
            if (isSkyMall && isPerkDisabled(texts)) {
                // A loadout without Sky Mall: the perk is off, so no buff applies.
                if (currentBuff != null) LOGGER.info("[ShaftSummary] Sky Mall perk is DISABLED in this loadout; clearing buff.");
                reset();
                lastMenuScanMs = now;
                return;
            }
            Buff detected = detectMenuBuff(texts);
            if (detected == null) continue;
            if (rawCurrent() != detected) {
                LOGGER.info("[ShaftSummary] Skymall detection method=hotm_menu buff={}", detected.name());
                forceSetBuff(detected);
            }
            lastMenuScanMs = now;
            return;
        }
        // Menu is still filling in (the server sends slots over a few ticks) or has no
        // readable Sky Mall perk: log once the menu looks complete so a miss can be diagnosed.
        if (scanned >= 40) {
            lastMenuScanMs = now;
            LOGGER.info("[ShaftSummary] HOTM scanned {} items, no Sky Mall buff read. Sky Mall item: {}", scanned,
                skyMallItem == null ? "not found" : skyMallItem.replaceAll("§.", ""));
        }
    }

    // ---- config round-trip: the buff and its expiry survive a restart ----

    public static String storedBuffName() {
        return currentBuff == null ? "" : currentBuff.name();
    }

    public static long storedUntil() {
        return activeUntilMs;
    }

    public static void restore(String buffName, long until) {
        if (buffName == null || buffName.isEmpty() || until <= System.currentTimeMillis()) return;
        try {
            currentBuff = Buff.valueOf(buffName);
        } catch (IllegalArgumentException e) {
            return;
        }
        activeUntilMs = until;
        sessionCarryBuff = currentBuff;
        sessionCarryUntilMs = until;
        lastMiningIslandMs = System.currentTimeMillis();
        LOGGER.info("[ShaftSummary] Restored Sky Mall buff {} (expires in {}s)", currentBuff.name(), (until - System.currentTimeMillis()) / 1000);
    }

    private static List<String> searchText(ItemStack stack) {
        List<String> texts = new ArrayList<>(8);
        texts.add(stack.getHoverName().getString());
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) texts.add(line.getString());
        }
        return texts;
    }

    /** The perk lore carries a bare "ENABLED" or "DISABLED" line. */
    private static boolean isPerkDisabled(List<String> texts) {
        for (String line : texts) {
            String clean = line.replaceAll("§.", "").trim();
            if (clean.equalsIgnoreCase("DISABLED")) return true;
        }
        return false;
    }

    static Buff detectMenuBuff(List<String> texts) {
        if (texts.isEmpty()) return null;
        String title = texts.get(0).replaceAll("§.", "");
        if (!title.toLowerCase(Locale.ROOT).contains("sky mall")) return null;
        List<String> lines = new ArrayList<>();
        for (int i = 1; i < texts.size(); i++) lines.add(texts.get(i).replaceAll("§.", ""));
        int header = -1;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i).toLowerCase(Locale.ROOT);
            if (l.contains("your current effect") || l.contains("active buff") || l.contains("today's buff") || l.contains("todays buff")) {
                header = i;
                break;
            }
        }
        if (header < 0) return null;
        StringBuilder blob = new StringBuilder(lines.get(header).trim());
        int taken = 1;
        for (int i = header + 1; i < lines.size() && taken < 3; i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) break;
            blob.append(" | ").append(line);
            taken++;
        }
        String effect = blob.toString().toLowerCase(Locale.ROOT);
        if (effect.contains("mining speed")) return Buff.MINING_SPEED;
        if (effect.contains("mining fortune")) return Buff.MINING_FORTUNE;
        if (effect.contains("more powder") || effect.contains("powder")) return Buff.MORE_POWDER;
        if (effect.contains("pickaxe ability cooldown") || effect.contains("ability cooldown")) return Buff.ABILITY_COOLDOWN;
        if (effect.contains("goblins")) return Buff.GOBLIN_CHANCE;
        if (effect.contains("titanium")) return Buff.TITANIUM_DROPS;
        return null;
    }
}
