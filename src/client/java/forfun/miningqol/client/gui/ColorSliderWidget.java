package forfun.miningqol.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ColorSliderWidget extends SliderWidget {
    private final String label;
    private final Consumer<Float> onChange;
    private final int color;
    private final boolean isAlpha;

    public ColorSliderWidget(int x, int y, int width, int height, String label, float initialValue, int color, Consumer<Float> onChange) {
        super(x, y, width, height, Text.literal(label + ": " + formatValue(label, initialValue)), initialValue);
        this.label = label;
        this.onChange = onChange;
        this.color = color;
        this.isAlpha = label.contains("Alpha");
    }

    private static String formatValue(String label, float value) {
        if (label.contains("Alpha")) {
            return Math.round(value * 100) + "%";
        }
        return String.valueOf(Math.round(value * 255));
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Text.literal(label + ": " + formatValue(label, (float) this.value)));
    }

    @Override
    protected void applyValue() {
        onChange.accept((float) this.value);
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, this.getMessage());
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);

        // Draw color preview on the right side of slider
        int previewSize = this.height - 4;
        int previewX = this.getX() + this.width - previewSize - 2;
        int previewY = this.getY() + 2;

        // Create color with current value
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int displayColor = 0xFF000000 | ((int)(r * this.value) << 16) | ((int)(g * this.value) << 8) | (int)(b * this.value);

        context.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, displayColor);
        context.drawBorder(previewX, previewY, previewSize, previewSize, 0xFFFFFFFF);
    }
}
