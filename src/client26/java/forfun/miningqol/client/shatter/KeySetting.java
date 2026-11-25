package forfun.miningqol.client.shatter;

import com.mojang.blaze3d.platform.InputConstants;

/** A key code (an SDL scancode on 26.3, see {@link forfun.miningqol.client.utils.InputCompat}), or {@link #NONE} when unbound. */
public class KeySetting extends Setting<Integer> {
    public static final int NONE = -1;

    /** Whether clicking the armed key box binds that mouse button (encoded as InputCompat.MOUSE_BASE + button). */
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
        return forfun.miningqol.client.utils.InputCompat.displayName(get());
    }
}
