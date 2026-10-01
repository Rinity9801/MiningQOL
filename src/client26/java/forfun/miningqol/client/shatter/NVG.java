package forfun.miningqol.client.shatter;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.nanovg.NanoVG.*;

/**
 * Thin NanoVG wrapper: anti-aliased rounded rects, circles, curves, drop shadows and TTF text.
 * Ported from Odin's NVGRenderer (BSD-3-Clause, © odtheking; design by Stivais/Aton).
 * <p>
 * All coordinates are window pixels divided by the device pixel ratio — i.e. "logical" pixels,
 * independent of Minecraft's GUI scale. Only call between {@link #beginFrame} and {@link #endFrame}.
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

	private static long vg = -1;
	private static final NVGColor color = NVGColor.malloc();
	private static final NVGColor color2 = NVGColor.malloc();
	private static final NVGPaint paint = NVGPaint.malloc();
	private static final float[] bounds = new float[4];
	private static final Map<Font, Integer> fonts = new HashMap<>();
	private static final Map<Font, ByteBuffer> fontData = new HashMap<>(); // must stay alive while NanoVG uses it
	private static Scissor scissor;
	private static boolean drawing;

	private static void ensureInit() {
		if (vg != -1) return;
		// No NVG_STENCIL_STROKES: the picture-in-picture framebuffer has a depth attachment but no stencil,
		// and stencil strokes without a stencil buffer draw every stroke twice (hard, aliased edges).
		vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS);
		if (vg == 0) throw new IllegalStateException("Failed to initialize NanoVG");
		for (Font f : Font.values()) loadFont(f);
	}

	private static void loadFont(Font font) {
		Identifier id = Identifier.fromNamespaceAndPath("miningqol", "font/" + font.file);
		try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(id).orElseThrow().open()) {
			byte[] bytes = in.readAllBytes();
			ByteBuffer buf = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
			buf.put(bytes).flip();
			int handle = nvgCreateFontMem(vg, font.key, buf, false);
			if (handle == -1) throw new IllegalStateException("NanoVG rejected font " + font.file);
			fonts.put(font, handle);
			fontData.put(font, buf);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read font " + id, e);
		}
	}

	/** Window pixels per logical pixel (2 on a Retina display). */
	public static float devicePixelRatio() {
		Minecraft mc = Minecraft.getInstance();
		int screenW = mc.getWindow().getScreenWidth();
		return screenW == 0 ? 1f : (float) mc.getWindow().getWidth() / screenW;
	}

	public static void beginFrame(float width, float height) {
		ensureInit();
		if (drawing) throw new IllegalStateException("NVG.beginFrame called while already drawing");
		float dpr = devicePixelRatio();
		nvgBeginFrame(vg, width / dpr, height / dpr, dpr);
		// NanoVG's GL backend outputs premultiplied colour with a (ONE, ONE_MINUS_SRC_ALPHA) composite by
		// default, which is exactly what Minecraft's premultiplied blit of this texture expects. Don't change it.
		nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		drawing = true;
	}

	public static void endFrame() {
		if (!drawing) throw new IllegalStateException("NVG.endFrame called while not drawing");
		nvgEndFrame(vg);
		drawing = false;
		scissor = null;
	}

	// ---- state ----

	public static void push() { nvgSave(vg); }
	public static void pop() { nvgRestore(vg); }
	public static void scale(float x, float y) { nvgScale(vg, x, y); }
	public static void translate(float x, float y) { nvgTranslate(vg, x, y); }
	public static void globalAlpha(float a) { nvgGlobalAlpha(vg, Math.max(0f, Math.min(1f, a))); }

	public static void pushScissor(float x, float y, float w, float h) {
		scissor = new Scissor(scissor, x, y, x + w, y + h);
		scissor.apply();
	}

	public static void popScissor() {
		nvgResetScissor(vg);
		scissor = scissor == null ? null : scissor.previous;
		if (scissor != null) scissor.apply();
	}

	// ---- shapes ----

	public static void rect(float x, float y, float w, float h, int argb) {
		nvgBeginPath(vg);
		nvgRect(vg, x, y, w, h);
		fill(argb);
	}

	public static void roundedRect(float x, float y, float w, float h, float r, int argb) {
		nvgBeginPath(vg);
		nvgRoundedRect(vg, x, y, w, h, r);
		fill(argb);
	}

	/** Rounded rect with a 1-px-ish outline drawn inside the bounds. */
	public static void roundedRectBorder(float x, float y, float w, float h, float r, int fillArgb, int borderArgb, float thickness) {
		roundedRect(x, y, w, h, r, fillArgb);
		hollowRoundedRect(x + thickness / 2f, y + thickness / 2f, w - thickness, h - thickness, Math.max(0, r - thickness / 2f), thickness, borderArgb);
	}

	public static void hollowRoundedRect(float x, float y, float w, float h, float r, float thickness, int argb) {
		nvgBeginPath(vg);
		nvgRoundedRect(vg, x, y, w, h, r);
		nvgStrokeWidth(vg, thickness);
		setColor(argb, color);
		nvgStrokeColor(vg, color);
		nvgStroke(vg);
	}

	public static void gradientRect(float x, float y, float w, float h, float r, int from, int to, boolean vertical) {
		nvgBeginPath(vg);
		nvgRoundedRect(vg, x, y, w, h, r);
		setColor(from, color);
		setColor(to, color2);
		if (vertical) nvgLinearGradient(vg, x, y, x, y + h, color, color2, paint);
		else nvgLinearGradient(vg, x, y, x + w, y, color, color2, paint);
		nvgFillPaint(vg, paint);
		nvgFill(vg);
	}

	public static void dropShadow(float x, float y, float w, float h, float blur, float spread, float r, float alpha) {
		nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) Math.round(alpha * 255), color);
		nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) 0, color2);
		nvgBoxGradient(vg, x - spread, y - spread, w + 2 * spread, h + 2 * spread, r + spread, blur, color, color2, paint);
		nvgBeginPath(vg);
		nvgRoundedRect(vg, x - spread - blur, y - spread - blur, w + 2 * spread + 2 * blur, h + 2 * spread + 2 * blur, r + spread);
		nvgRoundedRect(vg, x, y, w, h, r);
		nvgPathWinding(vg, NVG_HOLE);
		nvgFillPaint(vg, paint);
		nvgFill(vg);
	}

	/** Arc of a circle, angles in radians, clockwise from 3 o'clock (NanoVG convention). */
	public static void arc(float cx, float cy, float r, float from, float to, float thickness, int argb) {
		nvgBeginPath(vg);
		nvgArc(vg, cx, cy, r, from, to, NVG_CW);
		nvgStrokeWidth(vg, thickness);
		nvgLineCap(vg, NVG_ROUND);
		setColor(argb, color);
		nvgStrokeColor(vg, color);
		nvgStroke(vg);
		nvgLineCap(vg, NVG_BUTT);
	}

	/**
	 * A filled annular sector, drawn as one thick stroked arc (mid-radius {@code r}, width {@code thickness},
	 * flat ends) — strokes render correctly without a stencil buffer, unlike concave fills.
	 */
	public static void arcBand(float cx, float cy, float r, float from, float to, float thickness, int argb) {
		nvgBeginPath(vg);
		nvgArc(vg, cx, cy, r, from, to, NVG_CW);
		nvgStrokeWidth(vg, thickness);
		nvgLineCap(vg, NVG_BUTT);
		setColor(argb, color);
		nvgStrokeColor(vg, color);
		nvgStroke(vg);
	}

	/** Dotted ring: {@code dash}-long segments every {@code gap} px, rotated by {@code phase} radians. */
	public static void dottedCircle(float cx, float cy, float r, float dash, float gap, float phase, int argb) {
		float circumference = (float) (2 * Math.PI * r);
		int n = Math.max(1, Math.round(circumference / (dash + gap)));
		float step = (float) (2 * Math.PI / n);
		float dashAngle = dash / r;
		nvgBeginPath(vg);
		for (int i = 0; i < n; i++) {
			float a = phase + i * step;
			nvgMoveTo(vg, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
			nvgArc(vg, cx, cy, r, a, a + dashAngle, NVG_CW);
		}
		nvgStrokeWidth(vg, 1f);
		setColor(argb, color);
		nvgStrokeColor(vg, color);
		nvgStroke(vg);
	}

	/** Darkens toward the edges of the rectangle: transparent at {@code inner}, {@code argb} at {@code outer} (radii from the centre). */
	public static void vignette(float x, float y, float w, float h, float inner, float outer, int argb) {
		nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) 0, color2);
		setColor(argb, color);
		nvgRadialGradient(vg, x + w / 2f, y + h / 2f, inner, outer, color2, color, paint);
		nvgBeginPath(vg);
		nvgRect(vg, x, y, w, h);
		nvgFillPaint(vg, paint);
		nvgFill(vg);
	}

	public static void circle(float cx, float cy, float r, int argb) {
		nvgBeginPath(vg);
		nvgCircle(vg, cx, cy, r);
		fill(argb);
	}

	public static void ring(float cx, float cy, float r, float thickness, int argb) {
		nvgBeginPath(vg);
		nvgCircle(vg, cx, cy, r);
		nvgStrokeWidth(vg, thickness);
		setColor(argb, color);
		nvgStrokeColor(vg, color);
		nvgStroke(vg);
	}

	/** Soft radial glow around a circle. */
	public static void glow(float cx, float cy, float r, float feather, int argb) {
		setColor(argb, color);
		nvgRGBA((byte) 0, (byte) 0, (byte) 0, (byte) 0, color2);
		nvgRadialGradient(vg, cx, cy, r, r + feather, color, color2, paint);
		nvgBeginPath(vg);
		nvgCircle(vg, cx, cy, r + feather);
		nvgFillPaint(vg, paint);
		nvgFill(vg);
	}

	public static void line(float x1, float y1, float x2, float y2, float thickness, int argb) {
		nvgBeginPath(vg);
		nvgMoveTo(vg, x1, y1);
		nvgLineTo(vg, x2, y2);
		nvgStrokeWidth(vg, thickness);
		setColor(argb, color);
		nvgStrokeColor(vg, color);
		nvgStroke(vg);
	}

	/** Quadratic curve from (x1,y1) to (x2,y2) bowing through the control point. */
	public static void curve(float x1, float y1, float cx, float cy, float x2, float y2, float thickness, int argb) {
		nvgBeginPath(vg);
		nvgMoveTo(vg, x1, y1);
		nvgQuadTo(vg, cx, cy, x2, y2);
		nvgStrokeWidth(vg, thickness);
		setColor(argb, color);
		nvgStrokeColor(vg, color);
		nvgStroke(vg);
	}

	// ---- text ----

	public static void text(String s, float x, float y, float size, int argb, Font font) {
		nvgFontSize(vg, size);
		nvgFontFaceId(vg, fonts.get(font));
		nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
		setColor(argb, color);
		nvgFillColor(vg, color);
		nvgText(vg, x, y, s);
	}

	public static void textCentered(String s, float cx, float cy, float size, int argb, Font font) {
		nvgFontSize(vg, size);
		nvgFontFaceId(vg, fonts.get(font));
		nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
		setColor(argb, color);
		nvgFillColor(vg, color);
		nvgText(vg, cx, cy, s);
		nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
	}

	public static void textRight(String s, float right, float y, float size, int argb, Font font) {
		text(s, right - textWidth(s, size, font), y, size, argb, font);
	}

	public static float textWidth(String s, float size, Font font) {
		ensureInit();
		nvgFontSize(vg, size);
		nvgFontFaceId(vg, fonts.get(font));
		return nvgTextBounds(vg, 0f, 0f, s, bounds);
	}

	public static void textWrapped(String s, float x, float y, float w, float size, int argb, Font font) {
		nvgFontSize(vg, size);
		nvgFontFaceId(vg, fonts.get(font));
		nvgTextLineHeight(vg, 1.25f);
		setColor(argb, color);
		nvgFillColor(vg, color);
		nvgTextBox(vg, x, y, w, s);
	}

	/** Wrapped text, each line centred on {@code cx}. */
	public static void textWrappedCentered(String s, float cx, float y, float w, float size, int argb, Font font) {
		nvgFontSize(vg, size);
		nvgFontFaceId(vg, fonts.get(font));
		nvgTextLineHeight(vg, 1.15f);
		nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_TOP);
		setColor(argb, color);
		nvgFillColor(vg, color);
		nvgTextBox(vg, cx - w / 2f, y, w, s);
		nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
	}

	/** Height of {@code s} wrapped to width {@code w} at line height 1.15. */
	public static float textWrappedHeightTight(String s, float w, float size, Font font) {
		ensureInit();
		nvgFontSize(vg, size);
		nvgFontFaceId(vg, fonts.get(font));
		nvgTextLineHeight(vg, 1.15f);
		float[] b = new float[4];
		nvgTextBoxBounds(vg, 0f, 0f, w, s, b);
		return b[3] - b[1];
	}

	/** Height of {@code s} wrapped to width {@code w}. */
	public static float textWrappedHeight(String s, float w, float size, Font font) {
		ensureInit();
		nvgFontSize(vg, size);
		nvgFontFaceId(vg, fonts.get(font));
		nvgTextLineHeight(vg, 1.25f);
		float[] b = new float[4];
		nvgTextBoxBounds(vg, 0f, 0f, w, s, b);
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
		setColor(argb, color);
		nvgFillColor(vg, color);
		nvgFill(vg);
	}

	private static void setColor(int argb, NVGColor into) {
		nvgRGBA((byte) (argb >> 16), (byte) (argb >> 8), (byte) argb, (byte) (argb >>> 24), into);
	}

	private record Scissor(Scissor previous, float x, float y, float maxX, float maxY) {
		void apply() {
			if (previous == null) {
				nvgScissor(vg, x, y, maxX - x, maxY - y);
			} else {
				float nx = Math.max(x, previous.x), ny = Math.max(y, previous.y);
				nvgScissor(vg, nx, ny, Math.max(0, Math.min(maxX, previous.maxX) - nx), Math.max(0, Math.min(maxY, previous.maxY) - ny));
			}
		}
	}
}
