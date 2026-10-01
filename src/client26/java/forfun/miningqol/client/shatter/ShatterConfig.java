package forfun.miningqol.client.shatter;

import forfun.miningqol.client.MiningqolClient;
import forfun.miningqol.client.config.MiningConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistence shim for the skinned menu, replacing shattered's Config. Panel positions live in
 * {@link MiningConfig#shatterPanels}; every other value the skins touch already delegates into
 * live feature state, so "dirty" just means "save the mod config when the menu closes".
 */
public final class ShatterConfig {
    private ShatterConfig() {}

    public record Panel(int x, int y, boolean open) {}

    private static final Map<String, Panel> panels = new HashMap<>();
    private static boolean dirty;

    public static Panel panel(Category c) {
        return panels.get(c.id());
    }

    public static void setPanel(Category c, Panel p) {
        panels.put(c.id(), p);
        markDirty();
    }

    public static void markDirty() {
        dirty = true;
    }

    /** Menu closed: write everything through the normal config cycle. */
    public static void flush() {
        if (!dirty) return;
        dirty = false;
        MiningConfig config = MiningqolClient.getConfig();
        if (config != null) {
            config.loadFromGame();
            config.save();
        }
    }

    /** Called from MiningConfig.applyToGame with the persisted panel map. */
    public static void load(Map<String, int[]> saved) {
        panels.clear();
        if (saved == null) return;
        saved.forEach((id, v) -> {
            if (v != null && v.length >= 3) panels.put(id, new Panel(v[0], v[1], v[2] != 0));
        });
    }

    /** Called from MiningConfig.loadFromGame; returns the map to persist. */
    public static Map<String, int[]> store() {
        Map<String, int[]> out = new HashMap<>();
        panels.forEach((id, p) -> out.put(id, new int[] {p.x(), p.y(), p.open() ? 1 : 0}));
        return out;
    }
}
