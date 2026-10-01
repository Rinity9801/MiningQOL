package forfun.miningqol.client.shatter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** A set of independently toggleable options. */
public class MultiSetting extends Setting<Set<String>> {
    public final List<String> options;

    public MultiSetting(String name, String description, Set<String> defaultValue, String... options) {
        super(name, description, new LinkedHashSet<>(defaultValue));
        this.options = List.of(options);
    }

    public boolean has(String option) {
        return get().contains(option);
    }

    public void toggle(String option) {
        Set<String> current = new LinkedHashSet<>(get());
        if (!current.remove(option)) current.add(option);
        store(current);
    }
}
