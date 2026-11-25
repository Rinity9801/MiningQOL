package forfun.miningqol.client.utils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.sdl.SDLMouse;

/**
 * Input codes on Minecraft 26.3, which moved from GLFW to SDL3.
 * <ul>
 *   <li>Keys are SDL scancodes ({@code InputConstants.KEY_*}); GLFW is no longer shipped.</li>
 *   <li>Mouse buttons in events are SDL-numbered (1 left, 2 middle, 3 right). The mod's logic keeps
 *       the classic numbering — 0 left, 1 right, 2 middle, 3+ side buttons — via {@link #button}.</li>
 *   <li>A mod key code (command keybinds, {@code KeySetting}) is a scancode, or
 *       {@link #MOUSE_BASE} + classic button for a mouse button.</li>
 * </ul>
 * Configs written before 26.3 hold GLFW codes; {@link #fromLegacyGlfw} converts them.
 */
public final class InputCompat {
    private InputCompat() {}

    /** Mouse buttons are encoded from here up — above every SDL scancode (SDL_SCANCODE_COUNT = 512). */
    public static final int MOUSE_BASE = 0x1000;
    /** Unbound. */
    public static final int NONE = -1;

    /**
     * Turns SDL text input on or off for {@code owner}. SDL3 only delivers typed characters
     * ({@code charTyped}) while it's on; vanilla turns it on for a focused EditBox, so the mod's own
     * text fields have to do the same. Switching screens turns it off.
     */
    public static void setTextInput(Object owner, boolean on) {
        Minecraft.getInstance().textInputManager().onTextInputFocusChange(owner, on);
    }

    /** Classic button number (0 left, 1 right, 2 middle, 3+ side) of a 26.3 mouse event. */
    public static int button(MouseButtonEvent event) {
        return classicButton(event.button());
    }

    /** SDL button number (1 left, 2 middle, 3 right, 4+ side) -> classic numbering. */
    public static int classicButton(int sdlButton) {
        return switch (sdlButton) {
            case InputConstants.MOUSE_BUTTON_LEFT -> 0;
            case InputConstants.MOUSE_BUTTON_RIGHT -> 1;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> 2;
            default -> sdlButton - 1;
        };
    }

    /** Classic button number -> SDL button number. */
    public static int sdlButton(int classicButton) {
        return switch (classicButton) {
            case 0 -> InputConstants.MOUSE_BUTTON_LEFT;
            case 1 -> InputConstants.MOUSE_BUTTON_RIGHT;
            case 2 -> InputConstants.MOUSE_BUTTON_MIDDLE;
            default -> classicButton + 1;
        };
    }

    public static boolean isMouseCode(int code) {
        return code >= MOUSE_BASE;
    }

    public static int mouseCode(int classicButton) {
        return MOUSE_BASE + classicButton;
    }

    public static int mouseButtonOf(int code) {
        return code - MOUSE_BASE;
    }

    /** Whether a mouse button (classic numbering) is held right now. */
    public static boolean isMouseDown(int classicButton) {
        Minecraft mc = Minecraft.getInstance();
        return switch (classicButton) {
            case 0 -> mc.mouseHandler.isLeftPressed();
            case 1 -> mc.mouseHandler.isRightPressed();
            case 2 -> mc.mouseHandler.isMiddlePressed();
            default -> {
                int sdl = sdlButton(classicButton);
                int state = SDLMouse.SDL_GetMouseState(null, null);
                yield sdl >= 1 && sdl <= 32 && (state & (1 << (sdl - 1))) != 0;
            }
        };
    }

    /** Whether a mod key code (key or encoded mouse button) is held right now. */
    public static boolean isDown(int code) {
        if (code == NONE) return false;
        if (isMouseCode(code)) return isMouseDown(mouseButtonOf(code));
        return InputConstants.isKeyDown(code);
    }

    /** Whether a vanilla key (keyboard or SDL-numbered mouse) is held right now. */
    public static boolean isDown(InputConstants.Key key) {
        return switch (key.getType()) {
            case MOUSE -> isMouseDown(classicButton(key.getValue()));
            case KEYBOARD -> InputConstants.isKeyDown(key.getValue());
        };
    }

    /** Short display name: "A", "Space", "Mouse L"... */
    public static String displayName(int code) {
        if (code == NONE) return "none";
        if (isMouseCode(code)) {
            int button = mouseButtonOf(code);
            return switch (button) {
                case 0 -> "Mouse L";
                case 1 -> "Mouse R";
                case 2 -> "Mouse M";
                default -> "Mouse " + (button + 1);
            };
        }
        String s = InputConstants.Type.KEYBOARD.getOrCreate(code).getDisplayName().getString();
        return s.length() == 1 ? s.toUpperCase() : s;
    }

    /** Vanilla translation key of a mod key code ("key.keyboard.a", "key.mouse.left"...). */
    public static String keyName(int code) {
        if (isMouseCode(code)) {
            return InputConstants.Type.MOUSE.getOrCreate(sdlButton(mouseButtonOf(code))).getName();
        }
        return InputConstants.Type.KEYBOARD.getOrCreate(code).getName();
    }

    // ---- pre-26.3 configs

    /** GLFW_KEY_LAST: before 26.3 mouse buttons were stored as this + 1 + button. */
    private static final int LEGACY_GLFW_KEY_LAST = 348;

    /** A mod key code written by a pre-26.3 build (GLFW) -> this build's code; NONE if unmappable. */
    public static int fromLegacyGlfw(int code) {
        if (code < 0) return NONE;
        if (code > LEGACY_GLFW_KEY_LAST) return mouseCode(code - LEGACY_GLFW_KEY_LAST - 1);
        return switch (code) {
            // Generated from the KEY_* constants of 26.2's (GLFW) and 26.3's (SDL) InputConstants,
            // plus the keys 26.3 has no constant for (USB-HID scancodes).
            case 32 -> 44; // KEY_SPACE
            case 39 -> 52; // KEY_APOSTROPHE
            case 44 -> 54; // KEY_COMMA
            case 45 -> 45; // KEY_MINUS
            case 46 -> 55; // KEY_PERIOD
            case 47 -> 56; // KEY_SLASH
            case 48 -> 39; // KEY_0
            case 49 -> 30; // KEY_1
            case 50 -> 31; // KEY_2
            case 51 -> 32; // KEY_3
            case 52 -> 33; // KEY_4
            case 53 -> 34; // KEY_5
            case 54 -> 35; // KEY_6
            case 55 -> 36; // KEY_7
            case 56 -> 37; // KEY_8
            case 57 -> 38; // KEY_9
            case 59 -> 51; // KEY_SEMICOLON
            case 61 -> 46; // KEY_EQUALS
            case 65 -> 4; // KEY_A
            case 66 -> 5; // KEY_B
            case 67 -> 6; // KEY_C
            case 68 -> 7; // KEY_D
            case 69 -> 8; // KEY_E
            case 70 -> 9; // KEY_F
            case 71 -> 10; // KEY_G
            case 72 -> 11; // KEY_H
            case 73 -> 12; // KEY_I
            case 74 -> 13; // KEY_J
            case 75 -> 14; // KEY_K
            case 76 -> 15; // KEY_L
            case 77 -> 16; // KEY_M
            case 78 -> 17; // KEY_N
            case 79 -> 18; // KEY_O
            case 80 -> 19; // KEY_P
            case 81 -> 20; // KEY_Q
            case 82 -> 21; // KEY_R
            case 83 -> 22; // KEY_S
            case 84 -> 23; // KEY_T
            case 85 -> 24; // KEY_U
            case 86 -> 25; // KEY_V
            case 87 -> 26; // KEY_W
            case 88 -> 27; // KEY_X
            case 89 -> 28; // KEY_Y
            case 90 -> 29; // KEY_Z
            case 91 -> 47; // KEY_LBRACKET
            case 92 -> 49; // KEY_BACKSLASH
            case 93 -> 48; // KEY_RBRACKET
            case 96 -> 53; // KEY_GRAVE
            case 161 -> 100; // WORLD_1->NONUSBACKSLASH
            case 256 -> 41; // KEY_ESCAPE
            case 257 -> 40; // KEY_RETURN
            case 258 -> 43; // KEY_TAB
            case 259 -> 42; // KEY_BACKSPACE
            case 260 -> 73; // KEY_INSERT
            case 261 -> 76; // KEY_DELETE
            case 262 -> 79; // KEY_RIGHT
            case 263 -> 80; // KEY_LEFT
            case 264 -> 81; // KEY_DOWN
            case 265 -> 82; // KEY_UP
            case 266 -> 75; // KEY_PAGEUP
            case 267 -> 78; // KEY_PAGEDOWN
            case 268 -> 74; // KEY_HOME
            case 269 -> 77; // KEY_END
            case 280 -> 57; // KEY_CAPSLOCK
            case 281 -> 71; // KEY_SCROLLLOCK
            case 282 -> 83; // KEY_NUMLOCK
            case 283 -> 70; // KEY_PRINTSCREEN
            case 284 -> 72; // KEY_PAUSE
            case 290 -> 58; // KEY_F1
            case 291 -> 59; // KEY_F2
            case 292 -> 60; // KEY_F3
            case 293 -> 61; // KEY_F4
            case 294 -> 62; // KEY_F5
            case 295 -> 63; // KEY_F6
            case 296 -> 64; // KEY_F7
            case 297 -> 65; // KEY_F8
            case 298 -> 66; // KEY_F9
            case 299 -> 67; // KEY_F10
            case 300 -> 68; // KEY_F11
            case 301 -> 69; // KEY_F12
            case 302 -> 104; // KEY_F13
            case 303 -> 105; // KEY_F14
            case 304 -> 106; // KEY_F15
            case 305 -> 107; // KEY_F16
            case 306 -> 108; // KEY_F17
            case 307 -> 109; // KEY_F18
            case 308 -> 110; // KEY_F19
            case 309 -> 111; // KEY_F20
            case 310 -> 112; // KEY_F21
            case 311 -> 113; // KEY_F22
            case 312 -> 114; // KEY_F23
            case 313 -> 115; // KEY_F24
            case 320 -> 98; // KEY_NUMPAD0
            case 321 -> 89; // KEY_NUMPAD1
            case 322 -> 90; // KEY_NUMPAD2
            case 323 -> 91; // KEY_NUMPAD3
            case 324 -> 92; // KEY_NUMPAD4
            case 325 -> 93; // KEY_NUMPAD5
            case 326 -> 94; // KEY_NUMPAD6
            case 327 -> 95; // KEY_NUMPAD7
            case 328 -> 96; // KEY_NUMPAD8
            case 329 -> 97; // KEY_NUMPAD9
            case 330 -> 99; // KP_DECIMAL->KP_PERIOD
            case 331 -> 84; // KP_DIVIDE
            case 332 -> 85; // KEY_MULTIPLY
            case 333 -> 86; // KP_SUBTRACT
            case 334 -> 87; // KEY_ADD
            case 335 -> 88; // KEY_NUMPADENTER
            case 336 -> 103; // KEY_NUMPADEQUALS
            case 340 -> 225; // KEY_LSHIFT
            case 341 -> 224; // KEY_LCONTROL
            case 342 -> 226; // KEY_LALT
            case 343 -> 227; // KEY_LSUPER->LGUI
            case 344 -> 229; // KEY_RSHIFT
            case 345 -> 228; // KEY_RCONTROL
            case 346 -> 230; // KEY_RALT
            case 347 -> 231; // KEY_RSUPER->RGUI
            case 348 -> 101; // MENU->APPLICATION
            default -> NONE;
        };
    }
}
