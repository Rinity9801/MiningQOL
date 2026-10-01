package forfun.miningqol.client.shatter;

/** Free text, edited inline in the menu. */
public class StringSetting extends Setting<String> {
    public final String placeholder;

    public StringSetting(String name, String description, String defaultValue, String placeholder) {
        super(name, description, defaultValue);
        this.placeholder = placeholder;
    }
}
