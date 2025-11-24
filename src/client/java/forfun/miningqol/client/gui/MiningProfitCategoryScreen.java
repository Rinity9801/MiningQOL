package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.profit.BazaarPriceManager;
import forfun.miningqol.client.profit.GemstoneTracker;
import forfun.miningqol.client.profit.ProfitTrackerHUD;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;


public class MiningProfitCategoryScreen extends Screen {
    private static final int WINDOW_WIDTH = 550;
    private static final int WINDOW_HEIGHT = 380;
    private static final int CONTROL_WIDTH = 400;
    private static final int CONTROL_HEIGHT = 35;
    private static final int CONTROL_SPACING = 8;
    private static final int BACK_BUTTON_WIDTH = 80;
    private static final int BACK_BUTTON_HEIGHT = 30;
    private static final int SLIDER_WIDTH = 200;

    private final Screen parent;
    private List<Control> controls;
    private int hoveredControl = -1;
    private boolean backButtonHovered = false;
    private int draggingSlider = -1;

    private int windowX, windowY;

    private interface Control {
        void render(DrawContext context, int x, int y, boolean hovered, int mouseX);
        boolean click(int mouseX, int mouseY, int x, int y);
        void drag(int mouseX, int x);
    }

    private static class ToggleControl implements Control {
        final String label;
        final Runnable onToggle;
        boolean enabled;

        ToggleControl(String label, boolean enabled, Runnable onToggle) {
            this.label = label;
            this.enabled = enabled;
            this.onToggle = onToggle;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX) {
            int bgColor = hovered ? 0xCC303030 : 0xCC242424;
            context.fill(x, y, x + CONTROL_WIDTH, y + CONTROL_HEIGHT, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, hovered ? 0xFF4488FF : 0xFF404040);

            int indicatorColor = enabled ? 0xFF44FF44 : 0xFF404040;
            context.fill(x + 3, y + 3, x + 9, y + CONTROL_HEIGHT - 3, indicatorColor);

            int labelX = x + 15;
            int labelY = y + (CONTROL_HEIGHT - 8) / 2;
            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFFFF);

            String statusText = enabled ? "ON" : "OFF";
            int statusX = x + CONTROL_WIDTH - textRenderer.getWidth(statusText) - 10;
            context.drawTextWithShadow(textRenderer, statusText, statusX, labelY, enabled ? 0xFF44FF44 : 0xFF808080);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            if (mouseX >= x && mouseX <= x + CONTROL_WIDTH && mouseY >= y && mouseY <= y + CONTROL_HEIGHT) {
                onToggle.run();
                return true;
            }
            return false;
        }

        @Override
        public void drag(int mouseX, int x) {}
    }

    private static class SelectorControl implements Control {
        final String label;
        final Runnable onClick;
        String currentValue;

        SelectorControl(String label, String currentValue, Runnable onClick) {
            this.label = label;
            this.currentValue = currentValue;
            this.onClick = onClick;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX) {
            int bgColor = hovered ? 0xCC303030 : 0xCC242424;
            context.fill(x, y, x + CONTROL_WIDTH, y + CONTROL_HEIGHT, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, hovered ? 0xFF4488FF : 0xFF404040);

            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int labelX = x + 10;
            int labelY = y + (CONTROL_HEIGHT - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFFFF);


            String valueText = "§e" + currentValue;
            int valueX = x + CONTROL_WIDTH - textRenderer.getWidth(valueText) - 10;
            context.drawTextWithShadow(textRenderer, valueText, valueX, labelY, 0xFFFFFFFF);


            String arrow = ">";
            int arrowX = valueX - textRenderer.getWidth(arrow) - 5;
            context.drawTextWithShadow(textRenderer, arrow, arrowX, labelY, 0xFF888888);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            if (mouseX >= x && mouseX <= x + CONTROL_WIDTH && mouseY >= y && mouseY <= y + CONTROL_HEIGHT) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        public void drag(int mouseX, int x) {}
    }

    private static class ButtonControl implements Control {
        final String label;
        final Runnable onClick;

        ButtonControl(String label, Runnable onClick) {
            this.label = label;
            this.onClick = onClick;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX) {
            int bgColor = hovered ? 0xCC3A3A3A : 0xCC2A2A2A;
            context.fill(x, y, x + CONTROL_WIDTH, y + CONTROL_HEIGHT, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, hovered ? 0xFF44FF44 : 0xFF404040);

            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int labelX = x + (CONTROL_WIDTH - textRenderer.getWidth(label)) / 2;
            int labelY = y + (CONTROL_HEIGHT - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, hovered ? 0xFFFFFFFF : 0xFFC8C8C8);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            if (mouseX >= x && mouseX <= x + CONTROL_WIDTH && mouseY >= y && mouseY <= y + CONTROL_HEIGHT) {
                onClick.run();
                return true;
            }
            return false;
        }

        @Override
        public void drag(int mouseX, int x) {}
    }

    private static class SliderControl implements Control {
        final String label;
        final int min, max;
        final Runnable onChange;
        int value;
        String suffix;

        SliderControl(String label, int min, int max, int value, String suffix, Runnable onChange) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.value = value;
            this.suffix = suffix;
            this.onChange = onChange;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX) {
            int bgColor = hovered ? 0xCC303030 : 0xCC242424;
            context.fill(x, y, x + CONTROL_WIDTH, y + CONTROL_HEIGHT, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, hovered ? 0xFF4488FF : 0xFF404040);

            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int labelX = x + 10;
            int labelY = y + 6;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFFFF);

            
            int sliderX = x + (CONTROL_WIDTH - SLIDER_WIDTH) / 2;
            int sliderY = y + 20;
            context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + 8, 0xFF1A1A1A);
            context.drawBorder(sliderX, sliderY, SLIDER_WIDTH, 8, 0xFF404040);

            
            float percent = (value - min) / (float) (max - min);
            int fillWidth = (int) (SLIDER_WIDTH * percent);
            context.fill(sliderX + 1, sliderY + 1, sliderX + fillWidth - 1, sliderY + 7, 0xFF4488FF);

            
            int handleX = sliderX + fillWidth - 4;
            context.fill(handleX, sliderY - 2, handleX + 8, sliderY + 10, hovered ? 0xFFFFFFFF : 0xFFC8C8C8);


            String valueText = value + suffix;
            int valueX = sliderX + SLIDER_WIDTH + 10;
            context.drawTextWithShadow(textRenderer, valueText, valueX, labelY, 0xFFFFFFFF);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            int sliderX = x + (CONTROL_WIDTH - SLIDER_WIDTH) / 2;
            int sliderY = y + 20;
            if (mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH &&
                mouseY >= sliderY - 2 && mouseY <= sliderY + 10) {
                updateValue(mouseX, sliderX);
                return true;
            }
            return false;
        }

        @Override
        public void drag(int mouseX, int x) {
            int sliderX = x + (CONTROL_WIDTH - SLIDER_WIDTH) / 2;
            updateValue(mouseX, sliderX);
        }

        private void updateValue(int mouseX, int sliderX) {
            int relativeX = Math.max(0, Math.min(SLIDER_WIDTH, mouseX - sliderX));
            float percent = relativeX / (float) SLIDER_WIDTH;
            value = min + Math.round(percent * (max - min));
            onChange.run();
        }
    }

    public MiningProfitCategoryScreen(Screen parent) {
        super(Text.literal("Mining Profit Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        windowX = (this.width - WINDOW_WIDTH) / 2;
        windowY = (this.height - WINDOW_HEIGHT) / 2;

        if ((windowX & 1) != 0) windowX--;
        if ((windowY & 1) != 0) windowY--;

        setupControls();
    }

    private void setupControls() {
        controls = new ArrayList<>();

        controls.add(new ToggleControl("Enable Profit Tracker",
            ProfitTrackerHUD.isEnabled(), () -> {
                ProfitTrackerHUD.setEnabled(!ProfitTrackerHUD.isEnabled());
                ((ToggleControl) controls.get(0)).enabled = ProfitTrackerHUD.isEnabled();
            }));

        controls.add(new SliderControl("Pristine Chance", 0, 100,
            GemstoneTracker.getPristineChance(), "%", () -> {
                SliderControl slider = (SliderControl) controls.get(1);
                GemstoneTracker.setPristineChance(slider.value);
            }));

        controls.add(new SelectorControl("Gem Tier", GemstoneTracker.getGemTierName(), () -> {
            int currentTier = GemstoneTracker.getGemTier();
            int nextTier = currentTier >= 3 ? 1 : currentTier + 1;
            GemstoneTracker.setGemTier(nextTier);
            ((SelectorControl) controls.get(2)).currentValue = GemstoneTracker.getGemTierName();
        }));

        controls.add(new ToggleControl("Include Rough Gemstones",
            GemstoneTracker.isIncludingRough(), () -> {
                GemstoneTracker.setIncludeRough(!GemstoneTracker.isIncludingRough());
                ((ToggleControl) controls.get(3)).enabled = GemstoneTracker.isIncludingRough();
            }));

        controls.add(new ToggleControl("Use NPC Prices Instead of Bazaar",
            BazaarPriceManager.isUsingNPCPrices(), () -> {
                BazaarPriceManager.setUseNPCPrices(!BazaarPriceManager.isUsingNPCPrices());
                ((ToggleControl) controls.get(4)).enabled = BazaarPriceManager.isUsingNPCPrices();
            }));

        controls.add(new ButtonControl("Set HUD Position", () -> {
            if (this.client != null) {
                this.client.setScreen(new ProfitPositionScreen(this));
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

        
        context.fill(windowX, windowY, windowX + WINDOW_WIDTH, windowY + WINDOW_HEIGHT, 0xCC000000);
        context.fill(windowX + 2, windowY + 2, windowX + WINDOW_WIDTH - 2, windowY + WINDOW_HEIGHT - 2, 0x88000000);

        
        context.drawBorder(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF44FF44);
        context.drawBorder(windowX + 1, windowY + 1, WINDOW_WIDTH - 2, WINDOW_HEIGHT - 2, 0x8844FF44);


        int titleWidth = this.textRenderer.getWidth(this.title);
        int titleX = windowX + (WINDOW_WIDTH - titleWidth) / 2;
        int titleY = windowY + 15;
        context.drawTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFFFF);

        drawControls(context, mouseX, mouseY);
        drawBackButton(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawControls(DrawContext context, int mouseX, int mouseY) {
        int startX = windowX + (WINDOW_WIDTH - CONTROL_WIDTH) / 2;
        int startY = windowY + 50;

        if ((startX & 1) != 0) startX--;
        if ((startY & 1) != 0) startY--;

        if (draggingSlider == -1) {
            hoveredControl = -1;
        }

        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);
            int controlY = startY + i * (CONTROL_HEIGHT + CONTROL_SPACING);

            boolean hovered = (draggingSlider == i) ||
                            (draggingSlider == -1 && mouseX >= startX && mouseX <= startX + CONTROL_WIDTH &&
                             mouseY >= controlY && mouseY <= controlY + CONTROL_HEIGHT);

            if (hovered && draggingSlider == -1) hoveredControl = i;

            control.render(context, startX, controlY, hovered, mouseX);

            if (draggingSlider == i) {
                control.drag(mouseX, startX);
            }
        }
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
                backButtonHovered ? 0xFF44FF44 : 0xFF404040);

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

            int startX = windowX + (WINDOW_WIDTH - CONTROL_WIDTH) / 2;
            int startY = windowY + 50;

            for (int i = 0; i < controls.size(); i++) {
                Control control = controls.get(i);
                int controlY = startY + i * (CONTROL_HEIGHT + CONTROL_SPACING);

                if (control.click((int) mouseX, (int) mouseY, startX, controlY)) {
                    if (control instanceof SliderControl) {
                        draggingSlider = i;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
