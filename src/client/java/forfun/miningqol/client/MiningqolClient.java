package forfun.miningqol.client;

import forfun.miningqol.client.config.MiningConfig;
import forfun.miningqol.client.gui.VexelMainScreen;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import forfun.miningqol.client.profit.ProfitDebugger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiningqolClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningqolClient");
    private static final Pattern CORPSE_LOOT_PATTERN = Pattern.compile("\\s(.+) CORPSE LOOT!\\s");
    private static final Pattern PRISTINE_PATTERN = Pattern.compile("PRISTINE! You found . Flawed (.+) Gemstone x(\\d+)!");
    private static MiningConfig config;
    private static KeyBinding toggleAutoClickerKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[MiningqolClient] Initializing MiningQOL Mod");

        config = MiningConfig.load();
        config.applyToGame();

        
        toggleAutoClickerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.miningqol.toggle_coalclick",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.miningqol"
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("getcorpse")
                .executes(context -> {
                    CorpseESP.getCorpseInfo();
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("miningconfig")
                .executes(context -> {
                    MinecraftClient.getInstance().send(() -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.setScreen(new VexelMainScreen());
                    });
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("profitreset")
                .executes(context -> {
                    GemstoneTracker.reset();
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("profitdebug")
                .executes(context -> {
                    ProfitDebugger.showCalculationDetails();
                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("getplayerhead")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null) return 0;

                    net.minecraft.item.ItemStack heldItem = client.player.getMainHandStack();
                    if (heldItem.isEmpty()) {
                        client.player.sendMessage(Text.literal("§cNo item in hand!"), false);
                        return 0;
                    }

                    // Get the profile component
                    net.minecraft.component.type.ProfileComponent profile = heldItem.get(net.minecraft.component.DataComponentTypes.PROFILE);

                    if (profile == null) {
                        client.player.sendMessage(Text.literal("§cThis item has no profile data!"), false);
                        return 0;
                    }

                    // Format the profile data
                    StringBuilder sb = new StringBuilder();
                    sb.append("[minecraft:profile={");

                    // Add UUID as int array
                    java.util.UUID uuid = profile.gameProfile().getId();
                    if (uuid != null) {
                        long mostSig = uuid.getMostSignificantBits();
                        long leastSig = uuid.getLeastSignificantBits();
                        int[] uuidInts = new int[4];
                        uuidInts[0] = (int)(mostSig >> 32);
                        uuidInts[1] = (int)mostSig;
                        uuidInts[2] = (int)(leastSig >> 32);
                        uuidInts[3] = (int)leastSig;
                        sb.append("id:[I;").append(uuidInts[0]).append(",")
                          .append(uuidInts[1]).append(",")
                          .append(uuidInts[2]).append(",")
                          .append(uuidInts[3]).append("]");
                    }

                    // Add name
                    String name = profile.gameProfile().getName();
                    if (name == null) name = "";
                    sb.append(",name:\"").append(name).append("\"");

                    // Add properties
                    sb.append(",properties:[");
                    com.mojang.authlib.properties.PropertyMap properties = profile.gameProfile().getProperties();
                    if (!properties.isEmpty()) {
                        boolean first = true;
                        for (com.mojang.authlib.properties.Property prop : properties.values()) {
                            if (!first) sb.append(",");
                            first = false;
                            sb.append("{name:\"").append(prop.name()).append("\"");
                            sb.append(",value:\"").append(prop.value()).append("\"}");
                        }
                    }
                    sb.append("]}]");

                    // Send to player and copy to clipboard
                    client.player.sendMessage(Text.literal("§6Profile Data: §f" + sb.toString()), false);
                    client.keyboard.setClipboard(sb.toString());
                    client.player.sendMessage(Text.literal("§aCopied to clipboard!"), false);

                    return 1;
                }));
            dispatcher.register(ClientCommandManager.literal("lobbyfind")
                .then(ClientCommandManager.literal("add")
                    .executes(context -> {
                        LobbyFinder.addBlock();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("remove")
                    .executes(context -> {
                        LobbyFinder.removeBlock();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("clear")
                    .executes(context -> {
                        LobbyFinder.clearAll();
                        return 1;
                    }))
                .then(ClientCommandManager.literal("list")
                    .executes(context -> {
                        LobbyFinder.listBlocks();
                        return 1;
                    })));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (toggleAutoClickerKey.wasPressed()) {
                AutoClickerManager.toggle();
            }

            if (client.world != null && client.player != null) {
                CorpseESP.tick();
                GemstoneTracker.tick();
                EfficientMinerOverlay.tick();
                PickaxeCooldownHUD.tick();
                AutoClickerManager.tick();
                CommandKeybindManager.tick(client);
                LobbyFinder.tick();
            }
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.START_WORLD_TICK.register(world -> {
            LobbyFinder.onWorldChange();
        });

        WorldRenderEvents.LAST.register(context -> {
            CorpseESP.render(context.matrixStack(), context.camera());
            BlockOutlineRenderer.render(context.matrixStack(), context.camera());
            EfficientMinerOverlay.render(context.matrixStack(), context.camera());
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String messageText = message.getString();

            Matcher corpseMatcher = CORPSE_LOOT_PATTERN.matcher(messageText);
            if (corpseMatcher.find()) {
                CorpseESP.onCorpseClaimed();
            }

            Matcher pristineMatcher = PRISTINE_PATTERN.matcher(messageText);
            if (pristineMatcher.find()) {
                String gemType = pristineMatcher.group(1);
                int amount = Integer.parseInt(pristineMatcher.group(2));
                GemstoneTracker.onPristineGem(gemType, amount);
            }

            if (messageText.contains("New buff: -20% Pickaxe Ability cooldowns.")) {
                AutoClickerManager.activateCooldownBuff();
            }
        });

        ClientSendMessageEvents.COMMAND.register((command) -> {
            if (config.autoSkipShoLoad && command.startsWith("sho load ")) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    // Schedule the skipto command to run after a short delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(100); // Wait 100ms for the load command to process
                            client.execute(() -> {
                                if (client.player != null) {
                                    client.player.networkHandler.sendChatCommand("sho skipto 1");
                                }
                            });
                        } catch (InterruptedException e) {
                            LOGGER.error("Failed to auto-skip sho load", e);
                        }
                    }).start();
                }
            }
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            ProfitTrackerHUD.render(context);
            PickaxeCooldownHUD.render(context);
            AutoClickerHUD.render(context, client);
            LobbyFinderHUD.render(context);
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CorpseESP.onWorldUnload();
            AutoClickerManager.cleanup();

            config.loadFromGame();
            config.save();
        });

        LOGGER.info("[MiningqolClient] MiningQOL Mod initialized");
    }

    public static MiningConfig getConfig() {
        return config;
    }
}
