package forfun.miningqol.client.shatter;

import com.mojang.blaze3d.platform.InputConstants;

/** A GLFW key code, or {@link #NONE} when unbound. */
public class KeySetting extends Setting<Integer> {
    public static final int NONE = -1;

    /** Whether clicking the armed key box binds that mouse button (encoded past GLFW_KEY_LAST). */
    public boolean allowMouse;

    public KeySetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public KeySetting allowMouse() {
        allowMouse = true;
        return this;
    }

    public boolean isBound() {
        return get() != NONE;
    }

    public boolean matches(int keyCode) {
        return isBound() && get() == keyCode;
    }

    public String display() {
        if (!isBound()) return "none";
        if (get() > org.lwjgl.glfw.GLFW.GLFW_KEY_LAST) {
            int button = get() - org.lwjgl.glfw.GLFW.GLFW_KEY_LAST - 1;
            return switch (button) {
                case 0 -> "Mouse L";
                case 1 -> "Mouse R";
                case 2 -> "Mouse M";
                default -> "Mouse " + (button + 1);
            };
        }
        String s = InputConstants.Type.KEYSYM.getOrCreate(get()).getDisplayName().getString();
        return s.length() == 1 ? s.toUpperCase() : s;
    }
}
