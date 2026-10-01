package forfun.miningqol.client.shatter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dropdown panels across the top of the screen. Click a header to open, click a module to toggle, right-click a
 * module to unfold its settings behind a hairline, drag a header to move. Flat surfaces, no shadows.
 */
public class DropdownSkin implements Skin {
	static final float PANEL_W = 198f;
	static final float HEADER_H = 36f;
	static final float ROW_H = 30f;
	static final float GAP = 10f;
	static final float R = 8f;
	static final float PAD_L = 14f, PAD_R = 12f;

	private final List<Hit> hits = new ArrayList<>();
	private final Map<Category, Anim> openAnims = new HashMap<>();
	private final Map<Module, Anim> expandAnims = new HashMap<>();
	private final Map<Module, Anim> toggleAnims = new HashMap<>();
	private final Map<Module, Long> hoverSince = new HashMap<>();
	private Category dragging;
	private float dragDx, dragDy;
	private boolean dragMoved;
	private Hit sliding;
	private float width, height;
	private Module tooltipModule;
	private boolean tooltipTruncated;
	private float tooltipX, tooltipY;
	private final SearchBar search = new SearchBar();
	/** The move-HUDs button beside the search bar, as last drawn. */
	private Hit moveHit;
	/** How far each panel is scrolled up, and where it was last drawn (for finding it under the mouse). */
	private final Map<Category, Float> scrolls = new HashMap<>();
	private final Map<Category, float[]> drawn = new HashMap<>();

	public DropdownSkin() {
		for (Category c : Category.values()) panel(c);
	}

	private boolean animate() {
		return ShatterUi.animations();
	}

	private float t(Anim a, boolean target) {
		a.set(target);
		return animate() ? a.value() : (target ? 1f : 0f);
	}

	@Override
	public void draw(float mx, float my, float width, float height) {
		this.width = width;
		this.height = height;
		hits.clear();
		tooltipModule = null;
		for (Category c : Category.values()) if (c != dragging) drawPanel(c, mx, my);
		if (dragging != null) drawPanel(dragging, mx, my);
		drawTooltip();
		SettingWidgets.drawPendingTooltip(width, height);
		NVG.text("MiningQOL", 16f, height - 30f, 13f, Theme.FG_2, NVG.Font.MEDIUM);
		search.draw(width / 2f, height - 46f);
		drawMoveButton(width / 2f + 110f + 8f, height - 46f, mx, my);
	}

	/**
	 * The category's saved panel, created in the next free column when it has none — a category
	 * can be registered after the skin was built, or be new since the config was saved.
	 */
	private ShatterConfig.Panel panel(Category c) {
		ShatterConfig.Panel p = ShatterConfig.panel(c);
		if (p == null) {
			p = new ShatterConfig.Panel(Math.round(20f + c.ordinal * (PANEL_W + GAP)), 20, true);
			ShatterConfig.setPanel(c, p);
		}
		return p;
	}

	private void drawPanel(Category c, float mx, float my) {
		ShatterConfig.Panel p = panel(c);
		float x = Math.max(0, Math.min(width - PANEL_W, p.x()));
		float y = Math.max(0, Math.min(height - HEADER_H, p.y()));
		List<Module> modules = Modules.in(c);
		float openT = t(openAnims.computeIfAbsent(c, k -> new Anim(180, p.open())), p.open());

		float body = 0;
		if (openT > 0) {
			body = 12f;
			for (Module m : modules) {
				if (!search.matches(m.name)) continue;
				body += ROW_H + expandedHeight(m);
			}
			if (modules.isEmpty()) body += ROW_H;
		}
		float h = HEADER_H + body * openT;

		// A panel taller than the screen scrolls: the wheel slides it up until its bottom is on
		// screen. Clamped every frame, so folding a module takes the slack back.
		float maxScroll = Math.max(0f, y + h - (height - 8f));
		float scroll = Math.min(scrolls.getOrDefault(c, 0f), maxScroll);
		scrolls.put(c, scroll);
		y -= scroll;
		drawn.put(c, new float[] {x, y, PANEL_W, h, maxScroll});

		float ow = Theme.outlineWidth();
		NVG.roundedRect(x, y, PANEL_W, h, R, Theme.panel(Theme.SURFACE));
		NVG.hollowRoundedRect(x + ow / 2f, y + ow / 2f, PANEL_W - ow, h - ow, R, ow, Theme.outline());

		// header: name, enabled count, thin chevron
		int enabled = (int) modules.stream().filter(Module::isEnabled).count();
		NVG.text(c.label, x + PAD_L, y + 11f, 12.5f, Theme.FG, NVG.Font.MEDIUM);
		if (enabled > 0) NVG.textRight(String.valueOf(enabled), x + PANEL_W - PAD_R - 18f, y + 12f, 11f, Theme.MUTED, NVG.Font.SANS);
		chevron(x + PANEL_W - PAD_R - 5f, y + HEADER_H / 2f, openT);
		hits.add(new Hit(Hit.Kind.HEADER, x, y, PANEL_W, HEADER_H, null, null, c.id()));
		if (openT <= 0) return;

		NVG.rect(x + 1f, y + HEADER_H, PANEL_W - 2f, 1f, Theme.fade(Theme.HAIR, openT));
		NVG.pushScissor(x, y + HEADER_H + 1f, PANEL_W, h - HEADER_H - 1f);
		float cy = y + HEADER_H + 6f;
		if (modules.isEmpty()) NVG.text("Nothing here yet", x + PAD_L, cy + 9f, 12f, Theme.MUTED, NVG.Font.SANS);
		for (Module m : modules) {
			if (!search.matches(m.name)) continue;
			cy = drawModule(m, x, cy, mx, my);
		}
		NVG.popScissor();
	}

	private float expandedHeight(Module m) {
		float et = t(expandAnims.computeIfAbsent(m, k -> new Anim(200, m.expanded)), m.expanded);
		if (et <= 0) return 0;
		return (SettingWidgets.totalHeight(m, settingsWidth(), SettingWidgets.Size.COMPACT) + 12f) * et;
	}

	private float settingsWidth() {
		return PANEL_W - PAD_R - 21f - 12f;
	}

	private float drawModule(Module m, float x, float y, float mx, float my) {
		float labelW = PANEL_W - PAD_L - PAD_R
			- (m.opensScreen() ? 22f : m.hasToggle() ? 46f + (m.bind.isBound() ? 28f : 0f) : 18f);
		String fitted = NVG.fit(m.name, labelW, 12.5f, NVG.Font.SANS);
		boolean truncated = !fitted.equals(m.name);
		boolean hover = mx >= x && my >= y && mx < x + PANEL_W && my < y + ROW_H;
		if (hover) {
			long since = hoverSince.computeIfAbsent(m, k -> System.currentTimeMillis());
			// A truncated name earns its tooltip immediately; descriptions wait for the dwell.
			if (truncated || (System.currentTimeMillis() - since > 500 && !m.description.isEmpty())) {
				tooltipModule = m;
				tooltipTruncated = truncated;
				tooltipX = x + PANEL_W + 10f;
				tooltipY = y;
			}
			NVG.rect(x + 1f, y, PANEL_W - 2f, ROW_H, Theme.panel(Theme.SURFACE_2));
		} else {
			hoverSince.remove(m);
		}

		if (m.opensScreen()) {
			// A screen-opener is a link, not a feature: no toggle pill, no settings chevron.
			NVG.text(fitted, x + PAD_L, y + 8.5f, 12.5f, hover ? Theme.FG : Theme.FG_2, NVG.Font.SANS);
			openArrow(x + PANEL_W - PAD_R - 8f, y + ROW_H / 2f, hover ? Theme.FG : Theme.MUTED);
			hits.add(new Hit(Hit.Kind.MODULE, x, y, PANEL_W, ROW_H, m, null, null));
			return y + ROW_H;
		}

		if (!m.hasToggle()) {
			// Settings-only group: no pill — just the name and the unfold chevron.
			float et2 = t(expandAnims.computeIfAbsent(m, k -> new Anim(200, m.expanded)), m.expanded);
			NVG.text(fitted, x + PAD_L, y + 8.5f, 12.5f, hover || m.expanded ? Theme.FG : Theme.FG_2, NVG.Font.SANS);
			chevron(x + PANEL_W - PAD_R - 5f, y + ROW_H / 2f, et2);
			hits.add(new Hit(Hit.Kind.MODULE, x, y, PANEL_W, ROW_H, m, null, null));
		} else {
			float on = t(toggleAnims.computeIfAbsent(m, k -> new Anim(150, m.isEnabled())), m.isEnabled());
			NVG.text(fitted, x + PAD_L, y + 8.5f, 12.5f, Theme.blend(Theme.FG_2, Theme.FG, on), NVG.Font.SANS);
			// pill toggle 24×12
			float px = x + PANEL_W - PAD_R - 24f, py = y + 9f;
			NVG.roundedRect(px, py, 24f, 12f, 6f, Theme.blend(Theme.SURFACE_3, Theme.ACCENT, on));
			NVG.circle(px + 6f + 12f * on, py + 6f, 4f, Theme.blend(Theme.FG_2, Theme.BG, on));
			// settings affordance: a small chevron that flips while expanded; clicking it unfolds too
			float et = t(expandAnims.computeIfAbsent(m, k -> new Anim(200, m.expanded)), m.expanded);
			chevron(px - 12f, y + ROW_H / 2f, et);
			if (m.bind.isBound()) NVG.textRight(m.bind.display(), px - 22f, y + 10f, 10f, Theme.MUTED, NVG.Font.MONO);
			hits.add(new Hit(Hit.Kind.MODULE, x, y, PANEL_W, ROW_H, m, null, null));
			hits.add(new Hit(Hit.Kind.EXPAND, px - 19f, y + 6f, 16f, ROW_H - 12f, m, null, null));
		}

		float eh = expandedHeight(m);
		if (eh <= 0) return y + ROW_H;
		float sx = x + 21f, sy = y + ROW_H + 2f, sw = settingsWidth();
		NVG.pushScissor(x, sy, PANEL_W, eh - 2f);
		NVG.rect(sx, sy, 1f, eh - 10f, Theme.HAIR_2);
		float cy = sy;
		for (Setting<?> s : m.settings()) cy += SettingWidgets.draw(m, s, sx + 12f, cy, sw, mx, my, hits, SettingWidgets.Size.COMPACT);
		if (m.settings().isEmpty()) NVG.text("No settings", sx + 12f, sy + 2f, SettingWidgets.LABEL, Theme.MUTED, NVG.Font.SANS);
		NVG.popScissor();
		return y + ROW_H + eh;
	}

	/** Four-way arrow: a cross with a head on each end. */
	private static void moveIcon(float cx, float cy, int color) {
		NVG.line(cx - 7f, cy, cx + 7f, cy, 1.2f, color);
		NVG.line(cx, cy - 7f, cx, cy + 7f, 1.2f, color);
		for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			float tx = cx + d[0] * 7f, ty = cy + d[1] * 7f;
			NVG.line(tx, ty, tx - d[0] * 2.5f + d[1] * 2.5f, ty - d[1] * 2.5f + d[0] * 2.5f, 1.2f, color);
			NVG.line(tx, ty, tx - d[0] * 2.5f - d[1] * 2.5f, ty - d[1] * 2.5f - d[0] * 2.5f, 1.2f, color);
		}
	}

	/** The HUD mover, returning to this menu (panels as they were) on Done. */
	private static void openHudEditor() {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		var here = mc.screen;
		mc.schedule(() -> mc.setScreen(new forfun.miningqol.client.gui.HudPositionScreen(here)));
	}

	/** Same height and pill ends as the search bar, sitting just to its right. */
	private void drawMoveButton(float x, float y, float mx, float my) {
		float s = 26f;
		boolean hover = mx >= x && my >= y && mx < x + s && my < y + s;
		float ow = Theme.outlineWidth();
		NVG.roundedRect(x, y, s, s, s / 2f, Theme.panel(hover ? Theme.SURFACE_2 : Theme.SURFACE));
		NVG.hollowRoundedRect(x + ow / 2f, y + ow / 2f, s - ow, s - ow, s / 2f - ow / 2f, ow, hover ? Theme.ACCENT : Theme.outline());
		moveIcon(x + s / 2f, y + s / 2f, hover ? Theme.FG : Theme.MUTED);
		moveHit = new Hit(Hit.Kind.HEADER, x, y, s, s, null, null, "move");
	}

	/** A small ↗-style arrow (the fonts have no such glyph): diagonal shaft plus a corner head. */
	private void openArrow(float cx, float cy, int color) {
		NVG.line(cx - 3f, cy + 3f, cx + 3f, cy - 3f, 1.2f, color);
		NVG.line(cx - 1f, cy - 3f, cx + 3f, cy - 3f, 1.2f, color);
		NVG.line(cx + 3f, cy - 3f, cx + 3f, cy + 1f, 1.2f, color);
	}

	private void chevron(float cx, float cy, float openT) {
		float dir = 1f - 2f * openT; // down when closed, up when open
		NVG.line(cx - 4f, cy - 2f * dir, cx, cy + 2f * dir, 1f, Theme.MUTED);
		NVG.line(cx, cy + 2f * dir, cx + 4f, cy - 2f * dir, 1f, Theme.MUTED);
	}

	private void drawTooltip() {
		if (tooltipModule == null) return;
		boolean showDesc = ShatterUi.descriptions() && !tooltipModule.description.isEmpty();
		if (!showDesc && !tooltipTruncated) return;
		float w = 190f;
		float nameH = tooltipTruncated
			? NVG.textWrappedHeightTight(tooltipModule.name, w - 24f, 12f, NVG.Font.MEDIUM) + 6f : 0f;
		float descH = showDesc ? NVG.textWrappedHeight(tooltipModule.description, w - 24f, 11f, NVG.Font.SANS) : 0f;
		float th = nameH + descH;
		float x = Math.min(tooltipX, width - w - 6f), y = Math.min(tooltipY, height - th - 24f);
		float ow = Theme.outlineWidth();
		NVG.roundedRect(x, y, w, th + 20f, R, Theme.panel(Theme.SURFACE));
		NVG.hollowRoundedRect(x + ow / 2f, y + ow / 2f, w - ow, th + 20f - ow, R, ow, Theme.outline());
		if (tooltipTruncated) {
			NVG.textWrapped(tooltipModule.name, x + 12f, y + 10f, w - 24f, 12f, Theme.FG, NVG.Font.MEDIUM);
		}
		if (showDesc) {
			NVG.textWrapped(tooltipModule.description, x + 12f, y + 10f + nameH, w - 24f, 11f, Theme.FG_2, NVG.Font.SANS);
		}
	}

	// ---- input ----

	@Override
	public boolean mouseScrolled(float mx, float my, double delta) {
		for (Map.Entry<Category, float[]> entry : drawn.entrySet()) {
			float[] r = entry.getValue();
			if (mx < r[0] || mx >= r[0] + r[2] || my < r[1] || my >= r[1] + r[3]) continue;
			float next = scrolls.getOrDefault(entry.getKey(), 0f) - (float) delta * 30f;
			scrolls.put(entry.getKey(), Math.max(0f, Math.min(r[4], next)));
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(float mx, float my, int button) {
		if (SettingWidgets.listening() != null) return SettingWidgets.clickWhileListening(hits, mx, my, button);
		if (search.click(mx, my, button)) return true;
		if (moveHit != null && moveHit.contains(mx, my)) {
			openHudEditor();
			return true;
		}
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!h.contains(mx, my)) continue;
			switch (h.kind()) {
				case HEADER -> {
					Category c = Category.byId(h.option());
					if (button == 0) {
						dragging = c;
						ShatterConfig.Panel p = panel(c);
						dragDx = mx - p.x();
						dragDy = my - p.y();
						dragMoved = false;
					} else if (button == 1) {
						togglePanel(c);
					}
				}
				case EXPAND -> {
					h.module().expanded = !h.module().expanded;
					ShatterConfig.markDirty();
				}
				case MODULE -> {
					if (button == 0) {
						if (h.module().opensScreen() || h.module().hasToggle()) h.module().toggle();
						else h.module().expanded = !h.module().expanded;   // settings-only: unfold
					} else if (button == 1 && !h.module().opensScreen()) {
						h.module().expanded = !h.module().expanded;
					}
					ShatterConfig.markDirty();
				}
				case SLIDER -> {
					sliding = h;
					SettingWidgets.drag(h, mx);
				}
				default -> SettingWidgets.click(h, mx, button);
			}
			return true;
		}
		return false;
	}

	@Override
	public void mouseDragged(float mx, float my) {
		if (dragging != null) {
			ShatterConfig.Panel p = panel(dragging);
			int nx = Math.round(Math.max(0, Math.min(width - PANEL_W, mx - dragDx)));
			int ny = Math.round(Math.max(0, Math.min(height - HEADER_H, my - dragDy)));
			if (nx != p.x() || ny != p.y()) dragMoved = true;
			ShatterConfig.setPanel(dragging, new ShatterConfig.Panel(nx, ny, p.open()));
		} else if (sliding != null) {
			SettingWidgets.drag(sliding, mx);
		}
	}

	@Override
	public void mouseReleased(float mx, float my, int button) {
		if (dragging != null) {
			if (!dragMoved) togglePanel(dragging);
			dragging = null;
		}
		sliding = null;
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		return search.keyPressed(event);
	}

	@Override
	public boolean charTyped(char c) {
		return search.type(c);
	}

	private void togglePanel(Category c) {
		ShatterConfig.Panel p = panel(c);
		ShatterConfig.setPanel(c, new ShatterConfig.Panel(p.x(), p.y(), !p.open()));
	}
}
