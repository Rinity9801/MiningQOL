package forfun.miningqol.client;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Commission stats for the Commission HUD: total commissions completed (persisted
 * in the config, reset via /commtrack reset) and a session comms/hour rate.
 *
 * Completions are counted from Hypixel's "Commission Complete!" chat line, read in
 * SystemChatMixin so chat-filter mods can't hide it first. Like the
 * collection tracker, the rate clock pauses instead of diluting: the gap between
 * two completions only counts up to MAX_GAP_MS, so AFK time doesn't sink the rate,
 * and the displayed rate stays frozen until the next completion.
 */
public class CommTracker {
    private static final long MAX_GAP_MS = 15 * 60 * 1000;

    private static boolean statsEnabled = false;
    private static long totalCompleted = 0;

    // Session rate: (completions - 1) over the ACTIVE time between completions.
    private static int sessionCompleted = 0;
    private static long activeMillis = 0;
    private static long lastCompletionAt = 0;

    private CommTracker() {}

    public static boolean isStatsEnabled() { return statsEnabled; }
    public static void setStatsEnabled(boolean value) { statsEnabled = value; }

    public static long getTotalCompleted() { return totalCompleted; }
    public static void setTotalCompleted(long value) { totalCompleted = Math.max(0, value); }

    /**
     * Whether a system line is Hypixel's own completion notice ("COMMISSION COMPLETE! Mithril
     * Miner" / "Mithril Miner Commission Complete!"). Player, party and guild chat arrive as
     * system lines too, but always as "name: text", so a colon before the phrase rules it out.
     */
    public static boolean isCompletionLine(String message) {
        String clean = message.replaceAll("§.", "").toLowerCase(Locale.ROOT);
        int idx = clean.indexOf("commission complete");
        return idx >= 0 && clean.lastIndexOf(':', idx) < 0;
    }

    public static void onChatMessage(String message) {
        if (!isCompletionLine(message)) return;

        long now = System.currentTimeMillis();
        totalCompleted++;
        sessionCompleted++;
        if (lastCompletionAt > 0) {
            activeMillis += Math.min(now - lastCompletionAt, MAX_GAP_MS);
        }
        lastCompletionAt = now;
    }

    /** Comms/hour over active time; negative until two completions this session. */
    public static double getCommsPerHour() {
        if (sessionCompleted < 2 || activeMillis <= 0) return -1;
        return (sessionCompleted - 1) / (activeMillis / 3_600_000.0);
    }

    public static int getSessionCompleted() { return sessionCompleted; }

    /** True when the rate clock is paused (no completion for longer than the gap cap). */
    public static boolean isPaused() {
        return sessionCompleted > 0 && lastCompletionAt > 0
            && System.currentTimeMillis() - lastCompletionAt > MAX_GAP_MS;
    }

    /** /commtrack reset — clears the persisted total and the session rate. */
    public static void reset() {
        totalCompleted = 0;
        sessionCompleted = 0;
        activeMillis = 0;
        lastCompletionAt = 0;
        // Persist now, so a crash before the next config save can't bring the old total back.
        MiningqolClient.saveConfig();
        // reply, not log: command output shows even with mod chat messages off.
        MqoChat.reply(Component.literal("§6[MQO] §aCommission stats reset"));
    }
}
