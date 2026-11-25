package forfun.miningqol.client.shatter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import xyz.meowing.vexel.api.vg.VGCanvas;
import xyz.meowing.vexel.api.vg.VGFont;
import xyz.meowing.vexel.api.vg.VGPaint;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import static xyz.meowing.vexel.api.vg.VGCanvas.*;

/**
 * Thin vector-drawing wrapper: anti-aliased rounded rects, circles, curves, drop shadows and TTF text.
 * Ported from Odin's NVGRenderer (BSD-3-Clause, © odtheking; design by Stivais/Aton).
 * <p>
 * 26.3: drawn with Vexel's {@link VGCanvas} — NanoVG's drawing model and text metrics, rendered
 * through Minecraft's own GUI pipeline — instead of NanoVG's OpenGL backend, so the menus work on
 * the Vulkan backend as well as OpenGL. The API (and the look) is unchanged.
 * <p>
 * All coordinates are window pixels divided by the device pixel ratio — i.e. "logical" pixels,
 * independent of Minecraft's GUI scale. Draw only inside {@link #draw}/{@link #drawFullScreen}.
 */
public final class NVG {
	private NVG() {}

	public enum Font {
		/** DM Sans 400 — everything readable. */
		SANS("sans", "dm_sans_regular.ttf"),
		/** DM Sans 500 — headers, names. */
		MEDIUM("medium", "dm_sans_medium.ttf"),
		/** JetBrains Mono — key names only. */
		MONO("mono", "jetbrains_mono_regular.ttf");
		final String key, file;
		Font(String key, String file) { this.key = key; this.file = file; }
	}

	private static final VGCanvas vg = new VGCanvas();
	private static final Map<Font, VGFont> fonts = new EnumMap<>(Font.class);
	private static final float[] bounds = new float[4];
	private static Scissor scissor;

	private static void ensureInit() {
		if (!fonts.isEmpty()) return;
		for (Font f : Font.values()) loadFont(f);
	}

	private static void loadFont(Font font) {
		Identifier id = Identifier.fromNamespaceAndPath("miningqol", "font/" + font.file);
		try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow().open()) {
			fonts.put(font, VGFont.fromBytes(font.key, in.readAllBytes()));
		} catch (IOException e) {
			throw new IllegalStateException("Could not read font " + id, e);
		}
	}

	/** Window pixels per logical pixel (2 on a Retina display). */
	public static float devicePixelRatio() {
		return VGCanvas.devicePixelRatio();
	}

	// ---- frames ----

	/**
	 * Draws {@code content} over the GUI rectangle (x, y, w, h) in GUI units, with a frame open in
	 * logical window pixels whose origin is the rectangle's corner. Drawing is clipped to it and
	 * layered like any other GUI element submitted at this point.
	 */
	public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, Runnable content) {
		ensureInit();
		vg.beginFrame(g, x, y, w, h);
		vg.textAlign(ALIGN_LEFT | ALIGN_TOP);
		try {
			content.run();
		} finally {
			vg.endFrame();
			scissor = null;
		}
	}

	/** Full-screen convenience. */
	public static void drawFullScreen(GuiGraphicsExtractor g, Runnable content) {
		draw(g, 0, 0, g.guiWidth(), g.guiHeight(), content);
	}

	// ---- state ----

	public static void push() { vg.save(); }
	public static void pop() { vg.restore(); }
	public static void scale(float x, float y) { vg.scale(x, y); }
	public static void translate(float x, float y) { vg.translate(x, y); }
	public static void globalAlpha(float a) { vg.globalAlpha(Math.max(0f, Math.min(1f, a))); }

	public static void pushScissor(float x, float y, float w, float h) {
		scissor = new Scissor(scissor, x, y, x + w, y + h);
		scissor.apply();
	}

	public static void popScissor() {
		vg.resetScissor();
		scissor = scissor == null ? null : scissor.previous;
		if (scissor != null) scissor.apply();
	}

	// ---- shapes ----

	public static void rect(float x, float y, float w, float h, int argb) {
		vg.beginPath();
		vg.rect(x, y, w, h);
		fill(argb);
	}

	public static void roundedRect(float x, float y, float w, float h, float r, int argb) {
		vg.beginPath();
		vg.roundedRect(x, y, w, h, r);
		fill(argb);
	}

	/** Rounded rect with a 1-px-ish outline drawn inside the bounds. */
	public static void roundedRectBorder(float x, float y, float w, float h, float r, int fillArgb, int borderArgb, float thickness) {
		roundedRect(x, y, w, h, r, fillArgb);
		hollowRoundedRect(x + thickness / 2f, y + thickness / 2f, w - thickness, h - thickness, Math.max(0, r - thickness / 2f), thickness, borderArgb);
	}

	public static void hollowRoundedRect(float x, float y, float w, float h, float r, float thickness, int argb) {
		vg.beginPath();
		vg.roundedRect(x, y, w, h, r);
		vg.strokeWidth(thickness);
		vg.strokeColor(argb);
		vg.stroke();
	}

	public static void gradientRect(float x, float y, float w, float h, float r, int from, int to, boolean vertical) {
		vg.beginPath();
		vg.roundedRect(x, y, w, h, r);
		if (vertical) vg.fillPaint(VGPaint.linearGradient(x, y, x, y + h, from, to));
		else vg.fillPaint(VGPaint.linearGradient(x, y, x + w, y, from, to));
		vg.fill();
	}

	public static void dropShadow(float x, float y, float w, float h, float blur, float spread, float r, float alpha) {
		int inner = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24;
		VGPaint paint = VGPaint.boxGradient(x - spread, y - spread, w + 2 * spread, h + 2 * spread, r + spread, blur, inner, 0);
		vg.beginPath();
		vg.roundedRect(x - spread - blur, y - spread - blur, w + 2 * spread + 2 * blur, h + 2 * spread + 2 * blur, r + spread);
		vg.roundedRect(x, y, w, h, r);
		vg.pathWinding(HOLE);
		vg.fillPaint(paint);
		vg.fill();
	}

	/** Arc of a circle, angles in radians, clockwise from 3 o'clock (NanoVG convention). */
	public static void arc(float cx, float cy, float r, float from, float to, float thickness, int argb) {
		vg.beginPath();
		vg.arc(cx, cy, r, from, to, CW);
		vg.strokeWidth(thickness);
		vg.lineCap(CAP_ROUND);
		vg.strokeColor(argb);
		vg.stroke();
		vg.lineCap(CAP_BUTT);
	}

	/**
	 * A filled annular sector, drawn as one thick stroked arc (mid-radius {@code r}, width {@code thickness},
	 * flat ends).
	 */
	public static void arcBand(float cx, float cy, float r, float from, float to, float thickness, int argb) {
		vg.beginPath();
		vg.arc(cx, cy, r, from, to, CW);
		vg.strokeWidth(thickness);
		vg.lineCap(CAP_BUTT);
		vg.strokeColor(argb);
		vg.stroke();
	}

	/** Dotted ring: {@code dash}-long segments every {@code gap} px, rotated by {@code phase} radians. */
	public static void dottedCircle(float cx, float cy, float r, float dash, float gap, float phase, int argb) {
		float circumference = (float) (2 * Math.PI * r);
		int n = Math.max(1, Math.round(circumference / (dash + gap)));
		float step = (float) (2 * Math.PI / n);
		float dashAngle = dash / r;
		vg.beginPath();
		for (int i = 0; i < n; i++) {
			float a = phase + i * step;
			vg.moveTo(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
			vg.arc(cx, cy, r, a, a + dashAngle, CW);
		}
		vg.strokeWidth(1f);
		vg.strokeColor(argb);
		vg.stroke();
	}

	/** Darkens toward the edges of the rectangle: transparent at {@code inner}, {@code argb} at {@code outer} (radii from the centre). */
	public static void vignette(float x, float y, float w, float h, float inner, float outer, int argb) {
		vg.beginPath();
		vg.rect(x, y, w, h);
		vg.fillPaint(VGPaint.radialGradient(x + w / 2f, y + h / 2f, inner, outer, 0, argb));
		vg.fill();
	}

	public static void circle(float cx, float cy, float r, int argb) {
		vg.beginPath();
		vg.circle(cx, cy, r);
		fill(argb);
	}

	public static void ring(float cx, float cy, float r, float thickness, int argb) {
		vg.beginPath();
		vg.circle(cx, cy, r);
		vg.strokeWidth(thickness);
		vg.strokeColor(argb);
		vg.stroke();
	}

	/** Soft radial glow around a circle. */
	public static void glow(float cx, float cy, float r, float feather, int argb) {
		vg.beginPath();
		vg.circle(cx, cy, r + feather);
		vg.fillPaint(VGPaint.radialGradient(cx, cy, r, r + feather, argb, 0));
		vg.fill();
	}

	public static void line(float x1, float y1, float x2, float y2, float thickness, int argb) {
		vg.beginPath();
		vg.moveTo(x1, y1);
		vg.lineTo(x2, y2);
		vg.strokeWidth(thickness);
		vg.strokeColor(argb);
		vg.stroke();
	}

	/** Quadratic curve from (x1,y1) to (x2,y2) bowing through the control point. */
	public static void curve(float x1, float y1, float cx, float cy, float x2, float y2, float thickness, int argb) {
		vg.beginPath();
		vg.moveTo(x1, y1);
		vg.quadTo(cx, cy, x2, y2);
		vg.strokeWidth(thickness);
		vg.strokeColor(argb);
		vg.stroke();
	}

	// ---- text ----

	private static void font(float size, Font font) {
		ensureInit();
		vg.fontSize(size);
		vg.fontFace(fonts.get(font));
	}

	public static void text(String s, float x, float y, float size, int argb, Font font) {
		font(size, font);
		vg.textAlign(ALIGN_LEFT | ALIGN_TOP);
		vg.fillColor(argb);
		vg.text(x, y, s);
	}

	public static void textCentered(String s, float cx, float cy, float size, int argb, Font font) {
		font(size, font);
		vg.textAlign(ALIGN_CENTER | ALIGN_MIDDLE);
		vg.fillColor(argb);
		vg.text(cx, cy, s);
		vg.textAlign(ALIGN_LEFT | ALIGN_TOP);
	}

	public static void textRight(String s, float right, float y, float size, int argb, Font font) {
		text(s, right - textWidth(s, size, font), y, size, argb, font);
	}

	public static float textWidth(String s, float size, Font font) {
		font(size, font);
		return vg.textBounds(0f, 0f, s, bounds);
	}

	public static void textWrapped(String s, float x, float y, float w, float size, int argb, Font font) {
		font(size, font);
		vg.textLineHeight(1.25f);
		vg.fillColor(argb);
		vg.textBox(x, y, w, s);
	}

	/** Wrapped text, each line centred on {@code cx}. */
	public static void textWrappedCentered(String s, float cx, float y, float w, float size, int argb, Font font) {
		font(size, font);
		vg.textLineHeight(1.15f);
		vg.textAlign(ALIGN_CENTER | ALIGN_TOP);
		vg.fillColor(argb);
		vg.textBox(cx - w / 2f, y, w, s);
		vg.textAlign(ALIGN_LEFT | ALIGN_TOP);
	}

	/** Height of {@code s} wrapped to width {@code w} at line height 1.15. */
	public static float textWrappedHeightTight(String s, float w, float size, Font font) {
		font(size, font);
		vg.textLineHeight(1.15f);
		float[] b = new float[4];
		vg.textBoxBounds(0f, 0f, w, s, b);
		return b[3] - b[1];
	}

	/** Height of {@code s} wrapped to width {@code w}. */
	public static float textWrappedHeight(String s, float w, float size, Font font) {
		font(size, font);
		vg.textLineHeight(1.25f);
		float[] b = new float[4];
		vg.textBoxBounds(0f, 0f, w, s, b);
		return b[3] - b[1];
	}

	/** Truncate with an ellipsis so the string fits in {@code maxWidth}. */
	public static String fit(String s, float maxWidth, float size, Font font) {
		if (textWidth(s, size, font) <= maxWidth) return s;
		float w = maxWidth - textWidth("…", size, font);
		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (textWidth(sb.toString() + c, size, font) > w) break;
			sb.append(c);
		}
		return sb + "…";
	}

	// ---- internals ----

	private static void fill(int argb) {
		vg.fillColor(argb);
		vg.fill();
	}

	private record Scissor(Scissor previous, float x, float y, float maxX, float maxY) {
		void apply() {
			if (previous == null) {
				vg.scissor(x, y, maxX - x, maxY - y);
			} else {
				float nx = Math.max(x, previous.x), ny = Math.max(y, previous.y);
				vg.scissor(nx, ny, Math.max(0, Math.min(maxX, previous.maxX) - nx), Math.max(0, Math.min(maxY, previous.maxY) - ny));
			}
		}
	}
}
