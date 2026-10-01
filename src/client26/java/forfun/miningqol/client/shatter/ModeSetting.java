package forfun.miningqol.client.shatter;

import java.util.List;

public class ModeSetting extends Setting<String> {
    public final List<String> options;

    public ModeSetting(String name, String description, String defaultValue, String... options) {
        super(name, description, defaultValue);
        this.options = List.of(options);
    }

    public ModeSetting(String name, String description, String defaultValue, List<String> options) {
        super(name, description, defaultValue);
        this.options = List.copyOf(options);
    }

    public boolean is(String option) {
        return get().equals(option);
    }
}
