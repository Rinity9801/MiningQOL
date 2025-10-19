package forfun.miningqol.client;

import forfun.miningqol.client.config.MiningConfig;
import forfun.miningqol.client.gui.MiningConfigScreen;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import forfun.miningqol.client.profit.ProfitDebugger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiningqolClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MiningqolClient");
    private static final Pattern CORPSE_LOOT_PATTERN = Pattern.compile("\\s(.+) CORPSE LOOT!\\s");
    private static final Pattern PRISTINE_PATTERN = Pattern.compile("PRISTINE! You found . Flawed (.+) Gemstone x(\\d+)!");
    private static MiningConfig config;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[MiningqolClient] Initializing MiningQOL Mod");

        config = MiningConfig.load();
        config.applyToGame();

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
                        client.setScreen(new MiningConfigScreen(client.currentScreen));
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
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                CorpseESP.tick();
                GemstoneTracker.tick();
                EfficientMinerOverlay.tick();
            }
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
        });

        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            ProfitTrackerHUD.render(context);
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CorpseESP.onWorldUnload();

            config.loadFromGame();
            config.save();
        });

        LOGGER.info("[MiningqolClient] MiningQOL Mod initialized");
    }

    public static MiningConfig getConfig() {
        return config;
    }
}
