package forfun.miningqol.client.shatter;

/**
 * Tokens. The menu ("take two") uses the flat set: surfaces, hairlines, two text greys and one accent that only
 * ever means "on". The named Tokyo Night colours below it are kept for in-world features (lock overlay, HUD).
 */
public final class Theme {
	private Theme() {}

	// ---- menu: monochrome — neutral greys, white is the only "on" colour ----
	public static final int BG = 0xFF131313;
	public static final int SURFACE = 0xFF191919;
	public static final int SURFACE_2 = 0xFF212121;
	public static final int SURFACE_3 = 0xFF2B2B2B;
	public static final int HAIR = 0x24FFFFFF;      // 14 %
	public static final int HAIR_2 = 0x3DFFFFFF;    // 24 %
	public static final int FG = 0xFFEDEDED;
	public static final int FG_2 = 0xFF9E9E9E;
	public static final int MUTED = 0xFF5C5C5C;
	public static final int ACCENT = 0xFFFFFFFF;

	// ---- Tokyo Night, for in-world drawing ----
	public static final int BG_DARK = 0xFF16161E;
	public static final int PANEL = 0xF01F2335;
	public static final int RAISED = 0xFF24283B;
	public static final int LINE = 0xFF2F334D;
	public static final int LINE_HI = 0xFF3B4261;
	public static final int FG_DIM = 0xFFA9B1D6;
	public static final int GHOST = 0xFF414868;
	public static final int BLUE = 0xFF7AA2F7;
	public static final int PURPLE = 0xFFBB9AF7;
	public static final int CYAN = 0xFF7DCFFF;
	public static final int GREEN = 0xFF9ECE6A;
	public static final int RED = 0xFFF7768E;
	public static final int YELLOW = 0xFFE0AF68;
	public static final int ORANGE = 0xFFFF9E64;

	public static final int R_XS = 2;
	public static final int R_SM = 3;
	public static final int R_MD = 4;
	public static final int R_LG = 6;

	// ---- see-through panels with outlines: the Click GUI module's Opacity / Outline settings ----

	/** A panel surface at the menu's opacity. */
	public static int panel(int surface) {
		return fade(surface, ShatterUi.opacity.get().floatValue());
	}

	public static int outline() {
		return ShatterUi.outline.get();
	}

	public static float outlineWidth() {
		return ShatterUi.outlineWidth.get().floatValue();
	}

	/** Replace the alpha channel of {@code color} with {@code alpha} (0..1). */
	public static int alpha(int color, float alpha) {
		int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
		return (a << 24) | (color & 0x00FFFFFF);
	}

	/** Multiply the alpha channel of {@code color} by {@code factor}. */
	public static int fade(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0x00FFFFFF);
	}

	/** Mix two ARGB colours. */
	public static int blend(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16) | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}
}
