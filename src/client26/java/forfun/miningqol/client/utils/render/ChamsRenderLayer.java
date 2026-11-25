package forfun.miningqol.client.utils.render;

import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import forfun.miningqol.mixin.client.RenderTypeInvoker;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.List;
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
                // 26.3: keep the hidden pass out of the OIT passes ("Improved Transparency"): it
                // relies on its own LESS_THAN depth test, which the OIT pipelines can't express.
                .withForcedSolidModelPhase()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup()));
    }

    private static RenderPipeline pipeline() {
        if (hiddenPipeline != null) return hiddenPipeline;

        RenderPipeline source = RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE;
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("miningqol", "pipeline/chams_hidden"))
            .withPrimitiveTopology(source.getPrimitiveTopology())
            // Reversed depth since 26.2 (clears to 0, nearer is greater): a fragment behind the
            // scene has a SMALLER depth than what's stored, so the hidden-only pass is LESS_THAN.
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, false))
            .withPolygonMode(source.getPolygonMode())
            .withCull(source.isCull())
            .withPushConstantSize(source.pushConstantSize());
        // 26.3: shaders are a ShaderType -> location map; bindings and targets are lists.
        source.getShaders().forEach((type, shader) -> {
            if (type == ShaderType.VERTEX) builder.withVertexShader(shader);
            else if (type == ShaderType.FRAGMENT) builder.withFragmentShader(shader);
        });

        List<VertexFormat> bindings = source.getVertexFormatBindings();
        for (int i = 0; i < bindings.size(); i++) {
            if (bindings.get(i) != null) builder.withVertexBinding(i, bindings.get(i));
        }
        List<ColorTargetState> targets = source.getColorTargetStates();
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i) != null) builder.withColorTargetState(i, targets.get(i));
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
