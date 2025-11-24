package forfun.miningqol.client.gui;

import forfun.miningqol.client.CommandKeybindManager;
import forfun.miningqol.client.MiningqolClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CommandKeybindCategoryScreen extends Screen {
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 450;
    private static final int ENTRY_HEIGHT = 40;
    private static final int BACK_BUTTON_WIDTH = 80;
    private static final int BACK_BUTTON_HEIGHT = 30;
    private static final int ADD_BUTTON_WIDTH = 100;
    private static final int ADD_BUTTON_HEIGHT = 30;

    private final Screen parent;
    private int windowX, windowY;
    private boolean backButtonHovered = false;
    private boolean addButtonHovered = false;
    private int scrollOffset = 0;
    private List<KeybindEntry> entries;
    private int listeningEntry = -1;
    private boolean ignoreNextRelease = false;

    private static class KeybindEntry {
        int keyCode;
        String command;
        TextFieldWidget commandField;
        boolean deleteHovered = false;
        boolean keyHovered = false;
    }

    public CommandKeybindCategoryScreen(Screen parent) {
        super(Text.literal("Command Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        windowX = (this.width - WINDOW_WIDTH) / 2;
        windowY = (this.height - WINDOW_HEIGHT) / 2;

        if ((windowX & 1) != 0) windowX--;
        if ((windowY & 1) != 0) windowY--;

        loadEntries();
    }

    private void loadEntries() {
        entries = new ArrayList<>();
        Map<Integer, String> keybinds = CommandKeybindManager.getAllKeybinds();

        for (Map.Entry<Integer, String> entry : keybinds.entrySet()) {
            KeybindEntry ke = new KeybindEntry();
            ke.keyCode = entry.getKey();
            ke.command = entry.getValue();
            ke.commandField = new TextFieldWidget(this.textRenderer, 0, 0, 300, 20, Text.literal(""));
            ke.commandField.setText(entry.getValue());
            ke.commandField.setMaxLength(256);
            entries.add(ke);
        }
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

        drawEntries(context, mouseX, mouseY);
        drawAddButton(context, mouseX, mouseY);
        drawBackButton(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawEntries(DrawContext context, int mouseX, int mouseY) {
        int startY = windowY + 50;
        int maxHeight = WINDOW_HEIGHT - 120;
        int entryY = startY;

        for (int i = 0; i < entries.size(); i++) {
            KeybindEntry entry = entries.get(i);

            if (entryY - scrollOffset >= startY && entryY - scrollOffset < startY + maxHeight) {
                int displayY = entryY - scrollOffset;

                context.fill(windowX + 10, displayY, windowX + WINDOW_WIDTH - 10, displayY + ENTRY_HEIGHT, 0xCC2A2A2A);
                context.drawBorder(windowX + 10, displayY, WINDOW_WIDTH - 20, ENTRY_HEIGHT, 0xFF404040);

                String keyName = listeningEntry == i ? "..." : InputUtil.Type.KEYSYM.createFromCode(entry.keyCode).getLocalizedText().getString();
                int keyButtonX = windowX + 20;
                int keyButtonY = displayY + 10;
                int keyButtonWidth = 100;
                int keyButtonHeight = 20;

                entry.keyHovered = mouseX >= keyButtonX && mouseX <= keyButtonX + keyButtonWidth &&
                                  mouseY >= keyButtonY && mouseY <= keyButtonY + keyButtonHeight;

                context.fill(keyButtonX, keyButtonY, keyButtonX + keyButtonWidth, keyButtonY + keyButtonHeight,
                    entry.keyHovered ? 0xCC3A3A3A : 0xCC2A2A2A);
                context.drawBorder(keyButtonX, keyButtonY, keyButtonWidth, keyButtonHeight, 0xFF4488FF);

                int keyTextX = keyButtonX + (keyButtonWidth - this.textRenderer.getWidth(keyName)) / 2;
                context.drawTextWithShadow(this.textRenderer, keyName, keyTextX, keyButtonY + 6, 0xFFFFFFFF);

                entry.commandField.setPosition(windowX + 130, displayY + 10);
                entry.commandField.render(context, mouseX, mouseY, 0);

                int deleteX = windowX + WINDOW_WIDTH - 70;
                int deleteY = displayY + 10;
                int deleteWidth = 50;
                int deleteHeight = 20;

                entry.deleteHovered = mouseX >= deleteX && mouseX <= deleteX + deleteWidth &&
                                     mouseY >= deleteY && mouseY <= deleteY + deleteHeight;

                context.fill(deleteX, deleteY, deleteX + deleteWidth, deleteY + deleteHeight,
                    entry.deleteHovered ? 0xCCC83C3C : 0xCC2A2A2A);
                context.drawBorder(deleteX, deleteY, deleteWidth, deleteHeight, 0xFFFF4444);

                String deleteText = "Delete";
                int deleteTextX = deleteX + (deleteWidth - this.textRenderer.getWidth(deleteText)) / 2;
                context.drawTextWithShadow(this.textRenderer, deleteText, deleteTextX, deleteY + 6, 0xFFFFFFFF);
            }

            entryY += ENTRY_HEIGHT + 5;
        }
    }

    private void drawAddButton(DrawContext context, int mouseX, int mouseY) {
        int buttonX = windowX + (WINDOW_WIDTH - ADD_BUTTON_WIDTH) / 2;
        int buttonY = windowY + WINDOW_HEIGHT - 80;

        addButtonHovered = mouseX >= buttonX && mouseX <= buttonX + ADD_BUTTON_WIDTH &&
                          mouseY >= buttonY && mouseY <= buttonY + ADD_BUTTON_HEIGHT;

        int buttonColor = addButtonHovered ? 0xCC3A3A3A : 0xCC2A2A2A;
        context.fill(buttonX, buttonY, buttonX + ADD_BUTTON_WIDTH, buttonY + ADD_BUTTON_HEIGHT, buttonColor);
        context.drawBorder(buttonX, buttonY, ADD_BUTTON_WIDTH, ADD_BUTTON_HEIGHT,
                addButtonHovered ? 0xFF44FF44 : 0xFF404040);

        String buttonText = "Add Keybind";
        int textWidth = this.textRenderer.getWidth(buttonText);
        int textX = buttonX + (ADD_BUTTON_WIDTH - textWidth) / 2;
        int textY = buttonY + (ADD_BUTTON_HEIGHT - this.textRenderer.fontHeight) / 2;
        context.drawTextWithShadow(this.textRenderer, buttonText, textX, textY,
                addButtonHovered ? 0xFFFFFFFF : 0xFFC8C8C8);
    }

    private void drawBackButton(DrawContext context, int mouseX, int mouseY) {
        int buttonX = windowX + (WINDOW_WIDTH - BACK_BUTTON_WIDTH) / 2;
        int buttonY = windowY + WINDOW_HEIGHT - BACK_BUTTON_HEIGHT - 15;

        backButtonHovered = mouseX >= buttonX && mouseX <= buttonX + BACK_BUTTON_WIDTH &&
                           mouseY >= buttonY && mouseY <= buttonY + BACK_BUTTON_HEIGHT;

        int buttonColor = backButtonHovered ? 0xCC3A3A3A : 0xCC2A2A2A;
        context.fill(buttonX, buttonY, buttonX + BACK_BUTTON_WIDTH, buttonY + BACK_BUTTON_HEIGHT, buttonColor);
        context.drawBorder(buttonX, buttonY, BACK_BUTTON_WIDTH, BACK_BUTTON_HEIGHT,
                backButtonHovered ? 0xFFFF4444 : 0xFF404040);

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

            if (addButtonHovered) {
                KeybindEntry entry = new KeybindEntry();
                entry.keyCode = GLFW.GLFW_KEY_UNKNOWN;
                entry.command = "";
                entry.commandField = new TextFieldWidget(this.textRenderer, 0, 0, 300, 20, Text.literal(""));
                entry.commandField.setText("");
                entry.commandField.setMaxLength(256);
                entries.add(entry);
                return true;
            }

            for (int i = 0; i < entries.size(); i++) {
                KeybindEntry entry = entries.get(i);

                if (entry.deleteHovered) {
                    CommandKeybindManager.removeKeybind(entry.keyCode);
                    entries.remove(i);
                    return true;
                }

                if (entry.keyHovered) {
                    listeningEntry = i;
                    ignoreNextRelease = true;
                    for (KeybindEntry e : entries) {
                        e.commandField.setFocused(false);
                    }
                    return true;
                }

                if (entry.commandField.mouseClicked(mouseX, mouseY, button)) {
                    for (KeybindEntry e : entries) {
                        e.commandField.setFocused(false);
                    }
                    entry.commandField.setFocused(true);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ignoreNextRelease) {
            ignoreNextRelease = false;
            return super.mouseReleased(mouseX, mouseY, button);
        }

        if (listeningEntry >= 0 && listeningEntry < entries.size()) {
            if (button >= 0 && button <= 8) {
                int mouseKeyCode = GLFW.GLFW_KEY_LAST + 1 + button;
                entries.get(listeningEntry).keyCode = mouseKeyCode;
                listeningEntry = -1;
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningEntry >= 0 && listeningEntry < entries.size()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listeningEntry = -1;
                return true;
            }

            entries.get(listeningEntry).keyCode = keyCode;
            listeningEntry = -1;
            return true;
        }

        for (KeybindEntry entry : entries) {
            if (entry.commandField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (KeybindEntry entry : entries) {
            if (entry.commandField.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        CommandKeybindManager.clearAll();
        for (KeybindEntry entry : entries) {
            if (entry.keyCode != GLFW.GLFW_KEY_UNKNOWN && !entry.commandField.getText().isEmpty()) {
                CommandKeybindManager.registerKeybind(entry.keyCode, entry.commandField.getText());
            }
        }

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
