package forfun.miningqol.client.shatter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * The settings menu (shattered's ClickGuiScreen): the dropdown skin, drawn through NanoVG in UI
 * pixels. Input goes to the colour picker, a text field being edited or a key box listening
 * first, and to the skin otherwise.
 */
public class ShatterScreen extends Screen {
    private final Skin skin = new DropdownSkin();

    public ShatterScreen() {
        super(Component.literal("MiningQOL"));
        ShatterModules.ensureRegistered();
        CommandKeybindRows.reload();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Skin skin() {
        return skin;
    }

    private float uiScale() {
        return ShatterUi.uiScale();
    }

    /** Mouse position in UI pixels. */
    private float mouseX() {
        return (float) (Minecraft.getInstance().mouseHandler.xpos() / uiScale());
    }

    private float mouseY() {
        return (float) (Minecraft.getInstance().mouseHandler.ypos() / uiScale());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        Skin s = skin();
        NVGPipRenderer.drawFullScreen(g, () -> {
            Minecraft mc = Minecraft.getInstance();
            float scale = uiScale();
            float w = mc.getWindow().getScreenWidth() / scale, h = mc.getWindow().getScreenHeight() / scale;
            NVG.push();
            NVG.scale(scale, scale);
            s.draw(mouseX(), mouseY(), w, h);
            if (ColorPicker.isOpen()) ColorPicker.draw(w, h, mouseX(), mouseY());
            NVG.pop();
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (ColorPicker.isOpen()) return ColorPicker.mouseClicked(mouseX(), mouseY(), e.button());
        if (SettingWidgets.editing() != null) SettingWidgets.stopEditing();
        if (skin().mouseClicked(mouseX(), mouseY(), e.button())) return true;
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (ColorPicker.isOpen()) {
            ColorPicker.mouseDragged(mouseX(), mouseY());
            return true;
        }
        skin().mouseDragged(mouseX(), mouseY());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (ColorPicker.isOpen()) {
            ColorPicker.mouseReleased();
            return true;
        }
        skin().mouseReleased(mouseX(), mouseY(), e.button());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (skin().mouseScrolled(mouseX(), mouseY(), dy)) return true;
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (ColorPicker.isOpen()) return ColorPicker.charTyped((char) e.codepoint());
        if (SettingWidgets.editing() != null) {
            SettingWidgets.typeInto((char) e.codepoint());
            return true;
        }
        if (skin().charTyped((char) e.codepoint())) return true;
        return super.charTyped(e);
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (ColorPicker.isOpen() && ColorPicker.keyPressed(e)) return true;
        if (SettingWidgets.editing() != null) {
            if (TextField.isShortcut(e) && SettingWidgets.editShortcut(e.key())) {
                ShatterConfig.markDirty();
            } else if (e.isEscape() || e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
                    || e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                SettingWidgets.stopEditing();
                ShatterConfig.markDirty();
            } else if (e.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                SettingWidgets.backspace();
                ShatterConfig.markDirty();
            } else {
                SettingWidgets.editKey(e.key());
            }
            return true;
        }
        KeySetting listening = SettingWidgets.listening();
        if (listening != null) {
            listening.set(e.isEscape() ? KeySetting.NONE : e.key());
            SettingWidgets.listen(null);
            ShatterConfig.markDirty();
            return true;
        }
        if (skin().keyPressed(e)) return true;
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        ColorPicker.close();
        if (skin != null) skin.closed();
        SettingWidgets.listen(null);
        SettingWidgets.stopEditing();
        ShatterConfig.flush();
        super.onClose();
    }
}
