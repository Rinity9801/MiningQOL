package forfun.miningqol.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import forfun.miningqol.client.utils.InputCompat;

import java.util.HashMap;
import java.util.Map;

public class CommandKeybindManager {
    private static final Map<Integer, String> keybindCommands = new HashMap<>();
    private static final Map<Integer, Boolean> keyStates = new HashMap<>();

    public static void registerKeybind(int keyCode, String command) {
        keybindCommands.put(keyCode, command);
        keyStates.put(keyCode, false);
    }

    public static void removeKeybind(int keyCode) {
        keybindCommands.remove(keyCode);
        keyStates.remove(keyCode);
    }

    public static void clearAll() {
        keybindCommands.clear();
        keyStates.clear();
    }

    public static Map<Integer, String> getAllKeybinds() {
        return new HashMap<>(keybindCommands);
    }

    public static void tick(Minecraft client) {
        if (client.player == null) return;

        // Don't process keybinds when a screen is open (GUI, chat, etc.)
        if (client.gui.screen() != null) return;

        for (Map.Entry<Integer, String> entry : keybindCommands.entrySet()) {
            int keyCode = entry.getKey();
            String command = entry.getValue();

            // 26.3: SDL scancodes, or InputCompat.MOUSE_BASE + button for mouse buttons.
            boolean isPressed = InputCompat.isDown(keyCode);

            boolean wasPressed = keyStates.getOrDefault(keyCode, false);

            if (isPressed && !wasPressed) {
                executeCommand(client, command);
            }

            keyStates.put(keyCode, isPressed);
        }
    }

    private static void executeCommand(Minecraft client, String command) {
        if (client.player == null) return;

        if (command.startsWith("/")) {
            client.player.connection.sendCommand(command.substring(1));
        } else {
            client.player.connection.sendChat(command);
        }
    }

    public static String getKeyName(int keyCode) {
        if (InputCompat.isMouseCode(keyCode)) {
            return "Mouse " + (InputCompat.mouseButtonOf(keyCode) + 1);
        }
        return InputCompat.keyName(keyCode);
    }
}
