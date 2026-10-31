package forfun.miningqol.client.utils.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/** Premultiplied outline composite used by the Prisma-style custom halo. */
public final class OutlineHaloBlit {
    // Same shape as vanilla's ENTITY_OUTLINE_BLIT, with a premultiplied blend instead.
    private static final RenderPipeline OUTLINE_HALO_BLIT = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("miningqol", "pipeline/outline_halo_blit"))
        .withVertexShader("core/screenquad")
        .withFragmentShader("core/blit_screen")
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
        .withColorTargetState(new ColorTargetState(
            Optional.of(new BlendFunction(
                BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA,
                BlendFactor.ZERO, BlendFactor.ONE)),
            GpuFormat.RGBA8_UNORM,
            ColorTargetState.WRITE_COLOR))
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .build();

    private OutlineHaloBlit() {}

    public static void blit(RenderTarget source, GpuTextureView target) {
        RenderSystem.assertOnRenderThread();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass pass = encoder.createRenderPass(
            () -> "MiningQOL outline halo blit", target, Optional.empty())) {
            pass.setPipeline(OUTLINE_HALO_BLIT);
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindTexture(
                "InSampler",
                source.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(3, 1, 0, 0);
        }
    }
}
