package forfun.miningqol.client.shatter;

/** A one-shot button (add a row, print a preview, …); its name is the button's text. */
public class ActionSetting extends Setting<Boolean> {
    private final Runnable action;
    /** Tints the button red, for removals. */
    public final boolean danger;
    /** When set, the button text is read live (Start/Stop and the like) instead of [name]. */
    private java.util.function.Supplier<String> label;

    public ActionSetting(String name, String description, boolean danger, Runnable action) {
        super(name, description, false);
        this.action = action;
        this.danger = danger;
    }

    public ActionSetting label(java.util.function.Supplier<String> label) {
        this.label = label;
        return this;
    }

    public String label() {
        return label != null ? label.get() : name;
    }

    public void run() {
        action.run();
    }
}
