package forfun.miningqol.client.shatter;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * A small single-line text box for the colour picker: focus, select-all, a movable caret (arrows,
 * Home/End, forward delete, Cmd/Ctrl+arrows for line start/end) and the clipboard shortcuts
 * (Cmd/Ctrl + A, C, X, V). Selection is all-or-nothing, which is all a hex field needs.
 */
public class TextField {
	private String text = "";
	private boolean focused;
	private boolean allSelected;
	private int caret;
	private final Consumer<String> onChange;

	public TextField(Consumer<String> onChange) {
		this.onChange = onChange;
	}

	/** Cmd on macOS, Ctrl on Windows/Linux — checked on the raw modifier bits so both work everywhere. */
	public static boolean isShortcut(net.minecraft.client.input.KeyEvent e) {
		return (e.modifiers() & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
	}

	public void setText(String text) {
		this.text = text;
		caret = Math.min(caret, text.length());
	}

	public String text() {
		return text;
	}

	public boolean focused() {
		return focused;
	}

	public void focus(boolean selectAll) {
		focused = true;
		allSelected = selectAll;
		caret = text.length();
	}

	public void blur() {
		focused = false;
		allSelected = false;
	}

	public void draw(float x, float y, float w, float h, float textSize) {
		NVG.roundedRect(x, y, w, h, 6f, Theme.alpha(Theme.SURFACE_2, 0.9f));
		NVG.hollowRoundedRect(x + 0.5f, y + 0.5f, w - 1f, h - 1f, 6f, 1f, focused ? Theme.ACCENT : Theme.HAIR_2);
		caret = Math.clamp(caret, 0, text.length());
		float avail = w - 18f;
		// trim to fit, keeping the caret in view
		int start = 0;
		while (NVG.textWidth(text.substring(start), textSize, NVG.Font.MONO) > avail && start < caret) start++;
		String shown = text.substring(start);
		while (NVG.textWidth(shown, textSize, NVG.Font.MONO) > avail && shown.length() > 1) shown = shown.substring(0, shown.length() - 1);
		float tx = x + 9f, ty = y + (h - textSize) / 2f - 1f;
		if (focused && allSelected && !shown.isEmpty()) {
			NVG.roundedRect(tx - 2f, y + 4f, NVG.textWidth(shown, textSize, NVG.Font.MONO) + 4f, h - 8f, 3f, Theme.alpha(Theme.ACCENT, 0.3f));
		}
		NVG.text(shown, tx, ty, textSize, Theme.FG, NVG.Font.MONO);
		if (focused && !allSelected && System.currentTimeMillis() % 1000 < 500) {
			String upToCaret = shown.substring(0, Math.min(caret - start, shown.length()));
			NVG.rect(tx + NVG.textWidth(upToCaret, textSize, NVG.Font.MONO) + 1f, y + 5f, 1f, h - 10f, Theme.FG);
		}
	}

	public void type(char c) {
		if (!focused || c < 32 || c == 127) return;
		if (allSelected) {
			text = "";
			caret = 0;
			allSelected = false;
		}
		caret = Math.clamp(caret, 0, text.length());
		text = text.substring(0, caret) + c + text.substring(caret);
		caret++;
		onChange.accept(text);
	}

	/** @return true when the key was handled. */
	public boolean keyPressed(int key, boolean shortcutDown) {
		if (!focused) return false;
		Minecraft mc = Minecraft.getInstance();
		caret = Math.clamp(caret, 0, text.length());
		if (shortcutDown) {
			switch (key) {
				case GLFW.GLFW_KEY_A -> allSelected = true;
				case GLFW.GLFW_KEY_C -> mc.keyboardHandler.setClipboard(text);
				case GLFW.GLFW_KEY_X -> {
					mc.keyboardHandler.setClipboard(text);
					text = "";
					caret = 0;
					allSelected = false;
					onChange.accept(text);
				}
				case GLFW.GLFW_KEY_V -> {
					String paste = mc.keyboardHandler.getClipboard().replace("\n", "").replace("\r", "");
					if (allSelected) {
						text = "";
						caret = 0;
					}
					allSelected = false;
					text = text.substring(0, caret) + paste + text.substring(caret);
					caret += paste.length();
					onChange.accept(text);
				}
				case GLFW.GLFW_KEY_LEFT -> { // Cmd+Left on macOS: line start
					allSelected = false;
					caret = 0;
				}
				case GLFW.GLFW_KEY_RIGHT -> {
					allSelected = false;
					caret = text.length();
				}
				default -> { return false; }
			}
			return true;
		}
		switch (key) {
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (allSelected) {
					text = "";
					caret = 0;
				} else if (caret > 0) {
					text = text.substring(0, caret - 1) + text.substring(caret);
					caret--;
				}
				allSelected = false;
				onChange.accept(text);
			}
			case GLFW.GLFW_KEY_DELETE -> {
				if (allSelected) {
					text = "";
					caret = 0;
					allSelected = false;
				} else if (caret < text.length()) {
					text = text.substring(0, caret) + text.substring(caret + 1);
				}
				onChange.accept(text);
			}
			case GLFW.GLFW_KEY_LEFT -> {
				if (allSelected) {
					allSelected = false;
					caret = 0;
				} else if (caret > 0) caret--;
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				if (allSelected) {
					allSelected = false;
					caret = text.length();
				} else if (caret < text.length()) caret++;
			}
			case GLFW.GLFW_KEY_HOME -> {
				allSelected = false;
				caret = 0;
			}
			case GLFW.GLFW_KEY_END -> {
				allSelected = false;
				caret = text.length();
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> blur();
			default -> {}
		}
		return true; // swallow everything else while focused
	}
}
