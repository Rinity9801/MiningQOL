package forfun.miningqol.client.shatter;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** An ARGB colour, edited with the colour picker. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultArgb) {
        super(name, description, defaultArgb);
    }

    /** Bridges our float[]{r,g,b} + separate alpha accessors into one ARGB setting. */
    public static ColorSetting ofRgb(String name, String description,
                                     Supplier<float[]> getRgb, RgbConsumer setRgb,
                                     Supplier<Float> getAlpha, Consumer<Float> setAlpha) {
        ColorSetting s = new ColorSetting(name, description, 0xFFFFFFFF);
        s.bind(
            () -> {
                float[] c = getRgb.get();
                int a = Math.round(Math.max(0f, Math.min(1f, getAlpha == null ? 1f : getAlpha.get())) * 255f);
                return (a << 24) | (Math.round(c[0] * 255f) << 16) | (Math.round(c[1] * 255f) << 8) | Math.round(c[2] * 255f);
            },
            argb -> {
                setRgb.accept(((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f);
                if (setAlpha != null) setAlpha.accept((argb >>> 24) / 255f);
            });
        return s;
    }

    /** RGB-only variant: alpha shows as opaque and edits to it are dropped. */
    public static ColorSetting ofRgb(String name, String description,
                                     Supplier<float[]> getRgb, RgbConsumer setRgb) {
        return ofRgb(name, description, getRgb, setRgb, null, null);
    }

    public interface RgbConsumer {
        void accept(float red, float green, float blue);
    }

    public static String toHex(int argb) {
        return String.format("#%02X%02X%02X%02X", (argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24);
    }

    /** Parses #RRGGBB or #RRGGBBAA (leading # optional). Returns null when malformed. */
    public static Integer parseHex(String text) {
        String hex = text.trim().replace("#", "");
        try {
            if (hex.length() == 6) return 0xFF000000 | Integer.parseInt(hex, 16);
            if (hex.length() == 8) {
                long v = Long.parseLong(hex, 16);
                int rgb = (int) (v >>> 8) & 0xFFFFFF;
                int a = (int) (v & 0xFF);
                return (a << 24) | rgb;
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
