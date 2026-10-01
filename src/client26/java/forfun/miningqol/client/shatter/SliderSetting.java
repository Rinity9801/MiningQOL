package forfun.miningqol.client.shatter;

import java.util.Locale;

public class SliderSetting extends Setting<Double> {
    public final double min;
    public final double max;
    public final double step;
    public final String unit;

    public SliderSetting(String name, String description, double defaultValue, double min, double max, double step, String unit) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    @Override
    public void set(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        double stepped = Math.round((clamped - min) / step) * step + min;
        store(Math.max(min, Math.min(max, stepped)));
    }

    /** 0..1 position of the current value along the track. */
    public double fraction() {
        return max == min ? 0 : (get() - min) / (max - min);
    }

    public void setFraction(double f) {
        set(min + Math.max(0, Math.min(1, f)) * (max - min));
    }

    public int intValue() {
        return (int) Math.round(get());
    }

    public String display() {
        int decimals = 0;
        String s = Double.toString(step);
        if (!s.endsWith(".0")) decimals = s.length() - s.indexOf('.') - 1;
        return String.format(Locale.ROOT, "%." + decimals + "f", get()) + unit;
    }
}
