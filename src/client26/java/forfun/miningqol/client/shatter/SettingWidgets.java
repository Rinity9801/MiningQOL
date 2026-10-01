package forfun.miningqol.client.shatter;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The per-setting controls, shared by every skin. Flat: 12 px check squares, a 2 px hairline slider, a
 * segmented control for modes, check rows for multi-selects, a mono key box. Sizes are UI pixels.
 */
public final class SettingWidgets {
	private SettingWidgets() {}

	/** Metrics for one rendering size of the controls. */
	public enum Size {
		/** Inline in the dropdown panels. */
		COMPACT(11.5f, 16f, 10f, 22f, 18f, 12f, 2f, 4f, 10f, 11f);

		public final float label, row, gap, segH, multiRow, box, track, knob, mono, segText;

		Size(float label, float row, float gap, float segH, float multiRow, float box, float track, float knob, float mono, float segText) {
			this.label = label;
			this.row = row;
			this.gap = gap;
			this.segH = segH;
			this.multiRow = multiRow;
			this.box = box;
			this.track = track;
			this.knob = knob;
			this.mono = mono;
			this.segText = segText;
		}
	}

	public static final float LABEL = Size.COMPACT.label;

	private static final Map<Object, Anim> anims = new HashMap<>();
	private static String tipText;
	private static float tipX, tipY;
	private static KeySetting listening;
	private static StringSetting editing;
	private static boolean editingSelected;
	private static int editCaret;

	private static Anim anim(Object key, boolean on) {
		return anims.computeIfAbsent(key, k -> new Anim(120, on));
	}

	public static KeySetting listening() {
		return listening;
	}

	public static void listen(KeySetting k) {
		listening = k;
	}

	public static StringSetting editing() {
		return editing;
	}

	public static void stopEditing() {
		editing = null;
		editingSelected = false;
		editCaret = 0;
	}

	public static boolean editingSelected() {
		return editingSelected;
	}

	/** Feed a typed character into the text field being edited. */
	public static void typeInto(char c) {
		if (editing == null || c < 32 || c == 127) return;
		if (editingSelected) {
			editing.set("");
			editingSelected = false;
			editCaret = 0;
		}
		String t = editing.get();
		editCaret = Math.clamp(editCaret, 0, t.length());
		editing.set(t.substring(0, editCaret) + c + t.substring(editCaret));
		editCaret++;
	}

	public static void backspace() {
		if (editing == null) return;
		if (editingSelected) {
			editing.set("");
			editingSelected = false;
			editCaret = 0;
		} else {
			String t = editing.get();
			editCaret = Math.clamp(editCaret, 0, t.length());
			if (editCaret > 0) {
				editing.set(t.substring(0, editCaret - 1) + t.substring(editCaret));
				editCaret--;
			}
		}
	}

	/** Caret movement and forward delete on the field being edited. @return true when handled. */
	public static boolean editKey(int key) {
		if (editing == null) return false;
		String t = editing.get();
		editCaret = Math.clamp(editCaret, 0, t.length());
		switch (key) {
			case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> {
				if (editingSelected) {
					editingSelected = false;
					editCaret = 0;
				} else if (editCaret > 0) editCaret--;
			}
			case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> {
				if (editingSelected) {
					editingSelected = false;
					editCaret = t.length();
				} else if (editCaret < t.length()) editCaret++;
			}
			case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME -> {
				editingSelected = false;
				editCaret = 0;
			}
			case org.lwjgl.glfw.GLFW.GLFW_KEY_END -> {
				editingSelected = false;
				editCaret = t.length();
			}
			case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE -> {
				if (editingSelected) {
					editing.set("");
					editingSelected = false;
					editCaret = 0;
				} else if (editCaret < t.length()) {
					editing.set(t.substring(0, editCaret) + t.substring(editCaret + 1));
				}
				ShatterConfig.markDirty();
			}
			default -> { return false; }
		}
		return true;
	}

	/** Cmd/Ctrl + A, C, X, V on the field being edited. @return true when handled. */
	public static boolean editShortcut(int key) {
		if (editing == null) return false;
		var keyboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler;
		switch (key) {
			case org.lwjgl.glfw.GLFW.GLFW_KEY_A -> editingSelected = true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_C -> keyboard.setClipboard(editing.get());
			case org.lwjgl.glfw.GLFW.GLFW_KEY_X -> {
				keyboard.setClipboard(editing.get());
				editing.set("");
				editingSelected = false;
				editCaret = 0;
			}
			case org.lwjgl.glfw.GLFW.GLFW_KEY_V -> {
				String paste = keyboard.getClipboard().replace("\n", "").replace("\r", "");
				if (editingSelected) {
					editing.set("");
					editCaret = 0;
				}
				editingSelected = false;
				String t = editing.get();
				editCaret = Math.clamp(editCaret, 0, t.length());
				editing.set(t.substring(0, editCaret) + paste + t.substring(editCaret));
				editCaret += paste.length();
			}
			case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT -> { // Cmd+Left on macOS: line start
				editingSelected = false;
				editCaret = 0;
			}
			case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT -> {
				editingSelected = false;
				editCaret = editing.get().length();
			}
			default -> { return false; }
		}
		return true;
	}

	/** Room the label needs when wrapped into its available width (never truncated). */
	private static float labelBlock(String text, float availW, Size z) {
		return Math.max(z.row, NVG.textWrappedHeightTight(text, availW, z.label, NVG.Font.SANS) + 5f);
	}

	private static float boolLabelWidth(float w, Size z) {
		return w - z.box - 10f;
	}

	private static final float KEY_RESERVE = 66f;
	private static final float SLIDER_RESERVE = 64f;

	/** Height of one setting including the gap below it. */
	public static float height(Setting<?> s, float w, Size z) {
		if (s instanceof SliderSetting sl) return labelBlock(sl.name, w - SLIDER_RESERVE, z) + z.track + 12f + z.gap;
		if (s instanceof ModeSetting mo) return labelBlock(mo.name, w, z) + 5f + z.segH + z.gap;
		if (s instanceof MultiSetting mu) return labelBlock(mu.name, w, z) + 5f + mu.options.size() * z.multiRow + z.gap;
		if (s instanceof KeySetting k) return labelBlock(k.name, w - KEY_RESERVE, z) + z.gap;
		if (s instanceof StringSetting st) return labelBlock(st.name, w, z) + 5f + z.segH + z.gap;
		if (s instanceof ColorSetting c) return labelBlock(c.name, w - 40f, z) + z.gap;
		if (s instanceof ActionSetting) return z.segH + z.gap;
		return labelBlock(s.name, boolLabelWidth(w, z), z) + z.gap;
	}

	public static float totalHeight(Module m, float w, Size z) {
		float h = 0;
		for (Setting<?> s : m.settings()) h += height(s, w, z);
		return Math.max(0, h - z.gap);
	}

	/** Draws the setting at (x, y) with width w and records hits. Returns the height consumed. */
	public static float draw(Module m, Setting<?> s, float x, float y, float w, float mx, float my, List<Hit> hits, Size z) {
		switch (s) {
			case BoolSetting b -> {
				float lb = labelBlock(b.name, boolLabelWidth(w, z), z);
				label(b.name, x, y, boolLabelWidth(w, z), z);
				checkbox(b, x + w - z.box, y + (lb - z.box) / 2f - 1f, b.get(), z);
				hits.add(new Hit(Hit.Kind.BOOL, x, y, w, lb, m, s, null));
			}
			case SliderSetting sl -> {
				float lb = labelBlock(sl.name, w - SLIDER_RESERVE, z);
				label(sl.name, x, y, w - SLIDER_RESERVE, z);
				NVG.textRight(sl.display(), x + w, y + 2.5f, z.label - 0.5f, Theme.FG, NVG.Font.SANS);
				float ty = y + lb + 4f;
				NVG.rect(x, ty, w, z.track, Theme.SURFACE_3);
				float fill = (float) (w * sl.fraction());
				if (fill > 0) NVG.rect(x, ty, fill, z.track, Theme.ACCENT);
				NVG.circle(x + fill, ty + z.track / 2f, z.knob, Theme.FG);
				NVG.ring(x + fill, ty + z.track / 2f, z.knob, 1f, Theme.alpha(Theme.BG, 0.9f));
				hits.add(new Hit(Hit.Kind.SLIDER, x, ty - 6f, w, z.track + 12f, m, s, null));
			}
			case ModeSetting mo -> {
				label(mo.name, x, y, w, z);
				float sy = y + labelBlock(mo.name, w, z) + 3f;
				NVG.roundedRect(x, sy, w, z.segH, 5f, Theme.SURFACE_2);
				int n = mo.options.size();
				float cw = (w - 4f - 2f * (n - 1)) / n;
				for (int i = 0; i < n; i++) {
					String o = mo.options.get(i);
					float cx = x + 2f + i * (cw + 2f);
					Anim a = anim(new Key(s, o), mo.is(o));
					a.set(mo.is(o));
					float t = a.value();
					if (t > 0) NVG.roundedRect(cx, sy + 2f, cw, z.segH - 4f, 4f, Theme.fade(Theme.SURFACE_3, t));
					String fitted = NVG.fit(o, cw - 8f, z.segText, NVG.Font.SANS);
					NVG.textCentered(fitted, cx + cw / 2f, sy + z.segH / 2f, z.segText, Theme.blend(Theme.MUTED, Theme.FG, t), NVG.Font.SANS);
					maybeTip(o, fitted, cx, sy, cw, z.segH, mx, my);
					hits.add(new Hit(Hit.Kind.CHIP, cx, sy, cw, z.segH, m, s, o));
				}
			}
			case MultiSetting mu -> {
				label(mu.name, x, y, w, z);
				float ry = y + labelBlock(mu.name, w, z) + 3f;
				for (String o : mu.options) {
					checkbox(new Key(s, o), x, ry + (z.multiRow - z.box) / 2f, mu.has(o), z);
					String fitted = NVG.fit(o, w - z.box - 10f, z.label, NVG.Font.SANS);
					NVG.text(fitted, x + z.box + 8f, ry + (z.multiRow - z.label) / 2f - 1f, z.label, Theme.FG_2, NVG.Font.SANS);
					maybeTip(o, fitted, x, ry, w, z.multiRow, mx, my);
					hits.add(new Hit(Hit.Kind.CHIP, x, ry, w, z.multiRow, m, s, o));
					ry += z.multiRow;
				}
			}
			case ColorSetting c -> {
				float lb = labelBlock(c.name, w - 40f, z);
				label(c.name, x, y, w - 40f, z);
				float sw = 30f, sh = z.row - 2f, sx = x + w - sw, sy = y + (lb - sh) / 2f;
				// checker under the colour so translucency reads
				NVG.roundedRect(sx, sy, sw, sh, 4f, 0xFF232323);
				NVG.rect(sx + sw / 3f, sy, sw / 3f, sh / 2f, 0xFF3A3A3A);
				NVG.rect(sx, sy + sh / 2f, sw / 3f, sh / 2f, 0xFF3A3A3A);
				NVG.rect(sx + 2 * sw / 3f, sy + sh / 2f, sw / 3f, sh / 2f, 0xFF3A3A3A);
				NVG.roundedRect(sx, sy, sw, sh, 4f, c.get());
				NVG.hollowRoundedRect(sx + 0.5f, sy + 0.5f, sw - 1f, sh - 1f, 4f, 1f, Theme.HAIR_2);
				hits.add(new Hit(Hit.Kind.COLOR, sx, sy, sw, sh, m, s, null));
			}
			case StringSetting st -> {
				label(st.name, x, y, w, z);
				float by = y + labelBlock(st.name, w, z) + 3f;
				boolean live = st == editing;
				NVG.roundedRect(x, by, w, z.segH, 5f, Theme.alpha(Theme.SURFACE_2, 0.9f));
				NVG.hollowRoundedRect(x + 0.5f, by + 0.5f, w - 1f, z.segH - 1f, 5f, 1f, live ? Theme.ACCENT : Theme.HAIR_2);
				boolean placeholder = st.get().isEmpty() && !live;
				String full = placeholder ? st.placeholder : st.get();
				int color = placeholder ? Theme.MUTED : Theme.FG;
				if (live) editCaret = Math.clamp(editCaret, 0, st.get().length());
				int caret = live ? editCaret : full.length();
				// trim to fit, keeping the caret in view
				int startIdx = 0;
				while (NVG.textWidth(full.substring(startIdx), z.segText, NVG.Font.SANS) > w - 18f && startIdx < caret) startIdx++;
				String shown = full.substring(startIdx);
				while (NVG.textWidth(shown, z.segText, NVG.Font.SANS) > w - 18f && shown.length() > 1) shown = shown.substring(0, shown.length() - 1);
				if (live && editingSelected && !shown.isEmpty()) {
					NVG.roundedRect(x + 6f, by + 4f, NVG.textWidth(shown, z.segText, NVG.Font.SANS) + 4f, z.segH - 8f, 3f, Theme.alpha(Theme.ACCENT, 0.3f));
				}
				NVG.text(shown, x + 8f, by + (z.segH - z.segText) / 2f - 1f, z.segText, color, NVG.Font.SANS);
				if (live && !editingSelected && System.currentTimeMillis() % 1000 < 500) {
					String upToCaret = shown.substring(0, Math.min(caret - startIdx, shown.length()));
					float cx2 = x + 8f + NVG.textWidth(upToCaret, z.segText, NVG.Font.SANS) + 1f;
					NVG.rect(cx2, by + 5f, 1f, z.segH - 10f, Theme.FG);
				}
				hits.add(new Hit(Hit.Kind.TEXT, x, by, w, z.segH, m, s, null));
			}
			case ActionSetting ac -> {
				boolean hover = mx >= x && my >= y && mx < x + w && my < y + z.segH;
				int tone = ac.danger ? 0xFFE0605A : Theme.ACCENT;
				NVG.roundedRect(x, y, w, z.segH, 5f, Theme.alpha(tone, hover ? 0.22f : 0.12f));
				NVG.hollowRoundedRect(x + 0.5f, y + 0.5f, w - 1f, z.segH - 1f, 5f, 1f, Theme.alpha(tone, 0.6f));
				String fitted = NVG.fit(ac.label(), w - 12f, z.segText, NVG.Font.SANS);
				NVG.textCentered(fitted, x + w / 2f, y + z.segH / 2f, z.segText, hover ? Theme.FG : Theme.FG_2, NVG.Font.SANS);
				hits.add(new Hit(Hit.Kind.BUTTON, x, y, w, z.segH, m, s, null));
			}
			case KeySetting k -> {
				float lb = labelBlock(k.name, w - KEY_RESERVE, z);
				label(k.name, x, y, w - KEY_RESERVE, z);
				boolean live = k == listening;
				String text = live ? "press…" : k.display();
				float kw = NVG.textWidth(text, z.mono, NVG.Font.MONO) + 14f;
				float kx = x + w - kw;
				float ky = y + (lb - z.row) / 2f;
				NVG.hollowRoundedRect(kx + 0.5f, ky + 0.5f, kw - 1f, z.row - 1f, 4f, 1f, live ? Theme.ACCENT : Theme.HAIR_2);
				NVG.textCentered(text, kx + kw / 2f, ky + z.row / 2f, z.mono, live ? Theme.ACCENT : Theme.FG_2, NVG.Font.MONO);
				hits.add(new Hit(Hit.Kind.KEY, kx, ky, kw, z.row, m, s, null));
			}
			default -> {}
		}
		return height(s, w, z);
	}

	/** Remembers a truncated label under the cursor; the active skin draws it last. */
	private static void maybeTip(String full, String shown, float x, float y, float w, float h, float mx, float my) {
		if (shown.equals(full)) return;
		if (mx < x || my < y || mx >= x + w || my >= y + h) return;
		tipText = full;
		tipX = mx + 10f;
		tipY = my + 12f;
	}

	/** Draws (and clears) the pending truncation tooltip. Call at the end of a skin's draw. */
	public static void drawPendingTooltip(float screenW, float screenH) {
		if (tipText == null) return;
		float pad = 8f;
		float size = 11.5f;
		float w = Math.min(NVG.textWidth(tipText, size, NVG.Font.SANS) + pad * 2f, 260f);
		float th = NVG.textWrappedHeightTight(tipText, w - pad * 2f, size, NVG.Font.SANS);
		float x = Math.min(tipX, screenW - w - 6f);
		float y = Math.min(tipY, screenH - th - pad * 2f - 6f);
		NVG.roundedRect(x, y, w, th + pad * 2f - 2f, 6f, Theme.SURFACE);
		NVG.hollowRoundedRect(x + 0.5f, y + 0.5f, w - 1f, th + pad * 2f - 3f, 6f, 1f, Theme.HAIR);
		NVG.textWrapped(tipText, x + pad, y + pad - 2f, w - pad * 2f, size, Theme.FG, NVG.Font.SANS);
		tipText = null;
	}

	private static void label(String text, float x, float y, float availW, Size z) {
		NVG.textWrapped(text, x, y + 2f, availW, z.label, Theme.FG_2, NVG.Font.SANS);
	}

	private static void checkbox(Object key, float x, float y, boolean on, Size z) {
		Anim a = anim(key, on);
		a.set(on);
		float t = a.value();
		NVG.hollowRoundedRect(x + 0.5f, y + 0.5f, z.box - 1f, z.box - 1f, 3f, 1f, Theme.HAIR_2);
		if (t > 0) {
			float sz = (z.box / 2f) * t;
			NVG.roundedRect(x + z.box / 2f - sz / 2f, y + z.box / 2f - sz / 2f, sz, sz, 1.5f, Theme.ACCENT);
		}
	}

	private record Key(Setting<?> setting, String option) {}

	/** Handle a click on a setting hit. @return true if consumed. */
	public static boolean click(Hit h, float mx, int button) {
		switch (h.kind()) {
			case BOOL -> ((BoolSetting) h.setting()).toggle();
			case SLIDER -> ((SliderSetting) h.setting()).setFraction((mx - h.x()) / h.w());
			case CHIP -> {
				if (h.setting() instanceof ModeSetting mo) mo.set(h.option());
				else if (h.setting() instanceof MultiSetting mu) mu.toggle(h.option());
			}
			case KEY -> {
				if (button == 1) ((KeySetting) h.setting()).set(KeySetting.NONE);
				else listening = (KeySetting) h.setting();
			}
			case TEXT -> {
				if (button == 1) ((StringSetting) h.setting()).set("");
				editing = (StringSetting) h.setting();
				editCaret = editing.get().length();
			}
			case COLOR -> ColorPicker.open((ColorSetting) h.setting());
			case BUTTON -> {
				if (button != 0) return true;
				((ActionSetting) h.setting()).run();
			}
			default -> { return false; }
		}
		ShatterConfig.markDirty();
		return true;
	}

	/**
	 * A click while a key box is armed: on that same box, a mouse-capable key binds the button
	 * used; anywhere else the capture is just cancelled. Always consumes the click.
	 */
	public static boolean clickWhileListening(List<Hit> hits, float mx, float my, int button) {
		KeySetting k = listening;
		listening = null;
		if (k == null || !k.allowMouse) return true;
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (h.kind() == Hit.Kind.KEY && h.setting() == k && h.contains(mx, my)) {
				k.set(org.lwjgl.glfw.GLFW.GLFW_KEY_LAST + button + 1);
				ShatterConfig.markDirty();
				break;
			}
		}
		return true;
	}

	public static void drag(Hit slider, float mx) {
		((SliderSetting) slider.setting()).setFraction((mx - slider.x()) / slider.w());
		ShatterConfig.markDirty();
	}

	/** @deprecated use {@link Theme#blend}. */
	public static int blend(int a, int b, float t) {
		return Theme.blend(a, b, t);
	}
}
