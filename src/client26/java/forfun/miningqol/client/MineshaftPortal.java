package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import forfun.miningqol.client.utils.render.SeeThroughBoxRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Highlights the Glacite Mineshaft portal you just found: its hitbox through walls, and a line
 * from the crosshair to it. Ported from definitely-legit.
 *
 * <p>Hypixel announces the find in chat ("WOW! You found a Glacite Mineshaft portal!" and
 * "MINESHAFT! A Mineshaft portal spawned nearby!"). The portal itself is an invisible armour stand
 * named "[RANK] name's Mineshaft Portal", which may arrive a moment after the message, so the
 * search keeps running for a few seconds. The stand is then followed until it despawns.
 */
public final class MineshaftPortal {
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final String PORTAL_SUFFIX = "'s mineshaft portal";
    private static final long SEARCH_MS = 10_000;
    /** Other players' portals are only a fallback, and only this close — "spawned nearby". */
    private static final double FALLBACK_RANGE = 32.0;

    private static boolean enabled = true;
    private static boolean tracer = true;
    private static final float[] color = {170f / 255f, 1.0f, 85f / 255f};
    private static float alpha = 1.0f;
    private static float lineWidth = 1.5f;

    private static long searchUntil;
    private static int portalId = -1;

    private MineshaftPortal() {}

    public static void onChatMessage(String message) {
        String clean = FORMATTING.matcher(message).replaceAll("").trim();
        if (clean.startsWith("WOW! You found a Glacite Mineshaft portal!")
            || clean.startsWith("MINESHAFT! A Mineshaft portal spawned nearby!")) {
            searchUntil = System.currentTimeMillis() + SEARCH_MS;
            portalId = -1;
        }
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.level == null || client.player == null) return;

        if (portalId != -1 && client.level.getEntity(portalId) == null) portalId = -1;
        // Finding your own portal closes the window; a fallback portal leaves it open.
        if (System.currentTimeMillis() > searchUntil) return;

        String own = client.player.getName().getString().toLowerCase(Locale.ROOT);
        List<ArmorStand> portals = client.level.getEntitiesOfClass(ArmorStand.class,
            client.player.getBoundingBox().inflate(128.0), MineshaftPortal::isPortal);

        ArmorStand best = null;
        boolean bestOwn = false;
        double bestDistance = Double.MAX_VALUE;
        for (ArmorStand stand : portals) {
            boolean mine = portalOwner(stand).equals(own);
            double distance = stand.distanceTo(client.player);
            if (!mine && distance > FALLBACK_RANGE) continue;
            // Your own portal beats a closer one of someone else's.
            if (best == null || (mine && !bestOwn) || (mine == bestOwn && distance < bestDistance)) {
                best = stand;
                bestOwn = mine;
                bestDistance = distance;
            }
        }
        if (best != null && bestOwn) {
            portalId = best.getId();
            searchUntil = 0;
        } else if (best != null) {
            // Keep looking for our own until the window closes, but show this one meanwhile.
            portalId = best.getId();
        }
    }

    public static void clear() {
        searchUntil = 0;
        portalId = -1;
    }

    /** Whether an entity is a Glacite Mineshaft portal ("[RANK] name's Mineshaft Portal"). */
    public static boolean isPortalEntity(Entity entity) {
        return entity instanceof ArmorStand stand && isPortal(stand);
    }

    private static boolean isPortal(ArmorStand stand) {
        if (!stand.hasCustomName()) return false;
        String name = FORMATTING.matcher(stand.getCustomName().getString()).replaceAll("")
            .trim().toLowerCase(Locale.ROOT);
        return name.endsWith(PORTAL_SUFFIX);
    }

    /** "[VIP] Name's Mineshaft Portal" → "name". */
    private static String portalOwner(ArmorStand stand) {
        String name = FORMATTING.matcher(stand.getCustomName().getString()).replaceAll("")
            .trim().toLowerCase(Locale.ROOT);
        name = name.substring(0, name.length() - PORTAL_SUFFIX.length()).trim();
        int space = name.lastIndexOf(' ');
        return space >= 0 ? name.substring(space + 1) : name;
    }

    public static void render(CameraRenderState cameraState, Matrix4fc viewMatrix) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || portalId == -1 || client.level == null || client.options.hideGui) return;
        Entity portal = client.level.getEntity(portalId);
        if (portal == null) return;

        Vec3 cam = cameraState.pos;
        Matrix4f pose = new Matrix4f(viewMatrix);
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        VertexConsumer quads = buffers.getBuffer(RenderTypes.textBackgroundSeeThrough());

        AABB box = portal.getBoundingBox();
        SeeThroughBoxRenderer.outline(quads, pose,
            (float) (box.minX - cam.x), (float) (box.minY - cam.y), (float) (box.minZ - cam.z),
            (float) (box.maxX - cam.x), (float) (box.maxY - cam.y), (float) (box.maxZ - cam.z),
            color[0], color[1], color[2], alpha, lineWidth);

        if (tracer) {
            // One block ahead of the eye along the view direction reads as "from the crosshair".
            Vector3f forward = new Vector3f(0, 0, -1);
            cameraState.orientation.transform(forward);
            Vec3 target = box.getCenter();
            SeeThroughBoxRenderer.beam(quads, pose, forward.x, forward.y, forward.z,
                (float) (target.x - cam.x), (float) (target.y - cam.y), (float) (target.z - cam.z),
                color[0], color[1], color[2], alpha, lineWidth);
        }
        buffers.endBatch();
    }

    // ---- settings ----

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isTracer() { return tracer; }
    public static void setTracer(boolean value) { tracer = value; }
    public static float[] getColor() { return color.clone(); }
    public static void setColor(float r, float g, float b) { color[0] = r; color[1] = g; color[2] = b; }
    public static float getAlpha() { return alpha; }
    public static void setAlpha(float value) { alpha = Math.max(0f, Math.min(1f, value)); }
    public static float getLineWidth() { return lineWidth; }
    public static void setLineWidth(float value) { lineWidth = Math.max(0.5f, Math.min(5f, value)); }
}
