package forfun.miningqol.client.gui;

import forfun.miningqol.client.BlockOutlineRenderer;
import forfun.miningqol.client.MiningqolClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;


public class BlockOutlineCategoryScreen extends Screen {
    private static final int WINDOW_WIDTH = 550;
    private static final int WINDOW_HEIGHT = 500;
    private static final int CONTROL_WIDTH = 400;
    private static final int CONTROL_HEIGHT = 35;
    private static final int CONTROL_SPACING = 8;
    private static final int BACK_BUTTON_WIDTH = 80;
    private static final int BACK_BUTTON_HEIGHT = 30;
    private static final int SLIDER_WIDTH = 200;
    private static final int MODE_BUTTON_WIDTH = 120;
    private static final int MODE_BUTTON_HEIGHT = 30;

    private final Screen parent;
    private List<Control> controls;
    private int hoveredControl = -1;
    private boolean backButtonHovered = false;
    private int draggingSlider = -1;
    private int hoveredModeButton = -1;

    private int windowX, windowY;

    private interface Control {
        void render(DrawContext context, int x, int y, boolean hovered, int mouseX, int mouseY);
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
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX, int mouseY) {
            int bgColor = hovered ? 0xCC303030 : 0xCC242424;
            context.fill(x, y, x + CONTROL_WIDTH, y + CONTROL_HEIGHT, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, hovered ? 0xFF9966FF : 0xFF404040);

            int indicatorColor = enabled ? 0xFF9966FF : 0xFF404040;
            context.fill(x + 3, y + 3, x + 9, y + CONTROL_HEIGHT - 3, indicatorColor);

            int labelX = x + 15;
            int labelY = y + (CONTROL_HEIGHT - 8) / 2;
            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFFFF);

            String statusText = enabled ? "ON" : "OFF";
            int statusX = x + CONTROL_WIDTH - textRenderer.getWidth(statusText) - 10;
            context.drawTextWithShadow(textRenderer, statusText, statusX, labelY, enabled ? 0xFF9966FF : 0xFF808080);
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
        public int getHeight() {
            return CONTROL_HEIGHT;
        }
    }

    private class ModeControl implements Control {
        final String label;
        BlockOutlineRenderer.OutlineMode currentMode;

        ModeControl(String label, BlockOutlineRenderer.OutlineMode currentMode) {
            this.label = label;
            this.currentMode = currentMode;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX, int mouseY) {
            context.drawTextWithShadow(textRenderer, label, x + 10, y + 6, 0xFFFFFFFF);

            int buttonY = y + 20;
            int startX = x + (CONTROL_WIDTH - (MODE_BUTTON_WIDTH * 3 + 20)) / 2;

            String[] modes = {"BOTH", "FILLED", "OUTLINE_ONLY"};
            BlockOutlineRenderer.OutlineMode[] modeValues = {
                BlockOutlineRenderer.OutlineMode.BOTH,
                BlockOutlineRenderer.OutlineMode.FILLED,
                BlockOutlineRenderer.OutlineMode.OUTLINE_ONLY
            };

            hoveredModeButton = -1;

            for (int i = 0; i < modes.length; i++) {
                int buttonX = startX + i * (MODE_BUTTON_WIDTH + 10);
                boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + MODE_BUTTON_WIDTH &&
                                   mouseY >= buttonY && mouseY <= buttonY + MODE_BUTTON_HEIGHT;
                boolean isSelected = currentMode == modeValues[i];

                if (isHovered) hoveredModeButton = i;

                int bgColor = isSelected ? 0xFF9966FF : (isHovered ? 0xCC404040 : 0xCC242424);
                context.fill(buttonX, buttonY, buttonX + MODE_BUTTON_WIDTH, buttonY + MODE_BUTTON_HEIGHT, bgColor);
                context.drawBorder(buttonX, buttonY, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT,
                        isSelected ? 0xFFBB99FF : 0xFF606060);

                int textX = buttonX + (MODE_BUTTON_WIDTH - textRenderer.getWidth(modes[i])) / 2;
                int textY = buttonY + (MODE_BUTTON_HEIGHT - textRenderer.fontHeight) / 2;
                context.drawTextWithShadow(textRenderer, modes[i], textX, textY,
                        isSelected ? 0xFFFFFFFF : 0xFFC8C8C8);
            }
        }

        @Override
        public boolean click(int mouseX, int mouseY, int x, int y) {
            int buttonY = y + 20;
            int startX = x + (CONTROL_WIDTH - (MODE_BUTTON_WIDTH * 3 + 20)) / 2;

            BlockOutlineRenderer.OutlineMode[] modeValues = {
                BlockOutlineRenderer.OutlineMode.BOTH,
                BlockOutlineRenderer.OutlineMode.FILLED,
                BlockOutlineRenderer.OutlineMode.OUTLINE_ONLY
            };

            for (int i = 0; i < 3; i++) {
                int buttonX = startX + i * (MODE_BUTTON_WIDTH + 10);
                if (mouseX >= buttonX && mouseX <= buttonX + MODE_BUTTON_WIDTH &&
                    mouseY >= buttonY && mouseY <= buttonY + MODE_BUTTON_HEIGHT) {
                    currentMode = modeValues[i];
                    BlockOutlineRenderer.setMode(modeValues[i]);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void drag(int mouseX, int x) {}

        @Override
        public int getHeight() {
            return 55;
        }
    }

    private static class ColorSliderControl implements Control {
        final String label;
        final Runnable onChange;
        float value;

        ColorSliderControl(String label, float value, Runnable onChange) {
            this.label = label;
            this.value = value;
            this.onChange = onChange;
        }

        @Override
        public void render(DrawContext context, int x, int y, boolean hovered, int mouseX, int mouseY) {
            int bgColor = hovered ? 0xCC303030 : 0xCC242424;
            context.fill(x, y, x + CONTROL_WIDTH, y + CONTROL_HEIGHT, bgColor);
            context.drawBorder(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, hovered ? 0xFF9966FF : 0xFF404040);

            net.minecraft.client.font.TextRenderer textRenderer = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int labelX = x + 10;
            int labelY = y + 6;
            context.drawTextWithShadow(textRenderer, label, labelX, labelY, 0xFFFFFFFF);

            
            int sliderX = x + (CONTROL_WIDTH - SLIDER_WIDTH) / 2;
            int sliderY = y + 20;
            context.fill(sliderX, sliderY, sliderX + SLIDER_WIDTH, sliderY + 8, 0xFF1A1A1A);
            context.drawBorder(sliderX, sliderY, SLIDER_WIDTH, 8, 0xFF404040);

            
            int fillWidth = (int) (SLIDER_WIDTH * value);
            context.fill(sliderX + 1, sliderY + 1, sliderX + fillWidth - 1, sliderY + 7, 0xFF9966FF);

            
            int handleX = sliderX + fillWidth - 4;
            context.fill(handleX, sliderY - 2, handleX + 8, sliderY + 10, hovered ? 0xFFFFFFFF : 0xFFC8C8C8);


            String valueText = String.format("%.2f", value);
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

        @Override
        public int getHeight() {
            return CONTROL_HEIGHT;
        }

        private void updateValue(int mouseX, int sliderX) {
            int relativeX = Math.max(0, Math.min(SLIDER_WIDTH, mouseX - sliderX));
            value = relativeX / (float) SLIDER_WIDTH;
            onChange.run();
        }
    }

    public BlockOutlineCategoryScreen(Screen parent) {
        super(Text.literal("Block Outline Settings"));
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

        controls.add(new ToggleControl("Enable Block Outline",
            BlockOutlineRenderer.isEnabled(), () -> {
                BlockOutlineRenderer.setEnabled(!BlockOutlineRenderer.isEnabled());
                ((ToggleControl) controls.get(0)).enabled = BlockOutlineRenderer.isEnabled();
            }));

        controls.add(new ModeControl("Outline Mode", BlockOutlineRenderer.getMode()));

        controls.add(new ColorSliderControl("Red", BlockOutlineRenderer.getRed(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(2);
            BlockOutlineRenderer.setColor(slider.value, BlockOutlineRenderer.getGreen(),
                    BlockOutlineRenderer.getBlue(), BlockOutlineRenderer.getAlpha());
        }));

        controls.add(new ColorSliderControl("Green", BlockOutlineRenderer.getGreen(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(3);
            BlockOutlineRenderer.setColor(BlockOutlineRenderer.getRed(), slider.value,
                    BlockOutlineRenderer.getBlue(), BlockOutlineRenderer.getAlpha());
        }));

        controls.add(new ColorSliderControl("Blue", BlockOutlineRenderer.getBlue(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(4);
            BlockOutlineRenderer.setColor(BlockOutlineRenderer.getRed(), BlockOutlineRenderer.getGreen(),
                    slider.value, BlockOutlineRenderer.getAlpha());
        }));

        controls.add(new ColorSliderControl("Alpha (Transparency)", BlockOutlineRenderer.getAlpha(), () -> {
            ColorSliderControl slider = (ColorSliderControl) controls.get(5);
            BlockOutlineRenderer.setColor(BlockOutlineRenderer.getRed(), BlockOutlineRenderer.getGreen(),
                    BlockOutlineRenderer.getBlue(), slider.value);
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

        
        context.drawBorder(windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 0xFF9966FF);
        context.drawBorder(windowX + 1, windowY + 1, WINDOW_WIDTH - 2, WINDOW_HEIGHT - 2, 0x889966FF);


        int titleWidth = this.textRenderer.getWidth(this.title);
        int titleX = windowX + (WINDOW_WIDTH - titleWidth) / 2;
        int titleY = windowY + 15;
        context.drawTextWithShadow(this.textRenderer, this.title, titleX, titleY, 0xFFFFFFFF);


        String desc = "Customize mining block highlights";
        int descWidth = this.textRenderer.getWidth(desc);
        int descX = windowX + (WINDOW_WIDTH - descWidth) / 2;
        int descY = windowY + 30;
        context.drawTextWithShadow(this.textRenderer, desc, descX, descY, 0xFFAAAAAA);

        
        drawColorPreview(context);

        drawControls(context, mouseX, mouseY);
        drawBackButton(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawColorPreview(DrawContext context) {
        int previewSize = 60;
        int previewX = windowX + WINDOW_WIDTH - previewSize - 20;
        int previewY = windowY + 50;

        
        float r = BlockOutlineRenderer.getRed();
        float g = BlockOutlineRenderer.getGreen();
        float b = BlockOutlineRenderer.getBlue();
        float a = BlockOutlineRenderer.getAlpha();

        int color = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);

        
        for (int y = 0; y < previewSize; y += 10) {
            for (int x = 0; x < previewSize; x += 10) {
                boolean isEven = ((x / 10) + (y / 10)) % 2 == 0;
                int bgColor = isEven ? 0xFFCCCCCC : 0xFF999999;
                context.fill(previewX + x, previewY + y, previewX + x + 10, previewY + y + 10, bgColor);
            }
        }

        
        context.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, color);
        context.drawBorder(previewX, previewY, previewSize, previewSize, 0xFF606060);

        String previewLabel = "Preview";
        int labelX = previewX + (previewSize - textRenderer.getWidth(previewLabel)) / 2;
        context.drawTextWithShadow(textRenderer, previewLabel, labelX, previewY - 12, 0xFFFFFFFF);
    }

    private void drawControls(DrawContext context, int mouseX, int mouseY) {
        int startX = windowX + (WINDOW_WIDTH - CONTROL_WIDTH) / 2;
        int startY = windowY + 130;

        if ((startX & 1) != 0) startX--;
        if ((startY & 1) != 0) startY--;

        if (draggingSlider == -1) {
            hoveredControl = -1;
        }

        int currentY = startY;

        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);

            boolean hovered = (draggingSlider == i) ||
                            (draggingSlider == -1 && mouseX >= startX && mouseX <= startX + CONTROL_WIDTH &&
                             mouseY >= currentY && mouseY <= currentY + control.getHeight());

            if (hovered && draggingSlider == -1) hoveredControl = i;

            control.render(context, startX, currentY, hovered, mouseX, mouseY);

            if (draggingSlider == i) {
                control.drag(mouseX, startX);
            }

            currentY += control.getHeight() + CONTROL_SPACING;
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
                backButtonHovered ? 0xFF9966FF : 0xFF404040);

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
            int startY = windowY + 130;
            int currentY = startY;

            for (int i = 0; i < controls.size(); i++) {
                Control control = controls.get(i);

                if (control.click((int) mouseX, (int) mouseY, startX, currentY)) {
                    if (control instanceof ColorSliderControl) {
                        draggingSlider = i;
                    }
                    return true;
                }

                currentY += control.getHeight() + CONTROL_SPACING;
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
