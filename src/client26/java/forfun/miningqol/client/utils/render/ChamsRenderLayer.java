package forfun.miningqol.client.utils.render;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.VertexFormat;
import forfun.miningqol.mixin.client.RenderTypeInvoker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Prisma-style second entity pass that draws only model fragments hidden behind scene depth. */
public final class ChamsRenderLayer {
    private static final Map<RenderType, Identifier> LAYER_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<RenderType, RenderType> HIDDEN_LAYERS = new ConcurrentHashMap<>();
    private static RenderPipeline hiddenPipeline;
    private static java.util.UUID headChamsUuid;

    private ChamsRenderLayer() {}

    public static void beginHeadChams(java.util.UUID uuid) {
        headChamsUuid = uuid;
    }

    public static void endHeadChams() {
        headChamsUuid = null;
    }

    public static java.util.UUID headChamsUuid() {
        return headChamsUuid;
    }

    public static void noteTexture(RenderType layer, Identifier texture) {
        if (layer != null && texture != null) LAYER_TEXTURES.putIfAbsent(layer, texture);
    }

    public static RenderType hiddenFor(RenderType source) {
        Identifier texture = LAYER_TEXTURES.get(source);
        if (texture == null) return null;
        return HIDDEN_LAYERS.computeIfAbsent(source, ignored -> RenderTypeInvoker.miningqol$create(
            "miningqol_chams_hidden",
            RenderSetup.builder(pipeline())
                .withTexture("Sampler0", texture)
                .useOverlay()
                .sortOnUpload()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup()));
    }

    private static RenderPipeline pipeline() {
        if (hiddenPipeline != null) return hiddenPipeline;

        RenderPipeline source = RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE;
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("miningqol", "pipeline/chams_hidden"))
            .withVertexShader(source.getVertexShader())
            .withFragmentShader(source.getFragmentShader())
            .withPrimitiveTopology(source.getPrimitiveTopology())
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN, false))
            .withPolygonMode(source.getPolygonMode())
            .withCull(source.isCull());

        VertexFormat[] bindings = source.getVertexFormatBindings();
        for (int i = 0; i < bindings.length; i++) {
            if (bindings[i] != null) builder.withVertexBinding(i, bindings[i]);
        }
        ColorTargetState[] targets = source.getColorTargetStates();
        for (int i = 0; i < targets.length; i++) {
            if (targets[i] != null) builder.withColorTargetState(i, targets[i]);
            else builder.withUnusedColorTargetState(i);
        }
        // 26.2 groups samplers and uniforms into bind group layouts.
        source.getBindGroupLayouts().forEach(builder::withBindGroupLayout);
        source.getShaderDefines().values().forEach((key, value) -> {
            try {
                builder.withShaderDefine(key, Integer.parseInt(value));
            } catch (NumberFormatException first) {
                try {
                    builder.withShaderDefine(key, Float.parseFloat(value));
                } catch (NumberFormatException ignored) {
                }
            }
        });
        source.getShaderDefines().flags().forEach(builder::withShaderDefine);
        hiddenPipeline = builder.build();
        return hiddenPipeline;
    }
}
