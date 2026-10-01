package forfun.miningqol.client.shatter;

import net.minecraft.client.Minecraft;

/**
 * State for the dropdown menu itself (shattered's ClickGuiModule, flattened): its scale,
 * animations, descriptions, and panel opacity/outline. Persisted through MiningConfig.
 */
public final class ShatterUi {
    private ShatterUi() {}

    public static float scale = 1.0f;
    private static boolean animationsEnabled = true;
    private static boolean descriptionsEnabled = true;

    public static final SliderSetting opacity = new SliderSetting("Opacity",
        "How solid the panels are — lower to see the game through them", 0.65, 0.2, 1.0, 0.05, "");
    public static final ColorSetting outline = new ColorSetting("Outline", "Colour of the panel outlines", 0x99FFFFFF);
    public static final SliderSetting outlineWidth =
        new SliderSetting("Outline Width", "Thickness of the panel outlines", 1.0, 0.5, 3.0, 0.5, " px");

    public static boolean animations() {
        return animationsEnabled;
    }

    public static void setAnimations(boolean value) {
        animationsEnabled = value;
    }

    public static boolean descriptions() {
        return descriptionsEnabled;
    }

    public static void setDescriptions(boolean value) {
        descriptionsEnabled = value;
    }

    public static void setScale(float value) {
        scale = Math.max(0.75f, Math.min(2.0f, value));
    }

    public static String version() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("miningqol")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
    }

    /**
     * Logical pixels per UI pixel — shattered's formula: layout is designed at browser sizes,
     * with a floor so NanoVG text never falls below ~15 px.
     */
    public static float uiScale() {
        Minecraft mc = Minecraft.getInstance();
        float logicalH = mc.getWindow().getScreenHeight();
        float auto = Math.max(1.3f, logicalH / 830f);
        return Math.round(scale * auto * 20f) / 20f;
    }

    /** Opens the settings menu. */
    public static void openConfiguredGui() {
        Minecraft.getInstance().setScreen(new ShatterScreen());
    }
}
