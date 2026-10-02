package forfun.miningqol.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

public class AutoClickerManager {
    private static boolean enabled = false;
    private static boolean inSequence = false;
    private static int sequenceStep = 0;
    private static int sequenceTickCounter = 0;
    private static boolean firstEnable = true;
    private static int expectedSlot = 0;
    /** Swap to the hotbar fishing rod and right-click it before the drill's ability. */
    private static boolean enableRodSwap = false;
    private static boolean enableSecondDrill = false;
    private static int secondDrillSlot = 3;
    private static int mainDrillDelay = 3;
    private static int secondDrillDelay = 3;
    private static boolean wasOnCooldown = true;
    /**
     * Custom Cooldown: fire the ability {@link #customCooldownSeconds} after its last use, timed
     * locally instead of from the tab list — like the Pickaxe Cooldown option. The last use comes
     * from Hypixel's "You used your … Pickaxe Ability!" line and is remembered whether CoalClick
     * is on or off, so toggling it never forgets (or restarts) the cooldown.
     */
    private static boolean customCooldownEnabled = false;
    private static int customCooldownSeconds = 120;
    private static final java.util.regex.Pattern ABILITY_USED =
        java.util.regex.Pattern.compile("You used your (.+?) Pickaxe Ability!");
    /** When the pickaxe ability was last used, by anyone; 0 = not seen this session. */
    private static long lastAbilityUseAt = 0;

    // Internal timer for triggering (ported from commit 722e1c5 "when to use ability"):
    // capture the cooldown duration when it starts and count ticks up to it (+1s buffer)
    // so we fire when the ability is actually ready, never early.
    private static int internalTickCounter = 0;
    private static int targetCooldownTicks = 0;
    private static boolean timerActive = false;
    private static boolean waitingForCooldownStart = false; // Wait for next cooldown cycle

    // Prevent double triggering
    private static long lastSequenceEndTime = 0;
    private static final long MIN_SEQUENCE_INTERVAL_MS = 5000; // 5 seconds minimum between sequences

    // TEMPORARY debug logging to diagnose intermittent ("3rd cycle") activation misses
    private static final boolean DEBUG = false;
    private static int activationCount = 0;

    /** Every chat line: notes a pickaxe ability use, even while CoalClick is off. */
    public static void onChatMessage(String message) {
        if (message != null && ABILITY_USED.matcher(message).find()) {
            lastAbilityUseAt = System.currentTimeMillis();
        }
    }

    /** Custom Cooldown: seconds until the remembered cooldown runs out (0 = ready). */
    private static double customRemainingSeconds() {
        if (lastAbilityUseAt == 0) return 0;
        long remaining = lastAbilityUseAt + customCooldownSeconds * 1000L - System.currentTimeMillis();
        return Math.max(0, remaining / 1000.0);
    }

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            inSequence = false;
            sequenceStep = 0;
            sequenceTickCounter = 0;
            timerActive = false;
            waitingForCooldownStart = false;

            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                client.options.keyAttack.setDown(false);
                client.options.keyUse.setDown(false);
            }
        } else if (customCooldownEnabled) {
            // The remembered cooldown decides; tick fires once it has run out.
            inSequence = false;
            sequenceStep = 0;
            sequenceTickCounter = 0;
            timerActive = false;
            waitingForCooldownStart = false;
            firstEnable = false;
        } else {
            // Check if ability is currently ready
            boolean abilityIsReady = !PickaxeCooldownHUD.isOnCooldown();
            double currentCooldown = PickaxeCooldownHUD.getCurrentCooldown();

            if (firstEnable || abilityIsReady) {
                // If first enable OR ability is ready, trigger immediately
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                firstEnable = false;
                waitingForCooldownStart = false;
                timerActive = false;
            } else if (currentCooldown > 0) {
                // Ability is on cooldown, start tracking it
                targetCooldownTicks = (int) (currentCooldown * 20) + 20; // Add 1 second buffer
                internalTickCounter = 0;
                timerActive = true;
                waitingForCooldownStart = false;
            }
            wasOnCooldown = PickaxeCooldownHUD.isOnCooldown();
        }
    }

    public static void setEnabled(boolean value) {
        if (enabled != value) {
            toggle();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static double getRemainingSeconds() {
        if (customCooldownEnabled) return customRemainingSeconds();
        if (firstEnable) {
            return 0;
        }
        // Use internal timer when active
        if (timerActive && targetCooldownTicks > 0) {
            int remainingTicks = targetCooldownTicks - internalTickCounter;
            return Math.max(0, remainingTicks / 20.0);
        }
        // Fall back to scoreboard when timer not active
        return PickaxeCooldownHUD.getInterpolatedCooldown();
    }

    public static boolean isCustomCooldownEnabled() {
        return customCooldownEnabled;
    }

    public static void setCustomCooldownEnabled(boolean value) {
        customCooldownEnabled = value;
    }

    public static int getCustomCooldownSeconds() {
        return customCooldownSeconds;
    }

    public static void setCustomCooldownSeconds(int seconds) {
        customCooldownSeconds = Math.max(1, Math.min(600, seconds));
    }

    public static void setMiningSlot(int slot) {
        expectedSlot = slot;
    }

    public static int getMiningSlot() {
        return expectedSlot;
    }

    public static void setEnableRodSwap(boolean value) {
        enableRodSwap = value;
    }

    public static boolean isRodSwapEnabled() {
        return enableRodSwap;
    }

    public static void setEnableSecondDrill(boolean value) {
        enableSecondDrill = value;
    }

    public static boolean isSecondDrillEnabled() {
        return enableSecondDrill;
    }

    public static void setSecondDrillSlot(int slot) {
        secondDrillSlot = slot;
    }

    public static int getSecondDrillSlot() {
        return secondDrillSlot;
    }

    public static void setMainDrillDelay(int ticks) {
        mainDrillDelay = ticks;
    }

    public static int getMainDrillDelay() {
        return mainDrillDelay;
    }

    public static void setSecondDrillDelay(int ticks) {
        secondDrillDelay = ticks;
    }

    public static int getSecondDrillDelay() {
        return secondDrillDelay;
    }

    private static int findFishingRodSlot(Minecraft client) {
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.getItem() instanceof FishingRodItem) return i;
        }
        return -1;
    }

    private static int getSelectedSlot(Minecraft client) {
        if (client.player == null) return 0;
        return client.player.getInventory().getSelectedSlot();
    }

    private static void setSelectedSlot(Minecraft client, int slot) {
        if (client.player == null) return;
        client.player.getInventory().setSelectedSlot(slot);
    }

    private static String heldItemName(Minecraft client) {
        if (client.player == null) return "?";
        int s = client.player.getInventory().getSelectedSlot();
        return s + ":" + client.player.getInventory().getItem(s).getHoverName().getString();
    }

    private static void debug(Minecraft client, String msg) {
        if (DEBUG && client.player != null) {
            MqoChat.log(Component.literal("\u00A76[MQO] \u00A77" + msg));
        }
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        // Get scoreboard cooldown info
        boolean currentlyOnCooldown = PickaxeCooldownHUD.isOnCooldown();
        double scoreboardCooldown = PickaxeCooldownHUD.getCurrentCooldown();

        // Check if enough time has passed since last sequence
        long currentTime = System.currentTimeMillis();
        boolean canStartNewSequence = (currentTime - lastSequenceEndTime) >= MIN_SEQUENCE_INTERVAL_MS;

        if (customCooldownEnabled) {
            // Fire once the remembered cooldown is over.
            if (enabled && !inSequence && canStartNewSequence && customRemainingSeconds() <= 0) {
                debug(client, "Custom cooldown fire | lastUse=" + lastAbilityUseAt);
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
            }
        } else if (waitingForCooldownStart) {
            // After sequence ends, we wait for a NEW cooldown to start before tracking again
            if (currentlyOnCooldown && scoreboardCooldown > 10.0) {
                // New cooldown started, begin tracking
                waitingForCooldownStart = false;
                targetCooldownTicks = (int) (scoreboardCooldown * 20) + 20; // Add 1 second buffer
                internalTickCounter = 0;
                timerActive = true;
            }
            wasOnCooldown = currentlyOnCooldown;

            // Still allow mining while waiting for next cooldown
            if (enabled) {
                int currentSlot = getSelectedSlot(client);
                client.options.keyAttack.setDown(currentSlot == expectedSlot);
            }
            return;
        }

        // When cooldown starts, capture the duration and start our internal timer
        if (!customCooldownEnabled && currentlyOnCooldown && !wasOnCooldown && !timerActive && canStartNewSequence) {
            targetCooldownTicks = (int) (scoreboardCooldown * 20) + 20; // Add 1 second buffer
            internalTickCounter = 0;
            timerActive = true;
        }

        // Update internal timer
        if (timerActive && !inSequence) {
            internalTickCounter++;
        }

        // Trigger based on our internal timer (fires ~1s after the cooldown ends, never early)
        if (!inSequence && enabled && timerActive && canStartNewSequence) {
            if (internalTickCounter >= targetCooldownTicks) {
                debug(client, "Timer fire | targetTicks=" + targetCooldownTicks + " counter=" + internalTickCounter);
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                timerActive = false;
            }
        }

        wasOnCooldown = currentlyOnCooldown;

        if (!enabled) {
            return;
        }

        int currentSlot = getSelectedSlot(client);

        if (inSequence) {
            client.options.keyAttack.setDown(false);
            handleManiacMinerSequence(client);
        } else {
            client.options.keyAttack.setDown(currentSlot == expectedSlot);
        }
    }

    private static void handleManiacMinerSequence(Minecraft client) {
        sequenceTickCounter++;

        // Sequence:
        // 0: Switch to rod (or skip if no rod swap / no rod)
        // 1: Wait 2 ticks for rod switch
        // 2: Right click rod (2 ticks)
        // 3: Switch to main drill
        // 4: Wait mainDrillDelay ticks
        // 5: If second drill: switch to second drill; else: right click main drill
        // 6: Wait secondDrillDelay ticks (second drill only)
        // 7: Right click second drill (second drill only)
        // 8: Switch back to main drill (second drill only)
        // 9: End

        switch (sequenceStep) {
            case 0:
                activationCount++;
                debug(client, "Activation #" + activationCount + " START | held=" + heldItemName(client)
                        + " miningSlot=" + expectedSlot + " rodSlot=" + findFishingRodSlot(client)
                        + " rodSwap=" + enableRodSwap + " 2ndDrill=" + enableSecondDrill + "(" + secondDrillSlot + ")"
                        + " ready=" + (!PickaxeCooldownHUD.isOnCooldown()));
                int rodSlot = enableRodSwap ? findFishingRodSlot(client) : -1;
                if (rodSlot != -1) {
                    setSelectedSlot(client, rodSlot);
                    sequenceStep = 1;
                } else {
                    sequenceStep = 3; // Rod swap off, or no rod in the hotbar: straight to the drill
                }
                sequenceTickCounter = 0;
                break;

            case 1: // Wait before rod right click
                if (sequenceTickCounter >= 2) {
                    sequenceStep = 2;
                    sequenceTickCounter = 0;
                }
                break;

            case 2: // Right click rod
                if (sequenceTickCounter == 1) debug(client, "  -> right-click ROD | held=" + heldItemName(client));
                client.options.keyUse.setDown(true);
                if (sequenceTickCounter >= 2) {
                    client.options.keyUse.setDown(false);
                    sequenceStep = 3;
                    sequenceTickCounter = 0;
                }
                break;

            case 3: // Switch to main drill
                setSelectedSlot(client, expectedSlot);
                sequenceStep = 4;
                sequenceTickCounter = 0;
                break;

            case 4: // Wait mainDrillDelay ticks
                if (sequenceTickCounter >= mainDrillDelay) {
                    if (enableSecondDrill) {
                        sequenceStep = 5; // Go to second drill
                    } else {
                        sequenceStep = 50; // Right click main drill ability
                    }
                    sequenceTickCounter = 0;
                }
                break;

            case 50: // Right click main drill ability (no second drill)
                if (sequenceTickCounter == 1) debug(client, "  -> right-click MAIN DRILL (activate) | held=" + heldItemName(client));
                client.options.keyUse.setDown(true);
                if (sequenceTickCounter >= 2) {
                    client.options.keyUse.setDown(false);
                    sequenceStep = 9; // End
                    sequenceTickCounter = 0;
                }
                break;

            case 5: // Switch to second drill
                setSelectedSlot(client, secondDrillSlot);
                sequenceStep = 6;
                sequenceTickCounter = 0;
                break;

            case 6: // Wait secondDrillDelay ticks
                if (sequenceTickCounter >= secondDrillDelay) {
                    sequenceStep = 7;
                    sequenceTickCounter = 0;
                }
                break;

            case 7: // Right click second drill
                if (sequenceTickCounter == 1) debug(client, "  -> right-click 2ND DRILL (activate) | held=" + heldItemName(client));
                client.options.keyUse.setDown(true);
                if (sequenceTickCounter >= 2) {
                    client.options.keyUse.setDown(false);
                    sequenceStep = 8;
                    sequenceTickCounter = 0;
                }
                break;

            case 8: // Switch back to main drill
                setSelectedSlot(client, expectedSlot);
                sequenceStep = 9;
                sequenceTickCounter = 0;
                break;

            case 9: // End sequence
                debug(client, "Activation #" + activationCount + " DONE | held=" + heldItemName(client));
                inSequence = false;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                lastSequenceEndTime = System.currentTimeMillis();
                timerActive = false;
                // Custom Cooldown: count from this click, in case a chat filter hides Hypixel's
                // "used your ability" line; when that line does arrive it refines the time.
                if (customCooldownEnabled) lastAbilityUseAt = lastSequenceEndTime;
                waitingForCooldownStart = !customCooldownEnabled;
                break;
        }
    }

    public static void cleanup() {
        if (enabled) {
            Minecraft client = Minecraft.getInstance();
            client.options.keyAttack.setDown(false);
            client.options.keyUse.setDown(false);
        }
    }
}
