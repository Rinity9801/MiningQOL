package forfun.miningqol.client;

import forfun.miningqol.client.utils.render.WorldBuffers;
import forfun.miningqol.client.waypoints.OrderedWaypointRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Runs the mod's world renderers once per level frame. 26.2 removed LevelRenderer.renderLevel
 * (where these used to run at TAIL) and draws the world from submitted features, so they now run
 * while the level collects submits and draw through {@link WorldBuffers}.
 */
public final class WorldRenderHook {
    private static boolean registered;

    private WorldRenderHook() {}

    public static void init() {
        if (registered) return;
        registered = true;
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            CameraRenderState cameraState = context.levelState().cameraRenderState;
            WorldBuffers.beginFrame(context.submitNodeCollector());
            try {
                OrderedWaypointRenderer.render(cameraState);
                ShaftESP.render(cameraState);
                CorpseESP.render(cameraState);
                EfficientMinerOverlay.render(cameraState);
                WispRadius.render(cameraState);
                MineshaftPortal.render(cameraState);
            } finally {
                WorldBuffers.endFrame();
            }
            // No-op unless the local-only ESP feed module is compiled in.
            EspHooks.render(cameraState);
            if (CheatHooks.onRenderFrame != null) {
                CheatHooks.onRenderFrame.run();
            }
        });
    }
}
