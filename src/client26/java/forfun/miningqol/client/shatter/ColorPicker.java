package forfun.miningqol.client.shatter;

import net.minecraft.client.input.KeyEvent;

/**
 * The colour picker: saturation/value square, hue and alpha bars, before/after swatches, a hex field and
 * R/G/B/A fields — all matching the flat theme. The hex and channel fields take Cmd/Ctrl+A/C/X/V.
 */
public final class ColorPicker {
	private ColorPicker() {}

	private static final float W = 430f, H = 300f, PAD = 20f;
	private static final float SV = 170f, BAR_W = 16f, BAR_GAP = 12f;

	private static ColorSetting setting;
	private static int before;
	private static float hue, sat, val;
	private static int alpha;
	private static float x0, y0;
	private static int dragging; // 0 none, 1 sv, 2 hue, 3 alpha

	private static final TextField hexField = new TextField(ColorPicker::hexTyped);
	private static final TextField[] rgba = new TextField[4];

	static {
		for (int i = 0; i < 4; i++) {
			final int channel = i;
			rgba[i] = new TextField(text -> channelTyped(channel, text));
		}
	}

	public static boolean isOpen() {
		return setting != null;
	}

	public static void open(ColorSetting s) {
		setting = s;
		before = s.get();
		fromArgb(s.get());
		syncFields();
	}

	public static void close() {
		if (setting != null) ShatterConfig.markDirty();
		setting = null;
		hexField.blur();
		for (TextField f : rgba) f.blur();
		dragging = 0;
	}

	// ---- colour math ----

	private static void fromArgb(int argb) {
		alpha = argb >>> 24;
		float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
		val = max;
		sat = max == 0 ? 0 : d / max;
		if (d == 0) hue = 0;
		else if (max == r) hue = ((g - b) / d % 6f + 6f) % 6f / 6f;
		else if (max == g) hue = ((b - r) / d + 2f) / 6f;
		else hue = ((r - g) / d + 4f) / 6f;
	}

	private static int currentArgb() {
		int rgb = hsvToRgb(hue, sat, val);
		return (alpha << 24) | rgb;
	}

	public static int hsvToRgb(float h, float s, float v) {
		float r, g, b;
		int i = (int) Math.floor(h * 6f) % 6;
		float f = h * 6f - (int) Math.floor(h * 6f);
		float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
		switch (i < 0 ? i + 6 : i) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}

	private static void apply() {
		if (setting != null) setting.set(currentArgb());
	}

	private static void syncFields() {
		int c = currentArgb();
		hexField.setText(ColorSetting.toHex(c));
		rgba[0].setText(String.valueOf((c >> 16) & 255));
		rgba[1].setText(String.valueOf((c >> 8) & 255));
		rgba[2].setText(String.valueOf(c & 255));
		rgba[3].setText(String.valueOf(c >>> 24));
	}

	private static void hexTyped(String text) {
		Integer parsed = ColorSetting.parseHex(text);
		if (parsed != null) {
			fromArgb(parsed);
			apply();
			int c = currentArgb();
			rgba[0].setText(String.valueOf((c >> 16) & 255));
			rgba[1].setText(String.valueOf((c >> 8) & 255));
			rgba[2].setText(String.valueOf(c & 255));
			rgba[3].setText(String.valueOf(c >>> 24));
		}
	}

	private static void channelTyped(int channel, String text) {
		int v;
		try {
			v = Math.max(0, Math.min(255, Integer.parseInt(text.trim())));
		} catch (NumberFormatException e) {
			return;
		}
		int c = currentArgb();
		switch (channel) {
			case 0 -> c = (c & 0xFF00FFFF) | (v << 16);
			case 1 -> c = (c & 0xFFFF00FF) | (v << 8);
			case 2 -> c = (c & 0xFFFFFF00) | v;
			case 3 -> c = (c & 0x00FFFFFF) | (v << 24);
		}
		fromArgb(c);
		apply();
		hexField.setText(ColorSetting.toHex(c));
	}

	// ---- drawing ----

	public static void draw(float screenW, float screenH, float mx, float my) {
		if (setting == null) return;
		x0 = (screenW - W) / 2f;
		y0 = (screenH - H) / 2f;
		NVG.rect(0, 0, screenW, screenH, Theme.alpha(Theme.BG, 0.55f));
		float ow = Theme.outlineWidth();
		NVG.roundedRect(x0, y0, W, H, 10f, Theme.panel(Theme.SURFACE));
		NVG.hollowRoundedRect(x0 + ow / 2f, y0 + ow / 2f, W - ow, H - ow, 10f, ow, Theme.outline());
		NVG.text(setting.name, x0 + PAD, y0 + 16f, 15f, Theme.FG, NVG.Font.MEDIUM);
		boolean closeHover = in(mx, my, x0 + W - 38f, y0 + 10f, 28f, 28f);
		NVG.textCentered("×", x0 + W - 24f, y0 + 24f, 15f, closeHover ? Theme.FG : Theme.MUTED, NVG.Font.SANS);

		float top = y0 + 46f;
		// SV square: hue base + white→transparent horizontal + transparent→black vertical
		float svx = x0 + PAD, svy = top;
		int hueRgb = 0xFF000000 | hsvToRgb(hue, 1f, 1f);
		NVG.roundedRect(svx, svy, SV, SV, 6f, hueRgb);
		NVG.gradientRect(svx, svy, SV, SV, 6f, 0xFFFFFFFF, 0x00FFFFFF, false);
		NVG.gradientRect(svx, svy, SV, SV, 6f, 0x00000000, 0xFF000000, true);
		float cx = svx + sat * SV, cy = svy + (1f - val) * SV;
		NVG.ring(cx, cy, 6f, 1.5f, 0xFF000000);
		NVG.ring(cx, cy, 5f, 1.5f, 0xFFFFFFFF);

		// hue bar
		float hx = svx + SV + BAR_GAP;
		for (int i = 0; i < 6; i++) {
			int c1 = 0xFF000000 | hsvToRgb(i / 6f, 1f, 1f);
			int c2 = 0xFF000000 | hsvToRgb((i + 1) / 6f, 1f, 1f);
			NVG.gradientRect(hx, svy + SV * i / 6f, BAR_W, SV / 6f + 0.5f, 0f, c1, c2, true);
		}
		NVG.hollowRoundedRect(hx, svy, BAR_W, SV, 2f, 1f, Theme.HAIR);
		NVG.rect(hx - 2f, svy + hue * SV - 1.5f, BAR_W + 4f, 3f, 0xFFFFFFFF);

		// alpha bar: checkerboard + colour fade
		float ax = hx + BAR_W + BAR_GAP;
		for (int yy = 0; yy < (int) (SV / 6f); yy++) {
			for (int xx = 0; xx < 3; xx++) {
				if ((xx + yy) % 2 == 0) NVG.rect(ax + xx * (BAR_W / 3f), svy + yy * 6f, BAR_W / 3f, Math.min(6f, SV - yy * 6f), 0xFF3A3A3A);
				else NVG.rect(ax + xx * (BAR_W / 3f), svy + yy * 6f, BAR_W / 3f, Math.min(6f, SV - yy * 6f), 0xFF232323);
			}
		}
		int opaque = 0xFF000000 | hsvToRgb(hue, sat, val);
		NVG.gradientRect(ax, svy, BAR_W, SV, 0f, opaque, opaque & 0x00FFFFFF, true);
		NVG.hollowRoundedRect(ax, svy, BAR_W, SV, 2f, 1f, Theme.HAIR);
		NVG.rect(ax - 2f, svy + (1f - alpha / 255f) * SV - 1.5f, BAR_W + 4f, 3f, 0xFFFFFFFF);

		// right column
		float rx = ax + BAR_W + PAD, rw = x0 + W - PAD - rx;
		NVG.text("BEFORE", rx, top - 2f, 9.5f, Theme.MUTED, NVG.Font.MEDIUM);
		NVG.textRight("AFTER", rx + rw, top - 2f, 9.5f, Theme.MUTED, NVG.Font.MEDIUM);
		float sw = (rw - 6f) / 2f;
		swatch(rx, top + 12f, sw, 26f, before);
		swatch(rx + sw + 6f, top + 12f, sw, 26f, currentArgb());
		hexField.draw(rx, top + 48f, rw, 24f, 12f);
		String[] labels = {"R", "G", "B", "A"};
		for (int i = 0; i < 4; i++) {
			float fy = top + 80f + i * 30f;
			NVG.text(labels[i], rx, fy + 5f, 11.5f, Theme.FG_2, NVG.Font.SANS);
			rgba[i].draw(rx + 18f, fy, rw - 18f, 24f, 12f);
		}

		// buttons
		boolean resetHover = in(mx, my, x0 + PAD, y0 + H - 40f, 76f, 26f);
		NVG.hollowRoundedRect(x0 + PAD + 0.5f, y0 + H - 40f + 0.5f, 75f, 25f, 6f, 1f, resetHover ? Theme.FG_2 : Theme.HAIR_2);
		NVG.textCentered("Reset", x0 + PAD + 38f, y0 + H - 27f, 12f, resetHover ? Theme.FG : Theme.FG_2, NVG.Font.SANS);
		boolean doneHover = in(mx, my, x0 + W - PAD - 76f, y0 + H - 40f, 76f, 26f);
		NVG.roundedRect(x0 + W - PAD - 76f, y0 + H - 40f, 76f, 26f, 6f, doneHover ? Theme.SURFACE_3 : Theme.SURFACE_2);
		NVG.textCentered("Done", x0 + W - PAD - 38f, y0 + H - 27f, 12f, Theme.FG, NVG.Font.MEDIUM);
	}

	private static void swatch(float x, float y, float w, float h, int argb) {
		for (int xx = 0; xx < (int) (w / 6f) + 1; xx++) {
			for (int yy = 0; yy < (int) (h / 6f) + 1; yy++) {
				int c = (xx + yy) % 2 == 0 ? 0xFF3A3A3A : 0xFF232323;
				NVG.rect(x + xx * 6f, y + yy * 6f, Math.min(6f, w - xx * 6f), Math.min(6f, h - yy * 6f), c);
			}
		}
		NVG.roundedRect(x, y, w, h, 5f, argb);
		NVG.hollowRoundedRect(x + 0.5f, y + 0.5f, w - 1f, h - 1f, 5f, 1f, Theme.HAIR_2);
	}

	// ---- input (UI px, routed by ClickGuiScreen while open) ----

	public static boolean mouseClicked(float mx, float my, int button) {
		if (setting == null) return false;
		hexField.blur();
		for (TextField f : rgba) f.blur();
		float top = y0 + 46f, svx = x0 + PAD, svy = top;
		float hx = svx + SV + BAR_GAP, ax = hx + BAR_W + BAR_GAP;
		float rx = ax + BAR_W + PAD, rw = x0 + W - PAD - rx;
		if (in(mx, my, x0 + W - 38f, y0 + 10f, 28f, 28f)) { close(); return true; }
		if (in(mx, my, x0 + PAD, y0 + H - 40f, 76f, 26f)) { fromArgb(before); apply(); syncFields(); return true; }
		if (in(mx, my, x0 + W - PAD - 76f, y0 + H - 40f, 76f, 26f)) { close(); return true; }
		if (in(mx, my, svx - 4f, svy - 4f, SV + 8f, SV + 8f)) { dragging = 1; mouseDragged(mx, my); return true; }
		if (in(mx, my, hx - 4f, svy - 4f, BAR_W + 8f, SV + 8f)) { dragging = 2; mouseDragged(mx, my); return true; }
		if (in(mx, my, ax - 4f, svy - 4f, BAR_W + 8f, SV + 8f)) { dragging = 3; mouseDragged(mx, my); return true; }
		if (in(mx, my, rx, top + 48f, rw, 24f)) { hexField.focus(true); return true; }
		for (int i = 0; i < 4; i++) {
			if (in(mx, my, rx + 18f, top + 80f + i * 30f, rw - 18f, 24f)) { rgba[i].focus(true); return true; }
		}
		if (!in(mx, my, x0, y0, W, H)) close(); // click outside commits and closes
		return true;
	}

	public static void mouseDragged(float mx, float my) {
		float svy = y0 + 46f, svx = x0 + PAD;
		switch (dragging) {
			case 1 -> {
				sat = clamp01((mx - svx) / SV);
				val = 1f - clamp01((my - svy) / SV);
			}
			case 2 -> hue = clamp01((my - svy) / SV);
			case 3 -> alpha = Math.round((1f - clamp01((my - svy) / SV)) * 255f);
			default -> { return; }
		}
		apply();
		syncFields();
	}

	public static void mouseReleased() {
		dragging = 0;
	}

	public static boolean charTyped(char c) {
		if (setting == null) return false;
		if (hexField.focused()) { hexField.type(c); return true; }
		for (TextField f : rgba) {
			if (f.focused()) { f.type(c); return true; }
		}
		return true; // modal: swallow
	}

	public static boolean keyPressed(KeyEvent e) {
		if (setting == null) return false;
		boolean shortcut = TextField.isShortcut(e);
		if (hexField.focused()) return hexField.keyPressed(e.key(), shortcut);
		for (TextField f : rgba) {
			if (f.focused()) return f.keyPressed(e.key(), shortcut);
		}
		if (e.isEscape()) close();
		return true; // modal
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static boolean in(float mx, float my, float x, float y, float w, float h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
