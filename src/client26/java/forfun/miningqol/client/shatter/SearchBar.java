package forfun.miningqol.client.shatter;

import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** The menu's search pill: click to focus, type to filter modules, Esc or click-away to leave. */
public class SearchBar {
	private static final float W = 220f, H = 26f;

	private String text = "";
	private boolean focused;
	private boolean selected;
	private int caret;
	private float x, y;

	public boolean active() {
		return !text.isEmpty();
	}

	/** Empty and unfocused. */
	public void clear() {
		text = "";
		caret = 0;
		selected = false;
		focused = false;
	}

	public boolean focused() {
		return focused;
	}

	public boolean matches(String name) {
		return text.isEmpty() || name.toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT));
	}

	public void draw(float centerX, float y) {
		this.x = centerX - W / 2f;
		this.y = y;
		float ow = Theme.outlineWidth();
		NVG.roundedRect(x, y, W, H, H / 2f, Theme.panel(Theme.SURFACE));
		NVG.hollowRoundedRect(x + ow / 2f, y + ow / 2f, W - ow, H - ow, H / 2f - ow / 2f, ow, focused ? Theme.ACCENT : Theme.outline());
		// magnifier: ring + handle
		float ix = x + 15f, iy = y + H / 2f - 1.5f;
		NVG.ring(ix, iy, 4f, 1.2f, Theme.MUTED);
		NVG.line(ix + 3f, iy + 3f, ix + 6f, iy + 6f, 1.2f, Theme.MUTED);
		caret = Math.clamp(caret, 0, text.length());
		float avail = W - 46f;
		// trim to fit, keeping the caret in view
		int start = 0;
		while (NVG.textWidth(text.substring(start), 12f, NVG.Font.SANS) > avail && start < caret) start++;
		String shown = text.substring(start);
		while (NVG.textWidth(shown, 12f, NVG.Font.SANS) > avail && shown.length() > 1) shown = shown.substring(0, shown.length() - 1);
		if (shown.isEmpty() && !focused) {
			NVG.text("search", x + 26f, y + 7f, 12f, Theme.MUTED, NVG.Font.SANS);
		} else {
			if (focused && selected && !shown.isEmpty()) {
				NVG.roundedRect(x + 24f, y + 5f, NVG.textWidth(shown, 12f, NVG.Font.SANS) + 4f, H - 10f, 3f, Theme.alpha(Theme.ACCENT, 0.3f));
			}
			NVG.text(shown, x + 26f, y + 7f, 12f, Theme.FG, NVG.Font.SANS);
			if (focused && !selected && System.currentTimeMillis() % 1000 < 500) {
				String upToCaret = shown.substring(0, Math.min(caret - start, shown.length()));
				NVG.rect(x + 27f + NVG.textWidth(upToCaret, 12f, NVG.Font.SANS), y + 6f, 1f, H - 12f, Theme.FG);
			}
		}
	}

	/** @return true when the click landed on the bar (focused); clicking elsewhere blurs. */
	public boolean click(float mx, float my, int button) {
		boolean inside = mx >= x && my >= y && mx < x + W && my < y + H;
		focused = inside;
		if (inside) {
			if (button == 1) text = "";
			caret = text.length();
		}
		return inside;
	}

	/** @return true when the character was consumed. */
	public boolean type(char c) {
		if (!focused || c < 32 || c == 127) return focused;
		if (selected) {
			text = "";
			caret = 0;
			selected = false;
		}
		caret = Math.clamp(caret, 0, text.length());
		text = text.substring(0, caret) + c + text.substring(caret);
		caret++;
		return true;
	}

	/** Handle a key while focused. @return true when consumed (all keys are, while focused). */
	public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
		if (!focused) return false;
		var keyboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler;
		caret = Math.clamp(caret, 0, text.length());
		if (TextField.isShortcut(e)) {
			switch (e.key()) {
				case GLFW.GLFW_KEY_A -> selected = true;
				case GLFW.GLFW_KEY_C -> keyboard.setClipboard(text);
				case GLFW.GLFW_KEY_X -> {
					keyboard.setClipboard(text);
					text = "";
					caret = 0;
					selected = false;
				}
				case GLFW.GLFW_KEY_V -> {
					String paste = keyboard.getClipboard().replace("\n", "").replace("\r", "");
					if (selected) {
						text = "";
						caret = 0;
					}
					selected = false;
					text = text.substring(0, caret) + paste + text.substring(caret);
					caret += paste.length();
				}
				case GLFW.GLFW_KEY_LEFT -> { // Cmd+Left on macOS: line start
					selected = false;
					caret = 0;
				}
				case GLFW.GLFW_KEY_RIGHT -> {
					selected = false;
					caret = text.length();
				}
				default -> {}
			}
			return true;
		}
		if (e.isEscape()) {
			text = "";
			caret = 0;
			focused = false;
			selected = false;
		} else if (e.key() == GLFW.GLFW_KEY_BACKSPACE) {
			if (selected) {
				text = "";
				caret = 0;
				selected = false;
			} else if (caret > 0) {
				text = text.substring(0, caret - 1) + text.substring(caret);
				caret--;
			}
		} else if (e.key() == GLFW.GLFW_KEY_DELETE) {
			if (selected) {
				text = "";
				caret = 0;
				selected = false;
			} else if (caret < text.length()) {
				text = text.substring(0, caret) + text.substring(caret + 1);
			}
		} else if (e.key() == GLFW.GLFW_KEY_LEFT) {
			if (selected) {
				selected = false;
				caret = 0;
			} else if (caret > 0) caret--;
		} else if (e.key() == GLFW.GLFW_KEY_RIGHT) {
			if (selected) {
				selected = false;
				caret = text.length();
			} else if (caret < text.length()) caret++;
		} else if (e.key() == GLFW.GLFW_KEY_HOME) {
			selected = false;
			caret = 0;
		} else if (e.key() == GLFW.GLFW_KEY_END) {
			selected = false;
			caret = text.length();
		} else if (e.key() == GLFW.GLFW_KEY_ENTER || e.key() == GLFW.GLFW_KEY_KP_ENTER) {
			focused = false;
			selected = false;
		}
		return true;
	}
}
