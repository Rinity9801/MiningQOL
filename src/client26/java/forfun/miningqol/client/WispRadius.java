package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Highlights every block you can stand on inside a Will-o'-wisp's 30-block radius.
 *
 * <p>The wisp is found by its name-tag armor stand ("Will-o'-wisp 274s"). A block counts
 * when it is solid, has two clear blocks above it for the player to fit in, and the spot
 * the player would occupy on it (feet at the block's top, centre of the column) lies
 * within the radius of the wisp. Top faces are painted edge to edge, and where two
 * neighbouring spots sit at different heights (snow layers) the step between them is
 * painted too, so the highlight reads as one connected surface.
 */
public final class WispRadius {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final double STAND_SEARCH_RADIUS = 96.0;
    /** How far in from the edge "edge only" mode paints. */
    private static final double EDGE_BAND = 2.0;

    private static boolean enabled = true;
    private static boolean edgeOnly = false;
    /** Only paint the mineshaft floor: snow, snow layers, and smooth stone. */
    private static boolean floorBlocksOnly = true;
    private static final String[] FLOOR_BLOCK_IDS = {"snow", "smooth_stone"};
    private static float radius = 30f;
    private static float alpha = 0.35f;
    private static final float[] color = {0.45f, 0.85f, 1.0f};

    private record Wisp(Vec3 pos, int secondsLeft) {}
    /** A standable column; {@code top} is the real surface height, snow layers included. */
    private record Spot(int x, int y, int z, double top) {}
    /** A vertical strip closing the step between two spots, on the line (x0,z0)-(x1,z1). */
    private record Step(double x0, double z0, double x1, double z1, double low, double high) {}
    /** Biggest height difference that still gets a connecting step: one block. */
    private static final double MAX_STEP = 1.01;
    /** Pushes step strips off the block face they sit on, so they don't z-fight it. */
    private static final double STEP_NUDGE = 0.01;

    private static final List<Wisp> wisps = new ArrayList<>();
    private static final List<Spot> spots = new ArrayList<>();
    private static final List<Step> steps = new ArrayList<>();
    private static int tickCounter;
    private static ClientLevel lastLevel;

    private WispRadius() {}

    public static void tick() {
        if (!enabled) {
            if (!spots.isEmpty() || !steps.isEmpty()) clear();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            clear();
            return;
        }
        if (lastLevel != client.level) {
            clear();
            lastLevel = client.level;
        }
        if (++tickCounter % SCAN_INTERVAL_TICKS != 0) return;
        // Off inside Glacite Mineshafts: checked on the scan cadence, not every tick.
        if (forfun.miningqol.client.party.MineshaftAutoParty.isInMineshaft()) {
            clear();
            return;
        }
        scan(client);
    }

    private static void clear() {
        wisps.clear();
        spots.clear();
        steps.clear();
    }

    private static void scan(Minecraft client) {
        ClientLevel level = client.level;
        Vec3 me = client.player.position();
        AABB box = new AABB(me.x - STAND_SEARCH_RADIUS, me.y - STAND_SEARCH_RADIUS, me.z - STAND_SEARCH_RADIUS,
            me.x + STAND_SEARCH_RADIUS, me.y + STAND_SEARCH_RADIUS, me.z + STAND_SEARCH_RADIUS);
        List<ArmorStand> stands = level.getEntitiesOfClass(ArmorStand.class, box, ArmorStand::hasCustomName);

        wisps.clear();
        for (ArmorStand stand : stands) {
            String name = stand.getCustomName().getString().replaceAll("§.", "").trim();
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.contains("will-o'-wisp") && !lower.contains("will-o-wisp") && !lower.contains("will o' wisp")) continue;
            int seconds = -1;
            int idx = lower.lastIndexOf('s');
            if (idx > 0) {
                int start = idx - 1;
                while (start >= 0 && Character.isDigit(lower.charAt(start))) start--;
                if (start < idx - 1) {
                    try {
                        seconds = Integer.parseInt(lower.substring(start + 1, idx));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            wisps.add(new Wisp(stand.position(), seconds));
        }

        spots.clear();
        steps.clear();
        if (wisps.isEmpty()) return;
        double r = radius;
        double rSq = r * r;
        double innerSq = Math.max(0, r - EDGE_BAND) * Math.max(0, r - EDGE_BAND);
        int reach = (int) Math.ceil(r);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        java.util.HashSet<Long> seen = new java.util.HashSet<>();

        for (Wisp wisp : wisps) {
            int cx = (int) Math.floor(wisp.pos().x);
            int cy = (int) Math.floor(wisp.pos().y);
            int cz = (int) Math.floor(wisp.pos().z);
            for (int x = cx - reach; x <= cx + reach; x++) {
                for (int z = cz - reach; z <= cz + reach; z++) {
                    double dx = x + 0.5 - wisp.pos().x;
                    double dz = z + 0.5 - wisp.pos().z;
                    double horizSq = dx * dx + dz * dz;
                    if (horizSq > rSq) continue;
                    for (int y = cy - reach; y <= cy + reach; y++) {
                        double top = surfaceTop(level, pos, x, y, z);
                        if (Double.isNaN(top)) continue;
                        // Feet would be on the surface.
                        double dy = top - wisp.pos().y;
                        double distSq = horizSq + dy * dy;
                        if (distSq > rSq) continue;
                        if (edgeOnly && distSq < innerSq) continue;
                        long key = BlockPos.asLong(x, y, z);
                        if (seen.add(key)) spots.add(new Spot(x, y, z, top));
                    }
                }
            }
        }
        buildSteps();
    }

    /**
     * For each spot, a strip down to every lower neighbouring spot (within one block), so
     * snow layers of different depths join up instead of floating as separate tiles. Only
     * the higher side emits the strip, so each step is drawn once.
     */
    private static void buildSteps() {
        java.util.Map<Long, List<Spot>> columns = new java.util.HashMap<>();
        for (Spot spot : spots) {
            columns.computeIfAbsent(columnKey(spot.x(), spot.z()), k -> new ArrayList<>()).add(spot);
        }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (Spot spot : spots) {
            for (int[] d : dirs) {
                List<Spot> column = columns.get(columnKey(spot.x() + d[0], spot.z() + d[1]));
                if (column == null) continue;
                // The neighbour most likely to be the same floor: the closest one below.
                Spot best = null;
                for (Spot n : column) {
                    double drop = spot.top() - n.top();
                    if (drop <= 0.001 || drop > MAX_STEP) continue;
                    if (best == null || n.top() > best.top()) best = n;
                }
                if (best == null) continue;
                double x0, z0, x1, z1;
                if (d[0] != 0) {
                    double x = d[0] > 0 ? spot.x() + 1 + STEP_NUDGE : spot.x() - STEP_NUDGE;
                    x0 = x1 = x;
                    z0 = spot.z();
                    z1 = spot.z() + 1;
                } else {
                    double z = d[1] > 0 ? spot.z() + 1 + STEP_NUDGE : spot.z() - STEP_NUDGE;
                    z0 = z1 = z;
                    x0 = spot.x();
                    x1 = spot.x() + 1;
                }
                steps.add(new Step(x0, z0, x1, z1, best.top(), spot.top()));
            }
        }
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /**
     * The height the player's feet rest at when standing on this block, or NaN when it
     * cannot be stood on. The floor's collision top is used rather than the full block,
     * so a thick snow layer reads at its real height; a thin snow layer or carpet above
     * the floor has no collision but still raises the surface by its visual height.
     */
    private static double surfaceTop(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        pos.set(x, y, z);
        BlockState floor = level.getBlockState(pos);
        if (floor.isAir()) return Double.NaN;
        VoxelShape floorShape = floor.getCollisionShape(level, pos);
        if (floorShape.isEmpty()) return Double.NaN;
        // Snow layers collide one layer below their visible top; paint on what is seen.
        double top = y + floorShape.max(Direction.Axis.Y);
        VoxelShape floorOutline = floor.getShape(level, pos);
        if (!floorOutline.isEmpty()) top = Math.max(top, y + Math.min(1.0, floorOutline.max(Direction.Axis.Y)));
        BlockState surface = floor;

        pos.set(x, y + 1, z);
        BlockState above = level.getBlockState(pos);
        VoxelShape aboveCollision = above.getCollisionShape(level, pos);
        if (!aboveCollision.isEmpty()) return Double.NaN;
        if (!above.isAir()) {
            VoxelShape outline = above.getShape(level, pos);
            if (!outline.isEmpty()) {
                // A thin layer (snow, carpet) is the surface you actually stand on.
                top = Math.max(top, y + 1 + outline.max(Direction.Axis.Y));
                surface = above;
            }
        }
        if (floorBlocksOnly && !isFloorBlock(surface)) return Double.NaN;
        pos.set(x, y + 2, z);
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return Double.NaN;
        return top;
    }

    private static boolean isFloorBlock(BlockState state) {
        String id = state.getBlock().getDescriptionId().toLowerCase(Locale.ROOT);
        for (String floor : FLOOR_BLOCK_IDS) {
            if (id.contains(floor)) return true;
        }
        return false;
    }

    public static void render(CameraRenderState cameraState, Matrix4fc viewMatrix) {
        if (!enabled || spots.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        Vec3 cam = cameraState.pos;
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        Matrix4f pose = new Matrix4f(viewMatrix);
        VertexConsumer quads = buffers.getBuffer(RenderTypes.textBackground());
        int r = (int) (color[0] * 255);
        int g = (int) (color[1] * 255);
        int b = (int) (color[2] * 255);
        int a = (int) (alpha * 255);

        for (Spot spot : spots) {
            float minX = (float) (spot.x() - cam.x);
            float maxX = (float) (spot.x() + 1 - cam.x);
            float minZ = (float) (spot.z() - cam.z);
            float maxZ = (float) (spot.z() + 1 - cam.z);
            float top = (float) (spot.top() + 0.01 - cam.y);
            // Both windings so the face shows from above and below.
            quads.addVertex(pose, minX, top, minZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, minX, top, maxZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, maxX, top, maxZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, maxX, top, minZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, maxX, top, minZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, maxX, top, maxZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, minX, top, maxZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, minX, top, minZ).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        }
        for (Step step : steps) {
            float x0 = (float) (step.x0() - cam.x), z0 = (float) (step.z0() - cam.z);
            float x1 = (float) (step.x1() - cam.x), z1 = (float) (step.z1() - cam.z);
            // Same +0.01 lift as the top faces, so the strip meets both tiles exactly.
            float low = (float) (step.low() + 0.01 - cam.y);
            float high = (float) (step.high() + 0.01 - cam.y);
            quads.addVertex(pose, x0, low, z0).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, x0, high, z0).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, x1, high, z1).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, x1, low, z1).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, x1, low, z1).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, x1, high, z1).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, x0, high, z0).setColor(r, g, b, a).setLight(FULL_BRIGHT);
            quads.addVertex(pose, x0, low, z0).setColor(r, g, b, a).setLight(FULL_BRIGHT);
        }
        buffers.endBatch();
    }

    /** Whether the player currently stands inside any wisp's radius. */
    public static boolean isPlayerInRange() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        Vec3 me = client.player.position();
        for (Wisp wisp : wisps) {
            if (me.distanceTo(wisp.pos()) <= radius) return true;
        }
        return false;
    }

    public static int activeWisps() {
        return wisps.size();
    }

    public static int highlightedBlocks() {
        return spots.size();
    }

    // ---- settings ----

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isFloorBlocksOnly() { return floorBlocksOnly; }
    public static void setFloorBlocksOnly(boolean value) { floorBlocksOnly = value; spots.clear(); steps.clear(); tickCounter = SCAN_INTERVAL_TICKS - 1; }
    public static boolean isEdgeOnly() { return edgeOnly; }
    public static void setEdgeOnly(boolean value) { edgeOnly = value; spots.clear(); steps.clear(); tickCounter = SCAN_INTERVAL_TICKS - 1; }
    public static float getRadius() { return radius; }
    public static void setRadius(float value) { radius = Math.max(1f, Math.min(64f, value)); spots.clear(); steps.clear(); tickCounter = SCAN_INTERVAL_TICKS - 1; }
    public static float getAlpha() { return alpha; }
    public static void setAlpha(float value) { alpha = Math.max(0.05f, Math.min(1f, value)); }
    public static float[] getColor() { return color; }
    public static void setColor(float r, float g, float b) { color[0] = r; color[1] = g; color[2] = b; }
}
