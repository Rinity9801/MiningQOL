package forfun.miningqol.client.utils.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import forfun.miningqol.mixin.client.RenderPipelinesInvoker;
import forfun.miningqol.mixin.client.RenderTypeInvoker;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/** Render layers used by MiningQOL's targeted block overlay. */
public final class BlockOverlayRenderTypes {
    private static final RenderPipeline FILLED_PIPELINE = registerFilled("block_overlay_filled", false);
    private static final RenderPipeline FILLED_PHASE_PIPELINE = registerFilled("block_overlay_filled_phase", true);
    private static final RenderPipeline LINES_PIPELINE = registerLines("block_overlay_lines", false);
    private static final RenderPipeline LINES_PHASE_PIPELINE = registerLines("block_overlay_lines_phase", true);

    // 26.3 "Improved Transparency" routes every blended world render type through OIT passes, which
    // need an OIT pipeline set (else: "does not have OIT pipelines set up"). Phase/see-through
    // variants keep drawing through walls there via OitPipelineSet.withoutDepthTest().
    public static final RenderType FILLED = create("miningqol_block_overlay_filled", FILLED_PIPELINE,
        oitFilled("block_overlay_filled", PrimitiveTopology.TRIANGLES, false));
    public static final RenderType FILLED_PHASE = create("miningqol_block_overlay_filled_phase", FILLED_PHASE_PIPELINE,
        oitFilled("block_overlay_filled_phase", PrimitiveTopology.TRIANGLES, true));
    public static final RenderType LINES = create("miningqol_block_overlay_lines", LINES_PIPELINE,
        oitLines("block_overlay_lines", false));
    public static final RenderType LINES_PHASE = create("miningqol_block_overlay_lines_phase", LINES_PHASE_PIPELINE,
        oitLines("block_overlay_lines_phase", true));

    // 26.3 removed RenderTypes.textBackground()/textBackgroundSeeThrough() (and their shader). These
    // are the same colored-quad layers on the vanilla position_color shader: vertices are
    // POSITION_COLOR, so the callers' setLight(...) calls are ignored, as full-bright as before.
    private static final RenderPipeline QUADS_PIPELINE = registerQuads("colored_quads", false);
    private static final RenderPipeline QUADS_SEE_THROUGH_PIPELINE = registerQuads("colored_quads_see_through", true);
    /** Depth-tested translucent colored quads (was RenderTypes.textBackground()). */
    public static final RenderType QUADS = createSorted("miningqol_colored_quads", QUADS_PIPELINE,
        oitFilled("colored_quads", PrimitiveTopology.QUADS, false));
    /** Translucent colored quads drawn through walls (was RenderTypes.textBackgroundSeeThrough()). */
    public static final RenderType QUADS_SEE_THROUGH = createSorted("miningqol_colored_quads_see_through", QUADS_SEE_THROUGH_PIPELINE,
        oitFilled("colored_quads_see_through", PrimitiveTopology.QUADS, true));

    private BlockOverlayRenderTypes() {}

    private static RenderPipeline registerFilled(String path, boolean phase) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesInvoker.miningqol$getDebugFilledSnippet())
            .withLocation(id(path))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
        if (phase) builder.withDepthStencilState(Optional.empty());
        return RenderPipelinesInvoker.miningqol$register(builder.build());
    }

    private static RenderPipeline registerLines(String path, boolean phase) {
        // 26.3's LINES_SNIPPET no longer carries a colour target (26.2's was TRANSLUCENT). Without one
        // the pipeline has 0 colour targets and binding it in the 1-attachment main pass throws
        // "Render pass color attachment count must match pipeline color target state count".
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesInvoker.miningqol$getLinesSnippet())
            .withLocation(id(path))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        if (phase) builder.withDepthStencilState(Optional.empty());
        return RenderPipelinesInvoker.miningqol$register(builder.build());
    }

    private static RenderPipeline registerQuads(String path, boolean seeThrough) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelinesInvoker.miningqol$getDebugFilledSnippet())
            .withLocation(id(path));
        if (seeThrough) builder.withDepthStencilState(Optional.empty());
        return RenderPipelinesInvoker.miningqol$register(builder.build());
    }

    private static OitPipelineSet oitFilled(String name, PrimitiveTopology topology, boolean seeThrough) {
        OitPipelineSet.Builder builder = OitPipelineSet.builder("miningqol_" + name,
            RenderPipeline.builder(RenderPipelinesInvoker.miningqol$getOitDebugFilledSnippet()).withPrimitiveTopology(topology));
        if (seeThrough) builder.withoutDepthTest();
        return builder.build();
    }

    private static OitPipelineSet oitLines(String name, boolean seeThrough) {
        OitPipelineSet.Builder builder = OitPipelineSet.builder("miningqol_" + name,
            RenderPipeline.builder(RenderPipelinesInvoker.miningqol$getOitLinesSnippet()));
        if (seeThrough) builder.withoutDepthTest();
        return builder.build();
    }

    private static RenderType createSorted(String name, RenderPipeline pipeline, OitPipelineSet oit) {
        return RenderTypeInvoker.miningqol$create(
            name,
            RenderSetup.builder(pipeline).setOitPipelines(oit).sortOnUpload().createRenderSetup());
    }

    private static RenderType create(String name, RenderPipeline pipeline, OitPipelineSet oit) {
        return RenderTypeInvoker.miningqol$create(
            name,
            RenderSetup.builder(pipeline).setOitPipelines(oit).createRenderSetup());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("miningqol", "pipeline/" + path);
    }
}
