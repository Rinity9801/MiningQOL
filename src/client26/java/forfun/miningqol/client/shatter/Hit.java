package forfun.miningqol.client.shatter;


/** A clickable region produced while drawing, so rendering and hit-testing share one layout. */
public record Hit(Kind kind, float x, float y, float w, float h, Module module, Setting<?> setting, String option) {
	public enum Kind { HEADER, MODULE, EXPAND, BOOL, SLIDER, CHIP, KEY, TEXT, COLOR, BUTTON }

	public boolean contains(float mx, float my) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}
}
