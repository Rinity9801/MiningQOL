package forfun.miningqol.client.shatter;

import net.minecraft.client.input.KeyEvent;

/** One look for the menu. Coordinates are UI pixels (already divided by the UI scale). */
public interface Skin {
	void draw(float mx, float my, float width, float height);

	boolean mouseClicked(float mx, float my, int button);

	void mouseDragged(float mx, float my);

	void mouseReleased(float mx, float my, int button);

	default boolean mouseScrolled(float mx, float my, double delta) {
		return false;
	}

	default boolean keyPressed(KeyEvent event) {
		return false;
	}

	default boolean charTyped(char c) {
		return false;
	}

	default void closed() {}
}
