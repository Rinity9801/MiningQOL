package forfun.miningqol.client.gui;

import forfun.miningqol.client.CorpseESP;
import forfun.miningqol.client.MiningqolClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;


public class CorpseESPCategoryScreen extends Screen {
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 320;
    private static final int TOGGLE_WIDTH = 200;
    private static final int TOGGLE_HEIGHT = 40;
    private static final int TOGGLE_SPACING = 12;
    private static final int BACK_BUTTON_WIDTH = 80;
    private static final int BACK_BUTTON_HEIGHT = 30;

    private final Screen parent;
    private List<ToggleButton> toggleButtons;
    private int hoveredToggle = -1;
    private boolean backButtonHovered = false;

    private int windowX, windowY;

    private static class ToggleButton {
        final String label;
        final int color;
        final Runnable onToggle;
        boolean enabled;

        ToggleButton(String label, int color, boolean enabled, Runnable onToggle) {
            this.label = label;
            this.color = color;
            this.enabled = enabled;
            this.onToggle = onToggle;
        }
    }

    public CorpseESPCategoryScreen(Screen parent) {
        super(Text.literal("Corpse ESP Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        windowX = (this.width - WINDOW_WIDTH) / 2;
        windowY = (this.height - WINDOW_HEIGHT) / 2;

        if ((windowX & 1) != 0) windowX--;
        if ((windowY & 1) != 0) windowY--;

        setupToggles();
    }

    private void setupToggles() {
        toggleButtons = new ArrayList<>();

        toggleButtons.add(new ToggleButton("Lapis Corpses", 0x4488FF,
            CorpseESP.isLapisEnabled(), () -> {
                CorpseESP.toggleLapis();
                toggleButtons.get(0).enabled = CorpseESP.isLapisEnabled();
            }));

        toggleButtons.add(new ToggleButton("Tungsten Corpses", 0x88FF88,
            CorpseESP.isTungstenEnabled(), () -> {
                CorpseESP.toggleTungsten();
                toggleButtons.get(1).enabled = CorpseESP.isTungstenEnabled();
            }));

        toggleButtons.add(new ToggleButton("Umber Corpses", 0xFFAA44,
            CorpseESP.isUmberEnabled(), () -> {
                CorpseESP.toggleUmber();
                toggleButtons.get(2).enabled = CorpseESP.isUmberEnabled();
            }));

        toggleButtons.add(new ToggleButton("Vanguard Corpses", 0xFF4444,
            CorpseESP.isVanguardEnabled(), () -> {
                CorpseESP.toggleVanguard();
                toggleButtons.get(3).enabled = CorpseESP.isVanguardEnabled();
            }));
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        
        context.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xCC000000);
        context.fill(windowX + 2, windowY + 2, windowX + WINDOW_WIDTH - 2, windowY + WINDOW_HEIGHT - 2, 0x88000000);

        
        context.drawBorder(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF4488FF);
        context.drawBorder(windowX + 1, windowY + 1, WINDOW_WIDTH - 2, WINDOW_HEIGHT - 2, 0x884488FF);


        int titleWidth = this.textRenderer.getWidth(this.title);
        int titleX = windowX + (WINDOW_WIDTH - titleWidth) / 2;
        int titleY = windowY + 15;
        context.drawTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFFFF);

        drawToggles(context, mouseX, mouseY);
        drawBackButton(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawToggles(DrawContext context, int mouseX, int mouseY) {
        int startX = windowX + (WINDOW_WIDTH - TOGGLE_WIDTH) / 2;
        int startY = windowY + 60;

        if ((startX & 1) != 0) startX--;
        if ((startY & 1) != 0) startY--;

        hoveredToggle = -1;

        for (int i = 0; i < toggleButtons.size(); i++) {
            ToggleButton toggle = toggleButtons.get(i);
            int toggleY = startY + i * (TOGGLE_HEIGHT + TOGGLE_SPACING);

            boolean hovered = mouseX >= startX && mouseX <= startX + TOGGLE_WIDTH &&
                            mouseY >= toggleY && mouseY <= toggleY + TOGGLE_HEIGHT;

            if (hovered) hoveredToggle = i;

            drawToggle(context, toggle, startX, toggleY, hovered);
        }
    }

    private void drawToggle(DrawContext context, ToggleButton toggle, int x, int y, boolean hovered) {
        
        int bgColor = toggle.enabled ? 0xCC2A2A2A : 0xCC1A1A1A;
        if (hovered) {
            bgColor = toggle.enabled ? 0xCC353535 : 0xCC252525;
        }
        context.fill(x, y, x + TOGGLE_WIDTH, y + TOGGLE_HEIGHT, bgColor);

        
        int borderColor = toggle.enabled ? (0xFF000000 | toggle.color) : 0xFF404040;
        context.drawBorder(x, y, TOGGLE_WIDTH, TOGGLE_HEIGHT, borderColor);

        
        int indicatorColor = toggle.enabled ? (0xFF000000 | toggle.color) : 0xFF303030;
        context.fill(x, y, x + 6, y + TOGGLE_HEIGHT, indicatorColor);


        int labelX = x + 15;
        int labelY = y + (TOGGLE_HEIGHT - this.textRenderer.fontHeight) / 2;
        int labelColor = toggle.enabled ? 0xFFFFFFFF : 0xFF808080;
        context.drawTextWithShadow(this.textRenderer, toggle.label, labelX, labelY, labelColor);

        
        String statusText = toggle.enabled ? "ON" : "OFF";
        int statusWidth = this.textRenderer.getWidth(statusText);
        int statusX = x + TOGGLE_WIDTH - statusWidth - 10;
        int statusY = y + (TOGGLE_HEIGHT - this.textRenderer.fontHeight) / 2;
        int statusColor = toggle.enabled ? (0xFF000000 | toggle.color) : 0xFF606060;
        context.drawTextWithShadow(this.textRenderer, statusText, statusX, statusY, statusColor);
    }

    private void drawBackButton(DrawContext context, int mouseX, int mouseY) {
        int buttonX = windowX + (WINDOW_WIDTH - BACK_BUTTON_WIDTH) / 2;
        int buttonY = windowY + WINDOW_HEIGHT - BACK_BUTTON_HEIGHT - 15;

        if ((buttonX & 1) != 0) buttonX--;
        if ((buttonY & 1) != 0) buttonY--;

        backButtonHovered = mouseX >= buttonX && mouseX <= buttonX + BACK_BUTTON_WIDTH &&
                           mouseY >= buttonY && mouseY <= buttonY + BACK_BUTTON_HEIGHT;

        int buttonColor = backButtonHovered ? 0xCC3A3A3A : 0xCC2A2A2A;
        context.fill(buttonX, buttonY, buttonX + BACK_BUTTON_WIDTH, buttonY + BACK_BUTTON_HEIGHT, buttonColor);
        context.drawBorder(buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT,
                backButtonHovered ? 0xFF4488FF : 0xFF404040);

        String buttonText = "Back";
        int textWidth = this.textRenderer.getWidth(buttonText);
        int textX = buttonX + (BACK_BUTTON_WIDTH - textWidth) / 2;
        int textY = buttonY + (BACK_BUTTON_HEIGHT - this.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(this.textRenderer, buttonText, textX, textY,
                backButtonHovered ? 0xFFFFFFFF : 0xFFC8C8C8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (backButtonHovered) {
                this.close();
                return true;
            }

            if (hoveredToggle >= 0 && hoveredToggle < toggleButtons.size()) {
                ToggleButton toggle = toggleButtons.get(hoveredToggle);
                toggle.onToggle.run();
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
