package forfun.miningqol.mixin.client;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesInvoker {
    @Accessor("DEBUG_FILLED_SNIPPET")
    static RenderPipeline.Snippet miningqol$getDebugFilledSnippet() {
        throw new AssertionError();
    }

    @Accessor("LINES_SNIPPET")
    static RenderPipeline.Snippet miningqol$getLinesSnippet() {
        throw new AssertionError();
    }

    // 26.3: order-independent transparency ("Improved Transparency") draws blended world geometry
    // with per-stage OIT pipelines built on these snippets.
    @Accessor("OIT_DEBUG_FILLED_SNIPPET")
    static RenderPipeline.Snippet miningqol$getOitDebugFilledSnippet() {
        throw new AssertionError();
    }

    @Accessor("OIT_LINES_SNIPPET")
    static RenderPipeline.Snippet miningqol$getOitLinesSnippet() {
        throw new AssertionError();
    }

    @Invoker("register")
    static RenderPipeline miningqol$register(RenderPipeline pipeline) {
        throw new AssertionError();
    }
}
