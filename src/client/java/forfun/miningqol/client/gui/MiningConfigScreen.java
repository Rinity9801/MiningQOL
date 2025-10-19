package forfun.miningqol.client.gui;

import forfun.miningqol.client.BlockOutlineRenderer;
import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.EfficientMinerOverlay;
import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.profit.BazaarPriceManager;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MiningConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 500;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;

    private final Screen parent;

    public MiningConfigScreen(Screen parent) {
        super(Text.literal("MiningQOL Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int startY = panelY + 40;

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isLapisEnabled() ? "§9Lapis: §aON" : "§9Lapis: §cOFF"),
            button -> {
                CorpseESP.toggleLapis();
                button.setMessage(Text.literal(CorpseESP.isLapisEnabled() ? "§9Lapis: §aON" : "§9Lapis: §cOFF"));
            })
            .dimensions(buttonX, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isTungstenEnabled() ? "§fTungsten: §aON" : "§fTungsten: §cOFF"),
            button -> {
                CorpseESP.toggleTungsten();
                button.setMessage(Text.literal(CorpseESP.isTungstenEnabled() ? "§fTungsten: §aON" : "§fTungsten: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isUmberEnabled() ? "§6Umber: §aON" : "§6Umber: §cOFF"),
            button -> {
                CorpseESP.toggleUmber();
                button.setMessage(Text.literal(CorpseESP.isUmberEnabled() ? "§6Umber: §aON" : "§6Umber: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(CorpseESP.isVanguardEnabled() ? "§dVanguard: §aON" : "§dVanguard: §cOFF"),
            button -> {
                CorpseESP.toggleVanguard();
                button.setMessage(Text.literal(CorpseESP.isVanguardEnabled() ? "§dVanguard: §aON" : "§dVanguard: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 3, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(ProfitTrackerHUD.isEnabled() ? "§2Profit Tracker: §aON" : "§2Profit Tracker: §cOFF"),
            button -> {
                ProfitTrackerHUD.setEnabled(!ProfitTrackerHUD.isEnabled());
                button.setMessage(Text.literal(ProfitTrackerHUD.isEnabled() ? "§2Profit Tracker: §aON" : "§2Profit Tracker: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 4, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(GemstoneTracker.isIncludingRough() ? "§7Include Rough: §aON" : "§7Include Rough: §cOFF"),
            button -> {
                GemstoneTracker.setIncludeRough(!GemstoneTracker.isIncludingRough());
                button.setMessage(Text.literal(GemstoneTracker.isIncludingRough() ? "§7Include Rough: §aON" : "§7Include Rough: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 5, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(BazaarPriceManager.isUsingNPCPrices() ? "§eForce NPC Prices: §aON" : "§eForce NPC Prices: §cOFF"),
            button -> {
                BazaarPriceManager.setUseNPCPrices(!BazaarPriceManager.isUsingNPCPrices());
                button.setMessage(Text.literal(BazaarPriceManager.isUsingNPCPrices() ? "§eForce NPC Prices: §aON" : "§eForce NPC Prices: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 6, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("§5Gem Tier: §6" + GemstoneTracker.getGemTierName()),
            button -> {
                int currentTier = GemstoneTracker.getGemTier();
                int nextTier = currentTier >= 3 ? 1 : currentTier + 1;
                GemstoneTracker.setGemTier(nextTier);
                button.setMessage(Text.literal("§5Gem Tier: §6" + GemstoneTracker.getGemTierName()));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 7, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("§bProfit Tracker Position"),
            button -> {
                if (this.client != null) {
                    this.client.setScreen(new ProfitPositionScreen(this));
                }
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 8, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        // Efficient Miner Settings
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(EfficientMinerOverlay.isEnabled() ? "§cMiner Overlay: §aON" : "§cMiner Overlay: §cOFF"),
            button -> {
                EfficientMinerOverlay.setEnabled(!EfficientMinerOverlay.isEnabled());
                button.setMessage(Text.literal(EfficientMinerOverlay.isEnabled() ? "§cMiner Overlay: §aON" : "§cMiner Overlay: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 9, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(EfficientMinerOverlay.isUsingOldHeatmap() ? "§4Old Heatmap: §aON" : "§4Old Heatmap: §cOFF"),
            button -> {
                EfficientMinerOverlay.setUseOldHeatmap(!EfficientMinerOverlay.isUsingOldHeatmap());
                button.setMessage(Text.literal(EfficientMinerOverlay.isUsingOldHeatmap() ? "§4Old Heatmap: §aON" : "§4Old Heatmap: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 10, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        // Block Outline Settings
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal(BlockOutlineRenderer.isEnabled() ? "§3Block Outline: §aON" : "§3Block Outline: §cOFF"),
            button -> {
                BlockOutlineRenderer.setEnabled(!BlockOutlineRenderer.isEnabled());
                button.setMessage(Text.literal(BlockOutlineRenderer.isEnabled() ? "§3Block Outline: §aON" : "§3Block Outline: §cOFF"));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 11, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("§3Mode: " + getModeDisplayName()),
            button -> {
                BlockOutlineRenderer.OutlineMode currentMode = BlockOutlineRenderer.getMode();
                BlockOutlineRenderer.OutlineMode nextMode;
                switch (currentMode) {
                    case OUTLINE_ONLY -> nextMode = BlockOutlineRenderer.OutlineMode.FILLED;
                    case FILLED -> nextMode = BlockOutlineRenderer.OutlineMode.BOTH;
                    case BOTH -> nextMode = BlockOutlineRenderer.OutlineMode.OUTLINE_ONLY;
                    default -> nextMode = BlockOutlineRenderer.OutlineMode.OUTLINE_ONLY;
                }
                BlockOutlineRenderer.setMode(nextMode);
                button.setMessage(Text.literal("§3Mode: " + getModeDisplayName()));
            })
            .dimensions(buttonX, startY + BUTTON_SPACING * 12, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());

        // Color sliders
        this.addDrawableChild(new ColorSliderWidget(
            buttonX, startY + BUTTON_SPACING * 13, BUTTON_WIDTH, BUTTON_HEIGHT,
            "§cRed", BlockOutlineRenderer.getRed(), 0xFF0000,
            value -> BlockOutlineRenderer.setColor(value, BlockOutlineRenderer.getGreen(), BlockOutlineRenderer.getBlue(), BlockOutlineRenderer.getAlpha())
        ));

        this.addDrawableChild(new ColorSliderWidget(
            buttonX, startY + BUTTON_SPACING * 14, BUTTON_WIDTH, BUTTON_HEIGHT,
            "§aGreen", BlockOutlineRenderer.getGreen(), 0x00FF00,
            value -> BlockOutlineRenderer.setColor(BlockOutlineRenderer.getRed(), value, BlockOutlineRenderer.getBlue(), BlockOutlineRenderer.getAlpha())
        ));

        this.addDrawableChild(new ColorSliderWidget(
            buttonX, startY + BUTTON_SPACING * 15, BUTTON_WIDTH, BUTTON_HEIGHT,
            "§9Blue", BlockOutlineRenderer.getBlue(), 0x0000FF,
            value -> BlockOutlineRenderer.setColor(BlockOutlineRenderer.getRed(), BlockOutlineRenderer.getGreen(), value, BlockOutlineRenderer.getAlpha())
        ));

        this.addDrawableChild(new ColorSliderWidget(
            buttonX, startY + BUTTON_SPACING * 16, BUTTON_WIDTH, BUTTON_HEIGHT,
            "§fAlpha", BlockOutlineRenderer.getAlpha(), 0xFFFFFF,
            value -> BlockOutlineRenderer.setColor(BlockOutlineRenderer.getRed(), BlockOutlineRenderer.getGreen(), BlockOutlineRenderer.getBlue(), value)
        ));

        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Done"),
            button -> this.close())
            .dimensions(buttonX, startY + BUTTON_SPACING * 17 + 10, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC000000);
        context.fill(panelX + 2, panelY + 2, panelX + PANEL_WIDTH - 2, panelY + PANEL_HEIGHT - 2, 0x88000000);

        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFF4488FF);
        context.drawBorder(panelX + 1, panelY + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, 0x884488FF);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + 15, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (MiningqolClient.getConfig() != null) {
            MiningqolClient.getConfig().loadFromGame();
            MiningqolClient.getConfig().save();
        }

        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private String getModeDisplayName() {
        return switch (BlockOutlineRenderer.getMode()) {
            case OUTLINE_ONLY -> "§bOutline";
            case FILLED -> "§aFilled";
            case BOTH -> "§eBoth";
        };
    }
}
