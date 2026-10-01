package forfun.miningqol.client.shatter;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.lwjgl.opengl.GL33C;

/**
 * Lets NanoVG draw into Minecraft's GUI: the GUI renderer hands us an off-screen colour+depth texture,
 * we bind it as a GL framebuffer, run NanoVG on it, and the renderer blits the result into the screen
 * like any other GUI element. Ported from Odin's NVGPIPRenderer (BSD-3-Clause, © odtheking).
 */
public class NVGPipRenderer extends PictureInPictureRenderer<NVGPipRenderer.State> {
	private final xyz.meowing.vexel.api.nvg.GlStateSnapshot glState = new xyz.meowing.vexel.api.nvg.GlStateSnapshot();

	public NVGPipRenderer(MultiBufferSource.BufferSource bufferSource) {
		super(bufferSource);
	}

	@Override
	protected void renderToTexture(State state, PoseStack poseStack) {
		GpuTextureView colorView = RenderSystem.outputColorTextureOverride;
		GpuTextureView depthView = RenderSystem.outputDepthTextureOverride;
		if (colorView == null || depthView == null) return;
		if (!(RenderSystem.getDevice().backend instanceof GlDevice glDevice)) return;
		if (!(colorView.texture() instanceof GlTexture colorTex) || !(depthView.texture() instanceof GlTexture depthTex)) return;

		DirectStateAccess dsa = glDevice.directStateAccess();
		int width = colorView.getWidth(0), height = colorView.getHeight(0);
		int fbo = colorTex.getFbo(dsa, depthTex);
		GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, fbo);
		GlStateManager._viewport(0, 0, width, height);

		glState.capture();
		GL33C.glBindSampler(0, 0);
		NVG.beginFrame(width, height);
		try {
			state.content().run();
		} finally {
			NVG.endFrame();
			// NanoVG leaves its own texture/program/blend state bound behind GlStateManager's back.
			// Put the real GL state back (not through GlStateManager, whose cache would skip it),
			// or the next text draw samples the wrong texture and characters go missing.
			glState.restore();
		}
	}

	@Override
	protected float getTranslateY(int height, int guiScale) {
		return height / 2f;
	}

	@Override
	public Class<State> getRenderStateClass() {
		return State.class;
	}

	@Override
	protected String getTextureLabel() {
		return "miningqol_nvg";
	}

	public record State(int x, int y, int width, int height, Matrix3x2f poseMatrix, ScreenRectangle scissor,
	                    ScreenRectangle boundsRect, Runnable content) implements PictureInPictureRenderState {
		@Override public int x0() { return x; }
		@Override public int y0() { return y; }
		@Override public int x1() { return x + width; }
		@Override public int y1() { return y + height; }
		@Override public float scale() { return 1f; }
		@Override public Matrix3x2f pose() { return poseMatrix; }
		@Override public ScreenRectangle scissorArea() { return scissor; }
		@Override public ScreenRectangle bounds() { return boundsRect; }
	}

	/**
	 * Queue NanoVG drawing that covers the GUI rectangle (x, y, w, h) in GUI units. {@code content} runs
	 * later, during rendering, with a NanoVG frame open in logical window pixels.
	 */
	public static void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, Runnable content) {
		ScreenRectangle scissor = g.scissorStack.peek();
		Matrix3x2f pose = new Matrix3x2f(g.pose());
		ScreenRectangle rect = new ScreenRectangle(x, y, w, h).transformMaxBounds(pose);
		ScreenRectangle bounds = scissor != null ? scissor.intersection(rect) : rect;
		g.guiRenderState.addPicturesInPictureState(new State(x, y, w, h, pose, scissor, bounds, content));
	}

	/** Full-screen convenience. */
	public static void drawFullScreen(GuiGraphicsExtractor g, Runnable content) {
		draw(g, 0, 0, g.guiWidth(), g.guiHeight(), content);
	}
}
