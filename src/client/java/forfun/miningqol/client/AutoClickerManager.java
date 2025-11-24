package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;

public class AutoClickerManager {
    private static boolean enabled = false;
    private static int tickCounter = 0;
    private static int cycleDuration = 102 * 20;
    private static int baseCycleDuration = 102 * 20;
    private static boolean inSequence = false;
    private static int sequenceStep = 0;
    private static int sequenceTickCounter = 0;
    private static boolean firstEnable = true;
    private static int expectedSlot = 0;
    private static boolean enableRodSwap = true;
    private static boolean enableSecondDrill = false;
    private static int secondDrillSlot = 3;
    private static boolean cooldownBuffActive = false;
    private static int cooldownBuffTicksRemaining = 0;
    private static boolean useTabCooldown = true;
    private static boolean wasOnCooldown = false;

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            inSequence = false;
            sequenceStep = 0;
            sequenceTickCounter = 0;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.options.attackKey.setPressed(false);
                client.options.useKey.setPressed(false);
            }
        } else {
            // Check if ability is currently ready
            boolean abilityIsReady = !PickaxeCooldownHUD.isOnCooldown() && PickaxeCooldownHUD.getCurrentCooldown() <= 0;

            if (firstEnable || abilityIsReady) {
                // If first enable OR ability is ready, trigger immediately
                inSequence = true;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                tickCounter = 0;
                firstEnable = false;
            } else {
                // If ability is on cooldown, just sync the state
                tickCounter = 0;
            }

            // Set wasOnCooldown to current state to avoid immediate re-trigger after sequence
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

    public static int getRemainingTicks() {
        if (firstEnable) {
            return 0;
        }
        if (useTabCooldown && PickaxeCooldownHUD.isOnCooldown()) {
            double tabCooldown = PickaxeCooldownHUD.getCurrentCooldown();
            if (tabCooldown > 0) {
                return (int) (tabCooldown * 20);
            }
        }
        return Math.max(0, cycleDuration - tickCounter);
    }

    public static int getTotalCycleDuration() {
        return cycleDuration;
    }

    public static void setMiningSlot(int slot) {
        expectedSlot = slot;
    }

    public static int getMiningSlot() {
        return expectedSlot;
    }

    public static void setManiacMinerCooldown(int seconds) {
        baseCycleDuration = seconds * 20;
        updateCycleDuration();
    }

    public static int getManiacMinerCooldown() {
        return baseCycleDuration / 20;
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

    public static void activateCooldownBuff() {
        cooldownBuffActive = true;
        cooldownBuffTicksRemaining = 20 * 60 * 20;
        updateCycleDuration();
    }

    private static void updateCycleDuration() {
        if (cooldownBuffActive) {
            cycleDuration = (int) (baseCycleDuration * 0.8);
        } else {
            cycleDuration = baseCycleDuration;
        }
    }

    private static int findFishingRodSlot(MinecraftClient client) {
        if (client.player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof FishingRodItem) {
                return i;
            }
        }
        return -1;
    }

    private static int getSelectedSlot(MinecraftClient client) {
        if (client.player == null) return 0;
        return client.player.getInventory().getSelectedSlot();
    }

    private static void setSelectedSlot(MinecraftClient client, int slot) {
        if (client.player == null) return;
        client.player.getInventory().setSelectedSlot(slot);
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        if (cooldownBuffActive) {
            cooldownBuffTicksRemaining--;
            if (cooldownBuffTicksRemaining <= 0) {
                cooldownBuffActive = false;
                updateCycleDuration();
            }
        }

        if (useTabCooldown) {
            boolean currentlyOnCooldown = PickaxeCooldownHUD.isOnCooldown();

            // Check interpolated cooldown to fill gaps when tab doesn't update
            double tabCooldown = PickaxeCooldownHUD.getCurrentCooldown();
            boolean interpolatedReady = tabCooldown <= 0;

            if (!inSequence) {
                boolean shouldTrigger = false;

                // Trigger on transition from cooldown to ready (tab-based)
                if (wasOnCooldown && !currentlyOnCooldown) {
                    shouldTrigger = true;
                }

                // Also trigger if interpolated cooldown shows ready and we're not on cooldown
                if (!currentlyOnCooldown && interpolatedReady && tickCounter >= cycleDuration) {
                    shouldTrigger = true;
                }

                if (shouldTrigger && enabled) {
                    inSequence = true;
                    sequenceStep = 0;
                    sequenceTickCounter = 0;
                    tickCounter = 0;
                }
            }

            wasOnCooldown = currentlyOnCooldown;

            // Increment tickCounter even when using tab cooldown for fallback timing
            if (!inSequence && !currentlyOnCooldown) {
                tickCounter++;
            } else if (currentlyOnCooldown) {
                tickCounter = 0;
            }
        } else {
            if (!inSequence) {
                tickCounter++;
                if (tickCounter >= cycleDuration) {
                    if (enabled) {
                        inSequence = true;
                        sequenceStep = 0;
                        sequenceTickCounter = 0;
                        tickCounter = 0;
                    }
                }
            }
        }

        if (!enabled) {
            return;
        }

        int currentSlot = getSelectedSlot(client);

        if (inSequence) {
            client.options.attackKey.setPressed(false);
            handleManiacMinerSequence(client);
        } else {
            client.options.attackKey.setPressed(currentSlot == expectedSlot);
        }
    }

    private static void handleManiacMinerSequence(MinecraftClient client) {
        sequenceTickCounter++;

        switch (sequenceStep) {
            case 0:
                if (enableRodSwap) {
                    int rodSlot = findFishingRodSlot(client);
                    if (rodSlot != -1) {
                        setSelectedSlot(client, rodSlot);
                        sequenceStep++;
                    } else {
                        sequenceStep = 4;
                    }
                } else {
                    sequenceStep = 4;
                }
                sequenceTickCounter = 0;
                break;

            case 1, 5:
                if (sequenceTickCounter >= 2) {
                    sequenceStep++;
                    sequenceTickCounter = 0;
                }
                break;

            case 2, 6, 10:
                client.options.useKey.setPressed(true);
                if (sequenceTickCounter >= 3) {
                    client.options.useKey.setPressed(false);
                    sequenceStep++;
                    sequenceTickCounter = 0;
                }
                break;

            case 3, 7, 9:
                if (sequenceTickCounter >= 3) {
                    sequenceStep++;
                    sequenceTickCounter = 0;
                }
                break;

            case 4:
                if (enableSecondDrill) {
                    setSelectedSlot(client, secondDrillSlot);
                    sequenceStep++;
                } else {
                    sequenceStep = 8;
                }
                sequenceTickCounter = 0;
                break;

            case 8:
                setSelectedSlot(client, expectedSlot);
                sequenceStep++;
                sequenceTickCounter = 0;
                break;

            case 11:
                inSequence = false;
                sequenceStep = 0;
                sequenceTickCounter = 0;
                tickCounter = 0;
                break;
        }
    }

    public static void cleanup() {
        if (enabled) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);
        }
    }

    public static boolean isUsingTabCooldown() {
        return useTabCooldown;
    }

    public static void setUseTabCooldown(boolean value) {
        useTabCooldown = value;
    }
}
