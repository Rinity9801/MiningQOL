package forfun.miningqol.client;

/**
 * Custom fog: your own fog distance and colour in place of the normal fog, while the camera is in
 * air (water, lava and powder snow keep theirs). Optionally the sky — dome, sun, moon and stars —
 * gives way to the fog colour too. Applied by FogRendererMixin and LevelRendererSkyMixin; ported
 * from definitely-legit.
 */
public final class CustomFog {
    private CustomFog() {}

    private static boolean enabled = false;
    private static float start = 8.0f;
    private static float end = 48.0f;
    private static final float[] color = {170f / 255f, 190f / 255f, 220f / 255f};
    /** The shader blends by the colour's alpha, so this is how strong the fog is at its end. */
    private static float alpha = 1.0f;
    private static boolean fogSky = true;

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static float getStart() { return start; }
    public static void setStart(float value) { start = Math.max(0f, Math.min(256f, value)); }
    public static float getEnd() { return end; }
    public static void setEnd(float value) { end = Math.max(1f, Math.min(512f, value)); }
    public static float[] getColor() { return color.clone(); }
    public static void setColor(float r, float g, float b) { color[0] = r; color[1] = g; color[2] = b; }
    public static float getAlpha() { return alpha; }
    public static void setAlpha(float value) { alpha = Math.max(0f, Math.min(1f, value)); }
    public static boolean isFogSky() { return fogSky; }
    public static void setFogSky(boolean value) { fogSky = value; }
}
