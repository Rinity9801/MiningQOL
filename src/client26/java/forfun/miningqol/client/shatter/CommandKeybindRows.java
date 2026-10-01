package forfun.miningqol.client.shatter;

import forfun.miningqol.client.CommandKeybindManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Command Keybinds editor: per bind a key box (keys or mouse buttons), a command field and a
 * remove button, then an add button. Rows live here while the menu is open so a freshly added,
 * still unbound row survives; every edit rewrites the manager.
 */
final class CommandKeybindRows {
    private CommandKeybindRows() {}

    private static final class Row {
        int key;
        String command;

        Row(int key, String command) {
            this.key = key;
            this.command = command;
        }
    }

    private static List<Row> rows;
    private static List<Setting<?>> settings;

    /** Forget the working rows; the next read reloads them from the manager. */
    static void reload() {
        rows = null;
        settings = null;
    }

    static List<Setting<?>> settings() {
        if (rows == null) {
            rows = new ArrayList<>();
            for (Map.Entry<Integer, String> e : CommandKeybindManager.getAllKeybinds().entrySet()) {
                rows.add(new Row(e.getKey(), e.getValue()));
            }
            rows.sort((a, b) -> a.command.compareToIgnoreCase(b.command));
        }
        if (settings == null) settings = build();
        return settings;
    }

    private static List<Setting<?>> build() {
        List<Setting<?>> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int n = i + 1;
            out.add(new KeySetting("Bind " + n + " key", "Click, then press a key — or click the box again for a mouse button", KeySetting.NONE)
                .allowMouse()
                .bind(() -> row.key, code -> {
                    row.key = code;
                    save();
                }));
            out.add(new StringSetting("Bind " + n + " command", "Sent when the key is pressed", "", "e.g. /warp forge")
                .bind(() -> row.command, text -> {
                    row.command = text;
                    save();
                }));
            out.add(new ActionSetting("Remove bind " + n, "", true, () -> {
                rows.remove(row);
                restructure();
            }));
        }
        out.add(new ActionSetting("+ Add keybind", "", false, () -> {
            rows.add(new Row(KeySetting.NONE, ""));
            restructure();
        }));
        return out;
    }

    /** Rows were added or removed: renumber, and drop any capture pointing at a stale setting. */
    private static void restructure() {
        SettingWidgets.listen(null);
        SettingWidgets.stopEditing();
        settings = null;
        save();
    }

    private static void save() {
        CommandKeybindManager.clearAll();
        for (Row row : rows) {
            if (row.key != KeySetting.NONE && !row.command.isBlank()) {
                CommandKeybindManager.registerKeybind(row.key, row.command.trim());
            }
        }
        ShatterConfig.markDirty();
    }
}
