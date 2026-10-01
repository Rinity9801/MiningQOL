package forfun.miningqol.client.shatter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base setting, adapted from shattered. A setting either owns its value or — the normal case
 * here — delegates to a live getter/setter pair via {@link #bind}, so the skins edit exactly
 * the state the classic GUI edits and nothing needs separate persistence.
 */
public abstract class Setting<T> {
    public final String name;
    public final String description;
    protected T value;
    protected Supplier<T> getter;
    protected Consumer<T> setter;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S bind(Supplier<T> getter, Consumer<T> setter) {
        this.getter = getter;
        this.setter = setter;
        return (S) this;
    }

    public T get() {
        return getter != null ? getter.get() : value;
    }

    public void set(T v) {
        store(v);
    }

    /** Writes through to the delegate (and mirrors locally as the fallback). */
    protected void store(T v) {
        value = v;
        if (setter != null) setter.accept(v);
    }
}
