package forfun.miningqol.client.shatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * One feature in the skinned menu. Unlike shattered's modules, these do not own their state:
 * the toggle and every setting delegate to the same getters/setters the classic Vexel GUI uses,
 * so both GUIs edit one truth and the normal config save cycle persists it.
 */
public class Module {
    public final String id;
    public final String name;
    public final String description;
    public final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private java.util.function.Supplier<List<Setting<?>>> dynamicSettings;
    /** Kept for skin layout parity; our features bind keys elsewhere, so this stays unbound. */
    public final KeySetting bind = new KeySetting("Keybind", "Unused", KeySetting.NONE);
    /** ClickGUI state: settings unfolded (dropdown skin). Not persisted. */
    public boolean expanded;

    private final BooleanSupplier enabledGet;
    private final Consumer<Boolean> enabledSet;
    /** When set, "toggling" runs this instead (e.g. opens an editor screen). */
    private final Runnable openAction;

    public Module(String name, String description, Category category,
                  BooleanSupplier enabledGet, Consumer<Boolean> enabledSet) {
        this(name, description, category, enabledGet, enabledSet, null);
    }

    public Module(String name, String description, Category category, Runnable openAction) {
        this(name, description, category, null, null, openAction);
    }

    private Module(String name, String description, Category category,
                   BooleanSupplier enabledGet, Consumer<Boolean> enabledSet, Runnable openAction) {
        this.id = name.toLowerCase().replaceAll("[^a-z0-9]+", "_");
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabledGet = enabledGet;
        this.enabledSet = enabledSet;
        this.openAction = openAction;
    }

    public Module add(Setting<?> setting) {
        settings.add(setting);
        return this;
    }

    /** Settings appended after the fixed ones, rebuilt on every read — for editors whose rows come and go. */
    public Module dynamicSettings(java.util.function.Supplier<List<Setting<?>>> source) {
        this.dynamicSettings = source;
        return this;
    }

    public List<Setting<?>> settings() {
        if (dynamicSettings == null) return Collections.unmodifiableList(settings);
        List<Setting<?>> all = new ArrayList<>(settings);
        all.addAll(dynamicSettings.get());
        return Collections.unmodifiableList(all);
    }

    public boolean isEnabled() {
        return enabledGet != null && enabledGet.getAsBoolean();
    }

    public void setEnabled(boolean value) {
        if (enabledSet != null) enabledSet.accept(value);
    }

    public void toggle() {
        if (openAction != null) {
            openAction.run();
            return;
        }
        setEnabled(!isEnabled());
    }

    public boolean opensScreen() {
        return openAction != null;
    }

    /** Whether this module has a real on/off state (settings-only groups don't). */
    public boolean hasToggle() {
        return enabledGet != null && enabledSet != null;
    }
}
