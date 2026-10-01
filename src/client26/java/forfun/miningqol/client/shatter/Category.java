package forfun.miningqol.client.shatter;

import java.util.ArrayList;
import java.util.List;

/**
 * A sidebar category, adapted from shattered's enum: the skins are driven by whatever
 * {@link Modules} registered, so categories are plain objects created at registration time.
 */
public final class Category {
    private static final List<Category> ALL = new ArrayList<>();

    public final String label;
    public final int ordinal;

    private Category(String label) {
        this.label = label;
        this.ordinal = ALL.size();
    }

    public static Category of(String label) {
        for (Category c : ALL) {
            if (c.label.equals(label)) return c;
        }
        Category c = new Category(label);
        ALL.add(c);
        return c;
    }

    public String id() {
        return label.toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    public static Category[] values() {
        return ALL.toArray(new Category[0]);
    }

    public static Category byId(String id) {
        for (Category c : ALL) {
            if (c.id().equals(id)) return c;
        }
        return null;
    }
}
