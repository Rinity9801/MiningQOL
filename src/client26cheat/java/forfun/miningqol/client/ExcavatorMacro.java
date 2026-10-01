package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Farms glacite powder at the Fossil Excavator with Suspicious Scrap, the V5 macro
 * ported to Java: open the excavator, press Start Excavator, then keep clicking the
 * dig grid — lime tiles first, otherwise a random brown one — until the tiles turn
 * black (dig finished, start again) or yellow (reopen the excavator).
 *
 * <p>Stand next to the excavator at the Dwarven Base Camp before starting; there is no
 * pathfinder here, so it stops if you are not within reach.
 */
public final class ExcavatorMacro {
    private static final Logger LOGGER = LoggerFactory.getLogger("Excavator");
    private static final Vec3 EXCAVATOR_POS = new Vec3(19.5, 121.0, 227.5);
    private static final double MAX_REACH = 4.5;
    private static final String GUI_NAME = "fossil excavator";
    private static final int GUI_TIMEOUT_TICKS = 100;
    private static final int OPEN_RETRIES = 3;
    private static final int SLOT_BLACKLIST_TICKS = 10;
    private static final Random RANDOM = new Random();
    private static final Pattern CHARGES = Pattern.compile("Chisel Charges Remaining: (\\d+)");
    private static final Pattern PROGRESS = Pattern.compile("Fossil Excavation Progress: ([\\d.]+%)");

    private enum State { IDLE, OPENING, WAIT_GUI, SETUP, EXCAVATING }

    private static boolean noDelay = false;
    private static int tickDelay = 5;
    private static boolean debug = false;

    private static State state = State.IDLE;
    private static boolean inExcavator;
    private static int tickCount;
    private static int waitTicks;
    private static int openAttempts;
    private static final Map<Integer, Integer> blacklistedSlots = new HashMap<>();
    private static int digs;
    /** Chisel charges the current dig started with; picks the solver's opening sequence. */
    private static int maxCharges;
    private static String percentage;
    private static String lastSolverNote = "-";

    private ExcavatorMacro() {}

    public static boolean isRunning() { return state != State.IDLE; }
    public static boolean isNoDelay() { return noDelay; }
    public static void setNoDelay(boolean value) { noDelay = value; }
    public static int getTickDelay() { return tickDelay; }
    public static void setTickDelay(int value) { tickDelay = Math.max(1, Math.min(10, value)); }
    public static boolean isDebug() { return debug; }
    public static void setDebug(boolean value) { debug = value; }
    public static int getDigs() { return digs; }
    public static String stateName() { return state.name(); }

    public static void toggle() {
        if (isRunning()) stop("§cExcavator stopped.");
        else start();
    }

    public static void start() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (!nearExcavator(client)) {
            MqoChat.reply("§6[Excavator] §cStand next to the Fossil Excavator at the Dwarven Base Camp first.");
            return;
        }
        state = State.OPENING;
        inExcavator = false;
        tickCount = 0;
        waitTicks = 0;
        openAttempts = 0;
        digs = 0;
        blacklistedSlots.clear();
        MqoChat.log("§6[Excavator] §aStarted.");
    }

    public static void stop(String message) {
        boolean wasRunning = isRunning();
        state = State.IDLE;
        inExcavator = false;
        blacklistedSlots.clear();
        if (wasRunning && message != null) MqoChat.log("§6[Excavator] " + message);
    }

    public static void cleanup() {
        state = State.IDLE;
        inExcavator = false;
        blacklistedSlots.clear();
    }

    public static void onChatMessage(String message) {
        if (!isRunning() || message == null) return;
        String lower = message.replaceAll("§.", "").toLowerCase(Locale.ROOT);
        if (lower.contains("suspicious scrap") && (lower.contains("don't have") || lower.contains("do not have") || lower.contains("need"))) {
            stop("§cOut of Suspicious Scrap.");
        }
    }

    public static void tick() {
        if (state == State.IDLE) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            cleanup();
            return;
        }
        tickBlacklist();

        if (!nearExcavator(client)) {
            stop("§cLeft the excavator.");
            return;
        }
        boolean guiOpen = isExcavatorOpen(client);
        if (inExcavator && !guiOpen && state == State.EXCAVATING) {
            stop("§cExcavator closed.");
            return;
        }

        switch (state) {
            case OPENING -> {
                if (guiOpen) {
                    inExcavator = true;
                    state = State.SETUP;
                    return;
                }
                if (openAttempts >= OPEN_RETRIES) {
                    stop("§cCould not open the Fossil Excavator.");
                    return;
                }
                Entity npc = findExcavator(client);
                if (npc == null) {
                    stop("§cNo excavator NPC found nearby.");
                    return;
                }
                lookAt(client, npc);
                client.gameMode.interact(client.player, npc, new EntityHitResult(npc), InteractionHand.MAIN_HAND);
                client.player.swing(InteractionHand.MAIN_HAND);
                openAttempts++;
                waitTicks = GUI_TIMEOUT_TICKS;
                state = State.WAIT_GUI;
                if (debug) MqoChat.log("§6[Excavator] §7interact -> " + npc.getType().getDescriptionId() + " attempt " + openAttempts);
            }
            case WAIT_GUI -> {
                if (guiOpen) {
                    inExcavator = true;
                    state = State.SETUP;
                    return;
                }
                if (--waitTicks <= 0) state = State.OPENING;
            }
            case SETUP -> {
                if (!guiOpen) return;
                if (!clickDelay()) return;
                int start = findSlotByName(client, "start excavator");
                if (start < 0) {
                    // The grid is already showing: skip straight to digging.
                    state = State.EXCAVATING;
                    return;
                }
                clickSlot(client, start);
                digs++;
                maxCharges = 0;
                percentage = null;
                state = State.EXCAVATING;
                if (debug) MqoChat.log("§6[Excavator] §7Start Excavator clicked (dig #" + digs + ")");
            }
            case EXCAVATING -> {
                if (!guiOpen) return;
                ContainerScreen screen = (ContainerScreen) client.screen;
                int slots = Math.min(54, Math.max(0, screen.getMenu().slots.size() - 36));
                Set<Integer> dirt = new HashSet<>();
                Set<Integer> fossil = new HashSet<>();
                boolean chargesRead = false;
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = screen.getMenu().getSlot(i).getItem();
                    String id = itemId(stack);
                    if (id == null) continue;
                    if (id.contains("black_stained")) {
                        state = State.SETUP;
                        return;
                    }
                    if (id.contains("yellow_stained")) {
                        client.player.closeContainer();
                        inExcavator = false;
                        openAttempts = 0;
                        state = State.OPENING;
                        if (debug) MqoChat.log("§6[Excavator] §7yellow tile: reopening");
                        return;
                    }
                    String name = cleanName(stack);
                    boolean isDirt = name.equals("Dirt") || (name.isEmpty() && id.contains("brown_stained"));
                    boolean isFossil = name.equals("Fossil") || (name.isEmpty() && id.contains("lime_stained"));
                    if (!isDirt && !isFossil) continue;
                    if (isDirt) dirt.add(i); else fossil.add(i);
                    if (!chargesRead || (isFossil && percentage == null)) {
                        for (String line : loreLines(stack)) {
                            if (!chargesRead) {
                                Matcher m = CHARGES.matcher(line);
                                if (m.find()) {
                                    int charges = Integer.parseInt(m.group(1));
                                    if (maxCharges == 0) maxCharges = charges;
                                    chargesRead = true;
                                }
                            }
                            if (isFossil && percentage == null) {
                                Matcher m = PROGRESS.matcher(line);
                                if (m.find()) percentage = m.group(1);
                            }
                        }
                    }
                }
                if (dirt.isEmpty()) return;

                FossilSolver.Result result = FossilSolver.findBestTile(fossil, dirt, percentage, maxCharges);
                lastSolverNote = FossilSolver.describe(result);
                Integer target = result.slot();
                if (target == null || !dirt.contains(target)) {
                    // Fossil fully uncovered or board unreadable: spend the remaining charges on
                    // any undug tile, as the original macro did.
                    List<Integer> open = new ArrayList<>();
                    for (int i : dirt) if (!blacklistedSlots.containsKey(i)) open.add(i);
                    if (open.isEmpty()) return;
                    target = open.get(RANDOM.nextInt(open.size()));
                } else if (blacklistedSlots.containsKey(target)) {
                    return; // clicked already; the server has not updated the grid yet
                }
                if (!clickDelay()) return;
                clickSlot(client, target);
                blacklistedSlots.put(target, SLOT_BLACKLIST_TICKS);
                if (debug) MqoChat.log("§6[Excavator] §7dig " + target + " §8(" + lastSolverNote + ")");
            }
            default -> { }
        }
    }

    // ---- helpers ----

    private static boolean nearExcavator(Minecraft client) {
        // Distance only: the excavator's coordinates are unique enough, and the scoreboard
        // area line is not always readable at the base camp.
        return client.player.position().distanceTo(EXCAVATOR_POS) <= MAX_REACH + 2.0;
    }

    private static boolean isExcavatorOpen(Minecraft client) {
        return client.screen instanceof ContainerScreen screen
            && screen.getTitle().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT).contains(GUI_NAME);
    }

    /** The closest non-item entity to the excavator spot, which is Hypixel's NPC for it. */
    private static Entity findExcavator(Minecraft client) {
        AABB box = new AABB(EXCAVATOR_POS.x - 3, EXCAVATOR_POS.y - 3, EXCAVATOR_POS.z - 3,
            EXCAVATOR_POS.x + 3, EXCAVATOR_POS.y + 3, EXCAVATOR_POS.z + 3);
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity entity : client.level.getEntities(client.player, box, e -> !(e instanceof ItemEntity))) {
            double d = entity.position().distanceToSqr(EXCAVATOR_POS);
            if (d < bestDist) {
                bestDist = d;
                best = entity;
            }
        }
        return best;
    }

    private static void lookAt(Minecraft client, Entity entity) {
        Vec3 eye = client.player.getEyePosition();
        Vec3 target = entity.position().add(0, entity.getBbHeight() * 0.8, 0);
        Vec3 delta = target.subtract(eye);
        double xz = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, xz));
        client.player.setYRot(yaw);
        client.player.setXRot(Math.max(-90f, Math.min(90f, pitch)));
    }

    private static int findSlotByName(Minecraft client, String nameLower) {
        if (!(client.screen instanceof ContainerScreen screen)) return -1;
        int slots = Math.max(0, screen.getMenu().slots.size() - 36);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = screen.getMenu().getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            if (stack.getHoverName().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT).contains(nameLower)) return i;
        }
        return -1;
    }

    private static String cleanName(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getHoverName().getString().replaceAll("§.", "").trim();
    }

    private static List<String> loreLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        net.minecraft.world.item.component.ItemLore lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null) for (Component line : lore.lines()) lines.add(line.getString().replaceAll("§.", ""));
        return lines;
    }

    private static String itemId(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static void clickSlot(Minecraft client, int slot) {
        if (!(client.screen instanceof ContainerScreen screen) || client.gameMode == null) return;
        // Pick-block (middle) click, as SkyHanni's solver and the original macro send: it
        // registers as a click without ever lifting the tile onto the cursor.
        client.gameMode.handleContainerInput(screen.getMenu().containerId, slot, 2, ContainerInput.CLONE, client.player);
    }

    private static boolean clickDelay() {
        if (noDelay) return true;
        if (tickCount > 0) {
            tickCount--;
            return false;
        }
        tickCount = tickDelay;
        return true;
    }

    private static void tickBlacklist() {
        Iterator<Map.Entry<Integer, Integer>> it = blacklistedSlots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> e = it.next();
            if (e.getValue() <= 1) it.remove();
            else e.setValue(e.getValue() - 1);
        }
    }

    /** {@code /excavator debug}: what the macro sees right now. */
    public static void printDebug() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        MqoChat.reply("§6[Excavator] §7state=§f" + state + " §7digs=§f" + digs + " §7near=§f" + nearExcavator(client)
            + " §7guiOpen=§f" + isExcavatorOpen(client) + " §7dist=§f" + String.format(Locale.ROOT, "%.1f", client.player.position().distanceTo(EXCAVATOR_POS)));
        Entity npc = findExcavator(client);
        MqoChat.reply("§6[Excavator] §7solver=§f" + lastSolverNote + " §7maxCharges=§f" + maxCharges + " §7progress=§f" + percentage);
        MqoChat.reply("§6[Excavator] §7npc=§f" + (npc == null ? "none" : npc.getType().getDescriptionId() + " " + npc.getName().getString()));
        if (client.screen instanceof ContainerScreen screen) {
            MqoChat.reply("§6[Excavator] §7gui=§f" + screen.getTitle().getString());
            int slots = Math.max(0, screen.getMenu().slots.size() - 36);
            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; i < slots; i++) {
                String id = itemId(screen.getMenu().getSlot(i).getItem());
                if (id != null) counts.merge(id, 1, Integer::sum);
            }
            MqoChat.reply("§6[Excavator] §7items=§f" + counts);
        }
    }
}
