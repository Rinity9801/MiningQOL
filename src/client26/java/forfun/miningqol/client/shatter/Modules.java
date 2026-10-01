package forfun.miningqol.client.shatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Modules {
    private Modules() {}

    private static final List<Module> ALL = new ArrayList<>();

    public static <M extends Module> M register(M module) {
        ALL.add(module);
        return module;
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<Module> in(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == category) out.add(m);
        return out;
    }

    public static void clear() {
        ALL.clear();
    }
}
