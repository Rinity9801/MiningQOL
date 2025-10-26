package forfun.miningqol.client.gui;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.NameHider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Category screen for Name Hider settings
 */
public class NameHiderCategoryScreen extends Screen {
    private static final int WINDOW_WIDTH = 550;
    private static final int WINDOW_HEIGHT = 450;
    private static final int CONTROL_WIDTH = 400;
    private static final int CONTROL_HEIGHT = 35;
    private static final int CONTROL_SPACING = 8;
    private static final int BACK_BUTTON_WIDTH = 80;
    private static final int BACK_BUTTON_HEIGHT = 30;
    private static final int SLIDER_WIDTH = 120;
    private static final int TEXT_FIELD_HEIGHT = 20;

    private final Screen parent;
    private List<Control> controls;
    private int hoveredControl = -1;
    private boolean backButtonHovered = false;
    private int draggingSlider = -1;
    private TextFieldWidget nameField;

    private int windowX, windowY;

    private interface Control {
        void render(DrawContext context, int x, int y, boolean hovered, int mouseX);
        boolean click(int mouseX, int mouseY, int x, int y);
        void drag(int mouseX, int x);
        int getHeight();
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
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFF);

            String statusText = enabled ? "ON" : "OFF";
            int statusX = x + CONTROL_WIDTH - textRenderer.getWidth(statusText) - 10;
            context.drawTextWithShadow(textRenderer, statusText, statusX, labelY, enabled ? 0x44FF44 : 0x808080);
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

        @Override
        public int getHeight() { return CONTROL_HEIGHT; }
    }

    private static class TextFieldControl implements Control {
        final String label;
        TextFieldWidget textField;

        TextFieldControl(String label, TextFieldWidget textField) {
            this.label = label;
            this.textField = textField;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX) {
            int bgColor = hovered ? 0xCC303030 : 0xCC242424;
            context.fill(x, y, x + CONTROL_WIDTH, y + CONTROL_HEIGHT, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, hovered ? 0xFF4488FF : 0xFF404040);

            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int labelX = x + 10;
            int labelY = y + 5;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFF);

            // Position text field
            textField.setX(x + 10);
            textField.setY(y + 17);
            textField.setWidth(CONTROL_WIDTH - 20);
            textField.render(context, mouseX, y, 0);
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            boolean clicked = textField.mouseClicked(mouseX, mouseY, 0);
            if (clicked) {
                textField.setFocused(true);
            }
            return clicked;
        }

        @Override
        public void drag(int mouseX, int x) {}

        @Override
        public int getHeight() { return CONTROL_HEIGHT; }
    }

    private static class ColorSliderControl implements Control {
        final String label;
        final String channelName;
        final Runnable onChange;
        float value;

        ColorSliderControl(String label, String channelName, float value, Runnable onChange) {
            this.label = label;
            this.channelName = channelName;
            this.value = value;
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
            context.drawTextWithShadow(textRenderer, label + " - " + channelName, labelX, labelY, 0xFFFFFF);

            // Slider bar
            int sliderX = x + 10;
            int sliderY = y + 20;
            context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + 8, 0xFF1A1A1A);
            context.drawBorder(sliderX, sliderY, SLIDER_WIDTH, 8, 0xFF404040);

            // Slider fill with color preview
            int fillWidth = (int) (SLIDER_WIDTH * value);
            int previewColor = getPreviewColor();
            context.fill(sliderX + 1, sliderY + 1, sliderX + fillWidth - 1, sliderY + 7, previewColor);

            // Slider handle
            int handleX = sliderX + fillWidth - 4;
            context.fill(handleX, sliderY - 2, handleX + 8, sliderY + 10, hovered ? 0xFFFFFFFF : 0xFFC8C8C8);

            // Value display
            String valueText = String.format("%d", (int)(value * 255));
            int valueX = sliderX + SLIDER_WIDTH + 10;
            context.drawTextWithShadow(textRenderer, valueText, valueX, labelY, 0xFFFFFF);
        }

        private int getPreviewColor() {
            int val = (int)(value * 255);
            if (channelName.equals("Red")) {
                return 0xFF000000 | (val << 16);
            } else if (channelName.equals("Green")) {
                return 0xFF000000 | (val << 8);
            } else {
                return 0xFF000000 | val;
            }
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            int sliderX = x + 10;
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
            int sliderX = x + 10;
            updateValue(mouseX, sliderX);
        }

        private void updateValue(int mouseX, int sliderX) {
            int relativeX = Math.max(0, Math.min(SLIDER_WIDTH, mouseX - sliderX));
            value = relativeX / (float) SLIDER_WIDTH;
            onChange.run();
        }

        @Override
        public int getHeight() { return CONTROL_HEIGHT; }
    }

    private static class PreviewControl implements Control {
        final String label;

        PreviewControl(String label) {
            this.label = label;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX) {
            int bgColor = 0xCC242424;
            context.fill(x, y, x + CONTROL_WIDTH, y + 45, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, 45, 0xFF404040);

            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int labelX = x + 10;
            int labelY = y + 5;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFF);

            // Preview text
            String previewText = NameHider.getReplacementName();
            if (previewText.isEmpty()) previewText = "Preview";

            int previewY = y + 22;
            if (NameHider.isUsingGradient()) {
                // Draw gradient preview
                drawGradientText(context, textRenderer, previewText, x + CONTROL_WIDTH / 2, previewY);
            } else {
                // Draw solid color preview
                int color = rgbToInt(NameHider.getRed1(), NameHider.getGreen1(), NameHider.getBlue1());
                int textWidth = textRenderer.getWidth(previewText);
                context.drawTextWithShadow(textRenderer, previewText, x + (CONTROL_WIDTH - textWidth) / 2, previewY, 0xFF000000 | color);
            }
        }

        private void drawGradientText(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, String text, int centerX, int y) {
            int totalWidth = textRenderer.getWidth(text);
            int currentX = centerX - totalWidth / 2;

            for (int i = 0; i < text.length(); i++) {
                float ratio = text.length() == 1 ? 0.5f : (float) i / (text.length() - 1);
                float r = NameHider.getRed1() + (NameHider.getRed2() - NameHider.getRed1()) * ratio;
                float g = NameHider.getGreen1() + (NameHider.getGreen2() - NameHider.getGreen1()) * ratio;
                float b = NameHider.getBlue1() + (NameHider.getBlue2() - NameHider.getBlue1()) * ratio;

                int color = rgbToInt(r, g, b);
                String character = String.valueOf(text.charAt(i));
                context.drawTextWithShadow(textRenderer, character, currentX, y, 0xFF000000 | color);
                currentX += textRenderer.getWidth(character);
            }
        }

        private int rgbToInt(float r, float g, float b) {
            int ri = Math.max(0, Math.min(255, (int)(r * 255)));
            int gi = Math.max(0, Math.min(255, (int)(g * 255)));
            int bi = Math.max(0, Math.min(255, (int)(b * 255)));
            return (ri << 16) | (gi << 8) | bi;
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            return false;
        }

        @Override
        public void drag(int mouseX, int x) {}

        @Override
        public int getHeight() { return 45; }
    }

    public NameHiderCategoryScreen(Screen parent) {
        super(Text.literal("Name Hider Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        windowX = (this.width - WINDOW_WIDTH) / 2;
        windowY = (this.height - WINDOW_HEIGHT) / 2;

        if ((windowX & 1) != 0) windowX--;
        if ((windowY & 1) != 0) windowY--;

        // Create text field for replacement name
        nameField = new TextFieldWidget(this.textRenderer, 0, 0, CONTROL_WIDTH - 20, TEXT_FIELD_HEIGHT, Text.literal("Name"));
        nameField.setMaxLength(16);
        nameField.setText(NameHider.getReplacementName());
        nameField.setChangedListener(text -> {
            NameHider.setReplacementName(text);
        });
        this.addSelectableChild(nameField);

        setupControls();
    }

    private void setupControls() {
        controls = new ArrayList<>();

        controls.add(new ToggleControl("Enable Name Hider",
            NameHider.isEnabled(), () -> {
                NameHider.setEnabled(!NameHider.isEnabled());
                ((ToggleControl) controls.get(0)).enabled = NameHider.isEnabled();
            }));

        controls.add(new TextFieldControl("Replacement Name", nameField));

        controls.add(new ToggleControl("Use Gradient",
            NameHider.isUsingGradient(), () -> {
                NameHider.setUseGradient(!NameHider.isUsingGradient());
                ((ToggleControl) controls.get(2)).enabled = NameHider.isUsingGradient();
            }));

        controls.add(new ColorSliderControl("Color 1", "Red", NameHider.getRed1(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(3);
            NameHider.setColor1(slider.value, NameHider.getGreen1(), NameHider.getBlue1());
        }));

        controls.add(new ColorSliderControl("Color 1", "Green", NameHider.getGreen1(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(4);
            NameHider.setColor1(NameHider.getRed1(), slider.value, NameHider.getBlue1());
        }));

        controls.add(new ColorSliderControl("Color 1", "Blue", NameHider.getBlue1(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(5);
            NameHider.setColor1(NameHider.getRed1(), NameHider.getGreen1(), slider.value);
        }));

        controls.add(new ColorSliderControl("Color 2", "Red", NameHider.getRed2(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(6);
            NameHider.setColor2(slider.value, NameHider.getGreen2(), NameHider.getBlue2());
        }));

        controls.add(new ColorSliderControl("Color 2", "Green", NameHider.getGreen2(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(7);
            NameHider.setColor2(NameHider.getRed2(), slider.value, NameHider.getBlue2());
        }));

        controls.add(new ColorSliderControl("Color 2", "Blue", NameHider.getBlue2(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(8);
            NameHider.setColor2(NameHider.getRed2(), NameHider.getGreen2(), slider.value);
        }));

        controls.add(new PreviewControl("Preview"));
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
        context.drawBorder(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFFFF9944);
        context.drawBorder(windowX + 1, windowY + 1, WINDOW_WIDTH - 2, WINDOW_HEIGHT - 2, 0x88FF9944);

        // Title
        int titleWidth = this.textRenderer.getWidth(this.title);
        int titleX = windowX + (WINDOW_WIDTH - titleWidth) / 2;
        int titleY = windowY + 15;
        context.drawTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFF);

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

        int currentY = startY;
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);
            int controlHeight = control.getHeight();

            boolean hovered = (draggingSlider == i) ||
                            (draggingSlider == -1 && mouseX >= startX && mouseX <= startX + CONTROL_WIDTH &&
                             mouseY >= currentY && mouseY <= currentY + controlHeight);

            if (hovered && draggingSlider == -1) hoveredControl = i;

            control.render(context, startX, currentY, hovered, mouseX);

            if (draggingSlider == i) {
                control.drag(mouseX, startX);
            }

            currentY += controlHeight + CONTROL_SPACING;
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
                backButtonHovered ? 0xFFFF9944 : 0xFF404040);

        String buttonText = "Back";
        int textWidth = this.textRenderer.getWidth(buttonText);
        int textX = buttonX + (BACK_BUTTON_WIDTH - textWidth) / 2;
        int textY = buttonY + (BACK_BUTTON_HEIGHT - this.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(this.textRenderer, buttonText, textX, textY,
                backButtonHovered ? 0xFFFFFF : 0xC8C8C8);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (backButtonHovered) {
                this.close();
                return true;
            }

            int startX = windowX + (WINDOW_WIDTH - CONTROL_WIDTH) / 2;
            int currentY = windowY + 50;

            boolean clickedAnyControl = false;
            for (int i = 0; i < controls.size(); i++) {
                Control control = controls.get(i);
                int controlHeight = control.getHeight();

                if (control.click((int) mouseX, (int) mouseY, startX, currentY)) {
                    if (control instanceof ColorSliderControl) {
                        draggingSlider = i;
                    }
                    clickedAnyControl = true;
                    // If we didn't click the text field, unfocus it
                    if (!(control instanceof TextFieldControl)) {
                        nameField.setFocused(false);
                    }
                    return true;
                }

                currentY += controlHeight + CONTROL_SPACING;
            }

            // If we clicked outside all controls, unfocus the text field
            if (!clickedAnyControl) {
                nameField.setFocused(false);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (nameField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
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
