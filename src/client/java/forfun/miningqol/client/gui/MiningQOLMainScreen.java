package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Main screen for Mining QOL features using card-based design
 */
public class MiningQOLMainScreen extends Screen {
    private static final int WINDOW_WIDTH = 650;
    private static final int WINDOW_HEIGHT = 360;
    private static final int CARD_WIDTH = 140;
    private static final int CARD_HEIGHT = 80;
    private static final int CARD_SPACING = 12;
    private static final int CARDS_PER_ROW = 4;
    private static final int CLOSE_BUTTON_SIZE = 25;

    private final Screen parent;
    private List<ConfigCard> configCards;
    private int hoveredCard = -1;
    private boolean closeButtonHovered = false;

    private int windowX, windowY;

    private static class ConfigCard {
        final String title;
        final String description;
        final int accentColor;
        final Runnable onClick;

        ConfigCard(String title, String description, int accentColor, Runnable onClick) {
            this.title = title;
            this.description = description;
            this.accentColor = accentColor;
            this.onClick = onClick;
        }
    }

    public MiningQOLMainScreen(Screen parent) {
        super(Text.literal("Mining QOL"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        windowX = (this.width - WINDOW_WIDTH) / 2;
        windowY = (this.height - WINDOW_HEIGHT) / 2;

        if ((windowX & 1) != 0) windowX--;
        if ((windowY & 1) != 0) windowY--;

        setupCards();
    }

    private void setupCards() {
        configCards = new ArrayList<>();

        configCards.add(new ConfigCard("Corpse ESP", "Track and highlight corpses", 0x4488FF, () -> {
            if (this.client != null) {
                this.client.setScreen(new CorpseESPCategoryScreen(this));
            }
        }));
        configCards.add(new ConfigCard("Mining Profit", "Track your earnings", 0x44FF44, () -> {
            if (this.client != null) {
                this.client.setScreen(new MiningProfitCategoryScreen(this));
            }
        }));
        configCards.add(new ConfigCard("Pickaxe Cooldown", "Display pickaxe ability cooldown", 0x4A90E2, () -> {
            if (this.client != null) {
                this.client.setScreen(new PickaxeCooldownCategoryScreen(this));
            }
        }));
        configCards.add(new ConfigCard("Miner Overlay", "Efficient miner display", 0xFF9944, () -> {
            if (this.client != null) {
                this.client.setScreen(new MinerOverlayCategoryScreen(this));
            }
        }));
        configCards.add(new ConfigCard("Block Outline", "Highlight mining blocks", 0x9966FF, () -> {
            if (this.client != null) {
                this.client.setScreen(new BlockOutlineCategoryScreen(this));
            }
        }));
        configCards.add(new ConfigCard("Name Hider", "Replace your name with custom text", 0xFF9944, () -> {
            if (this.client != null) {
                this.client.setScreen(new NameHiderCategoryScreen(this));
            }
        }));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // Main window background
        context.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xCC000000);
        context.fill(windowX + 2, windowY + 2, windowX + WINDOW_WIDTH - 2, windowY + WINDOW_HEIGHT - 2, 0x88000000);

        // Borders
        context.drawBorder(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF4488FF);
        context.drawBorder(windowX + 1, windowY + 1, WINDOW_WIDTH - 2, WINDOW_HEIGHT - 2, 0x884488FF);

        // Title
        int titleWidth = this.textRenderer.getWidth(this.title);
        int titleX = windowX + (WINDOW_WIDTH - titleWidth) / 2;
        int titleY = windowY + 15;
        context.drawTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFF);

        drawCloseButton(context, mouseX, mouseY);
        drawCards(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawCloseButton(DrawContext context, int mouseX, int mouseY) {
        int buttonX = windowX + WINDOW_WIDTH - CLOSE_BUTTON_SIZE - 10;
        int buttonY = windowY + 10;

        if ((buttonX & 1) != 0) buttonX--;
        if ((buttonY & 1) != 0) buttonY--;

        closeButtonHovered = mouseX >= buttonX && mouseX <= buttonX + CLOSE_BUTTON_SIZE &&
                            mouseY >= buttonY && mouseY <= buttonY + CLOSE_BUTTON_SIZE;

        int buttonColor = closeButtonHovered ? 0xCCC83C3C : 0xB428281C;
        context.fill(buttonX, buttonY, buttonX + CLOSE_BUTTON_SIZE, buttonY + CLOSE_BUTTON_SIZE, buttonColor);
        context.drawBorder(buttonX, buttonY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE,
                closeButtonHovered ? 0xFFFF6060 : 0xFF804040);

        String xText = "X";
        int xWidth = this.textRenderer.getWidth(xText);
        int xX = buttonX + (CLOSE_BUTTON_SIZE - xWidth) / 2;
        int xY = buttonY + (CLOSE_BUTTON_SIZE - this.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(this.textRenderer, xText, xX, xY, closeButtonHovered ? 0xFFFFFF : 0xC8C8C8);
    }

    private void drawCards(DrawContext context, int mouseX, int mouseY) {
        int contentAreaX = windowX + 10;
        int contentAreaY = windowY + 50;
        int contentAreaWidth = WINDOW_WIDTH - 20;

        int totalWidth = CARDS_PER_ROW * CARD_WIDTH + (CARDS_PER_ROW - 1) * CARD_SPACING;

        int startX = contentAreaX + (contentAreaWidth - totalWidth) / 2;
        if ((startX & 1) != 0) startX--;

        int startY = contentAreaY;
        if ((startY & 1) != 0) startY--;

        hoveredCard = -1;

        for (int i = 0; i < configCards.size(); i++) {
            ConfigCard card = configCards.get(i);

            int row = i / CARDS_PER_ROW;
            int col = i % CARDS_PER_ROW;

            int cardX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = startY + row * (CARD_HEIGHT + CARD_SPACING);

            boolean hovered = mouseX >= cardX && mouseX <= cardX + CARD_WIDTH &&
                             mouseY >= cardY && mouseY <= cardY + CARD_HEIGHT;

            if (hovered) hoveredCard = i;

            drawCard(context, card, cardX, cardY, hovered);
        }
    }

    private void drawCard(DrawContext context, ConfigCard card, int x, int y, boolean hovered) {
        int cardBg = hovered ? 0xCC2A2A2A : 0xCC1E1E1E;
        context.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, cardBg);

        int accentColor = 0xFF000000 | card.accentColor;
        context.fill(x, y, x + CARD_WIDTH, y + 8, accentColor);

        int borderColor = hovered ? accentColor : 0xFF404040;
        context.drawBorder(x, y, CARD_WIDTH, CARD_HEIGHT, borderColor);

        // Title
        int titleWidth = this.textRenderer.getWidth(card.title);
        int titleX = x + (CARD_WIDTH - titleWidth) / 2;
        int titleY = y + 18;
        context.drawTextWithShadow(this.textRenderer, card.title, titleX, titleY, 0xFFFFFF);

        // Description
        List<String> lines = wrapText(card.description, CARD_WIDTH - 16);
        int descColor = hovered ? accentColor : 0xFFB4B4B4;

        int lineY = y + 34;
        for (int i = 0; i < Math.min(lines.size(), 3); i++) {
            String line = lines.get(i);
            int lw = this.textRenderer.getWidth(line);
            int lineX = x + (CARD_WIDTH - lw) / 2;
            context.drawTextWithShadow(this.textRenderer, line, lineX, lineY, descColor);
            lineY += 10;
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (closeButtonHovered) {
                this.close();
                return true;
            }

            if (hoveredCard >= 0 && hoveredCard < configCards.size()) {
                ConfigCard card = configCards.get(hoveredCard);
                card.onClick.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (MiningqolClient.getConfig() != null) {
            MiningqolClient.getConfig().loadFromGame();
            MiningqolClient.getConfig().save();
        }

        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
