package forfun.miningqol.client.summary;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How close to the theoretical maximum the player mined during a shaft.
 *
 * <p>Every gem block that breaks is a "real" mine. In parallel, a simulated miner with
 * the player's tab-list Mining Speed breaks the same gem type back to back and each of
 * its breaks is a "max" mine. Efficiency is real / max. The simulation pauses after
 * {@link #pauseSeconds} without a break, so walking between veins is not penalised.
 */
public final class ShaftEfficiency {
    private static final Pattern MINING_SPEED = Pattern.compile("Mining Speed.*?([\\d,]+)");
    private static final long HIT_EXPIRY_MS = 2_000L;

    private static final Map<String, Integer> BLOCK_STRENGTH = Map.ofEntries(
        Map.entry("Ruby", 2300),
        Map.entry("Amethyst", 3000),
        Map.entry("Jade", 3000),
        Map.entry("Sapphire", 3000),
        Map.entry("Amber", 3000),
        Map.entry("Opal", 3000),
        Map.entry("Topaz", 3800),
        Map.entry("Jasper", 4800),
        Map.entry("Aquamarine", 5200),
        Map.entry("Citrine", 5200),
        Map.entry("Peridot", 5200),
        Map.entry("Onyx", 5200)
    );

    private record HitBlock(String gem, long time) {}

    private boolean tracking;
    private int msbTicks;
    private int currentBreakTick;
    private int tablistMiningSpeed;
    private long lastMinedTime;
    private String lastGemType;
    private float efficiency;
    private int tickCounter;
    private final Map<String, Integer> maxMined = new HashMap<>();
    private final Map<String, Integer> realMined = new HashMap<>();
    private final Map<BlockPos, HitBlock> hitBlocks = new HashMap<>();

    public void reset() {
        tracking = false;
        msbTicks = 0;
        currentBreakTick = 0;
        tablistMiningSpeed = 0;
        lastMinedTime = 0;
        lastGemType = null;
        efficiency = 0f;
        maxMined.clear();
        realMined.clear();
        hitBlocks.clear();
    }

    /** Current efficiency, or null while nothing has been mined yet. */
    public Float current() {
        if (!hasData(realMined) && !hasData(maxMined)) return null;
        return calculate();
    }

    private static boolean hasData(Map<String, Integer> counts) {
        for (int v : counts.values()) {
            if (v > 0) return true;
        }
        return false;
    }

    public void onTick(Minecraft client, List<String> tabLines, int pauseSeconds, boolean blueCheese, int configSpeed) {
        tickCounter++;
        if (efficiency > 200f || tickCounter % 20 == 0) {
            int speed = parseMiningSpeed(tabLines);
            if (speed > 0) tablistMiningSpeed = speed;
        }
        pollHitBlocks(client);
        if (!tracking) return;

        long now = System.currentTimeMillis();
        if (now - lastMinedTime > pauseSeconds * 1000L) {
            currentBreakTick = 0;
            lastGemType = null;
            hitBlocks.clear();
            if (msbTicks > 0) msbTicks--;
            calculate();
            return;
        }

        String target = lastGemType;
        if (target != null) {
            currentBreakTick++;
            int multiplier = msbTicks > 0 ? 4 : 1;
            int speed = configSpeed > 0 ? configSpeed : tablistMiningSpeed;
            int ticksToBreak = ticksFor(target, speed * multiplier);
            if (currentBreakTick >= ticksToBreak) {
                currentBreakTick -= ticksToBreak;
                maxMined.merge(target, 1, Integer::sum);
            }
        }
        if (msbTicks > 0) msbTicks--;
        calculate();
    }

    /** Watches the blocks the player has swung at; one turning to air is a mined block. */
    private void pollHitBlocks(Minecraft client) {
        if (hitBlocks.isEmpty() || client.level == null) return;
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<BlockPos, HitBlock>> it = hitBlocks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, HitBlock> entry = it.next();
            if (client.level.getBlockState(entry.getKey()).isAir()) {
                it.remove();
                onBlockMined(entry.getValue().gem(), now);
            } else if (now - entry.getValue().time() > HIT_EXPIRY_MS) {
                it.remove();
            }
        }
    }

    public void onAttackBlock(BlockState state, BlockPos pos) {
        if (state.isAir()) return;
        String gem = gemType(state.getBlock());
        if (gem == null) return;
        hitBlocks.put(pos.immutable(), new HitBlock(gem, System.currentTimeMillis()));
    }

    private void onBlockMined(String gem, long now) {
        boolean resumingFromPause = tracking && lastGemType == null;
        if (!tracking) {
            tracking = true;
            msbTicks = Math.max(msbTicks, 0);
            currentBreakTick = 0;
            maxMined.clear();
            realMined.clear();
            maxMined.merge(gem, 1, Integer::sum);
        } else if (resumingFromPause) {
            maxMined.merge(gem, 1, Integer::sum);
        }
        lastMinedTime = now;
        lastGemType = gem;
        realMined.merge(gem, 1, Integer::sum);
        calculate();
    }

    public void onGameMessage(String cleanText, boolean blueCheese) {
        if (cleanText.contains("Mining Speed Boost") && cleanText.contains("used your")) {
            msbTicks = blueCheese ? 500 : 400;
        } else if (cleanText.contains("Mining Speed Boost has expired")) {
            msbTicks = 0;
        }
    }

    private float calculate() {
        double real = 0, max = 0;
        for (int v : realMined.values()) real += v;
        for (int v : maxMined.values()) max += v;
        efficiency = max > 0 ? (float) (real / max * 100.0) : 0f;
        return efficiency;
    }

    private static int ticksFor(String gem, int miningSpeed) {
        if (miningSpeed <= 0) return 20;
        Integer strength = BLOCK_STRENGTH.get(gem);
        if (strength == null) return 20;
        return Math.max((int) Math.round(30.0 * strength / miningSpeed), 4);
    }

    private static int parseMiningSpeed(List<String> lines) {
        int found = 0;
        for (String line : lines) {
            if (!line.contains("Mining Speed") || line.contains("Boost")) continue;
            Matcher m = MINING_SPEED.matcher(line);
            if (!m.find()) continue;
            try {
                int speed = Integer.parseInt(m.group(1).replace(",", ""));
                if (speed > found) found = speed;
            } catch (NumberFormatException ignored) {
            }
        }
        return found;
    }

    /** The gemstone a mineshaft block stands for, keyed off the stained glass colour. */
    static String gemType(Block block) {
        String id = block.getDescriptionId().toLowerCase(Locale.ROOT);
        if (id.contains("ruby") || id.contains("red_stained")) return "Ruby";
        if (id.contains("amethyst") || id.contains("purple_stained")) return "Amethyst";
        if (id.contains("jade") || id.contains("lime_stained")) return "Jade";
        if (id.contains("sapphire") || id.contains("light_blue_stained")) return "Sapphire";
        if (id.contains("amber") || id.contains("orange_stained")) return "Amber";
        if (id.contains("topaz") || id.contains("yellow_stained")) return "Topaz";
        if (id.contains("jasper") || id.contains("magenta_stained")) return "Jasper";
        if (id.contains("aquamarine") || id.contains("blue_stained")) return "Aquamarine";
        if (id.contains("citrine") || id.contains("brown_stained")) return "Citrine";
        if (id.contains("peridot") || id.contains("green_stained")) return "Peridot";
        if (id.contains("opal") || id.contains("white_stained")) return "Opal";
        if (id.contains("onyx") || id.contains("black_stained") || id.contains("gray_stained")) return "Onyx";
        return null;
    }
}
