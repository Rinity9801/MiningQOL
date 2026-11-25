package forfun.miningqol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import forfun.miningqol.client.utils.render.BlockOverlayRenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/** Configurable replacement for the vanilla targeted-block outline. */
public final class BlockOverlay {
    private static final float[] DEFAULT_COLOR = {0.0f, 134.0f / 255.0f, 1.0f};

    private static boolean registered;
    private static boolean enabled;
    private static Mode mode = Mode.FILLED_OUTLINE;
    private static float[] fillColor = DEFAULT_COLOR.clone();
    private static float fillAlpha = 50.0f / 255.0f;
    private static float[] outlineColor = DEFAULT_COLOR.clone();
    private static float outlineAlpha = 1.0f;
    private static float lineWidth = 2.5f;
    private static boolean phase;
    private static boolean hideDuringEtherwarp;

    private BlockOverlay() {}

    public enum Mode {
        OUTLINE("Outline"),
        FILL("Fill"),
        FILLED_OUTLINE("Filled Outline");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static void init() {
        if (registered) return;
        registered = true;
        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(BlockOverlay::beforeBlockOutline);
    }

    private static boolean beforeBlockOutline(LevelRenderContext context, BlockOutlineRenderState outlineState) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.gui.hud.isHidden()) return true;
        if (hideDuringEtherwarp && shouldHideForEtherwarp(client)) return false;
        if (client.level == null) return true;

        render(context, outlineState, client);
        return false;
    }

    private static void render(LevelRenderContext context, BlockOutlineRenderState outlineState, Minecraft client) {
        BlockPos pos = outlineState.pos();
        BlockState state = client.level.getBlockState(pos);
        VoxelShape shape = state.isAir() ? Shapes.block() : state.getShape(client.level, pos);
        if (shape.isEmpty()) return;

        // Follow the block's real shape: a bent or branching pane (or stairs, walls, bars) is
        // several boxes, and one box around all of them covers empty space. Of each box, draw only
        // the faces turned towards the camera, and only their edges, so the back of the shape
        // never shows through see-through blocks as lines and fill inside the glass.
        double ox = pos.getX(), oy = pos.getY(), oz = pos.getZ();
        List<AABB> boxes = shape.toAabbs();
        Vec3 camera = client.gameRenderer.mainCamera().position();
        double camX = camera.x - ox, camY = camera.y - oy, camZ = camera.z - oz;

        List<float[]> faces = new ArrayList<>();
        List<float[]> edges = new ArrayList<>();
        Set<String> seenEdges = new HashSet<>();
        for (AABB box : boxes) {
            float x0 = (float) (ox + box.minX - PUSH), y0 = (float) (oy + box.minY - PUSH), z0 = (float) (oz + box.minZ - PUSH);
            float x1 = (float) (ox + box.maxX + PUSH), y1 = (float) (oy + box.maxY + PUSH), z1 = (float) (oz + box.maxZ + PUSH);
            if (camX < box.minX && !covered(boxes, box, 0, box.minX - EPS))
                addFace(faces, edges, seenEdges, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            if (camX > box.maxX && !covered(boxes, box, 0, box.maxX + EPS))
                addFace(faces, edges, seenEdges, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
            if (camY < box.minY && !covered(boxes, box, 1, box.minY - EPS))
                addFace(faces, edges, seenEdges, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
            if (camY > box.maxY && !covered(boxes, box, 1, box.maxY + EPS))
                addFace(faces, edges, seenEdges, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
            if (camZ < box.minZ && !covered(boxes, box, 2, box.minZ - EPS))
                addFace(faces, edges, seenEdges, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
            if (camZ > box.maxZ && !covered(boxes, box, 2, box.maxZ + EPS))
                addFace(faces, edges, seenEdges, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        }
        if (faces.isEmpty()) return; // camera inside the block

        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        matrices.translate(-camera.x, -camera.y, -camera.z);

        // 26.2: geometry is submitted and drawn later in the frame, so capture this frame's values.
        SubmitNodeCollector collector = context.submitNodeCollector();
        if (mode == Mode.FILL || mode == Mode.FILLED_OUTLINE) {
            float r = fillColor[0], g = fillColor[1], b = fillColor[2], a = fillAlpha;
            collector.submitCustomGeometry(matrices,
                phase ? BlockOverlayRenderTypes.FILLED_PHASE : BlockOverlayRenderTypes.FILLED,
                (pose, fill) -> {
                    for (float[] f : faces) {
                        quad(fill, pose, f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], r, g, b, a);
                    }
                });
        }

        if (mode == Mode.OUTLINE || mode == Mode.FILLED_OUTLINE) {
            float r = outlineColor[0], g = outlineColor[1], b = outlineColor[2], a = outlineAlpha, width = lineWidth;
            collector.submitCustomGeometry(matrices,
                phase ? BlockOverlayRenderTypes.LINES_PHASE : BlockOverlayRenderTypes.LINES,
                (pose, lines) -> {
                    for (float[] e : edges) line(lines, pose, e[0], e[1], e[2], e[3], e[4], e[5], r, g, b, a, width);
                });
        }

        matrices.popPose();
    }

    /** Pushed slightly out from each box so it doesn't z-fight the block's own faces. */
    private static final double PUSH = 0.002;
    private static final double EPS = 1.0E-4;

    /** A face (corners in order) and its four edges, each edge once even when faces share it. */
    private static void addFace(List<float[]> faces, List<float[]> edges, Set<String> seen,
                                float ax, float ay, float az, float bx, float by, float bz,
                                float cx, float cy, float cz, float dx, float dy, float dz) {
        faces.add(new float[]{ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz});
        addEdge(edges, seen, ax, ay, az, bx, by, bz);
        addEdge(edges, seen, bx, by, bz, cx, cy, cz);
        addEdge(edges, seen, cx, cy, cz, dx, dy, dz);
        addEdge(edges, seen, dx, dy, dz, ax, ay, az);
    }

    private static void addEdge(List<float[]> edges, Set<String> seen,
                                float ax, float ay, float az, float bx, float by, float bz) {
        String a = ax + "," + ay + "," + az, b = bx + "," + by + "," + bz;
        if (seen.add(a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a)) edges.add(new float[]{ax, ay, az, bx, by, bz});
    }

    /**
     * Whether one of {@code self}'s faces is buried against the rest of the shape: its corners and
     * centre, just outside the face (axis 0/1/2 = X/Y/Z, at {@code plane}), are all inside other
     * boxes. A face only partly covered, like a pane's crossbar where the stem joins it, still draws.
     */
    private static boolean covered(List<AABB> shape, AABB self, int axis, double plane) {
        double[][] uv = axis == 0 ? new double[][]{{self.minY, self.maxY}, {self.minZ, self.maxZ}}
            : axis == 1 ? new double[][]{{self.minX, self.maxX}, {self.minZ, self.maxZ}}
            : new double[][]{{self.minX, self.maxX}, {self.minY, self.maxY}};
        double[] us = {uv[0][0] + EPS, uv[0][1] - EPS, (uv[0][0] + uv[0][1]) / 2.0};
        double[] vs = {uv[1][0] + EPS, uv[1][1] - EPS, (uv[1][0] + uv[1][1]) / 2.0};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if ((i == 2) != (j == 2)) continue; // the four corners and the centre
                double u = us[i], v = vs[j];
                double x = axis == 0 ? plane : u, y = axis == 0 ? u : axis == 1 ? plane : v, z = axis == 2 ? plane : v;
                if (axis == 1) { x = u; z = v; }
                if (!insideOther(shape, self, x, y, z)) return false;
            }
        }
        return true;
    }

    private static boolean insideOther(List<AABB> shape, AABB self, double x, double y, double z) {
        for (AABB other : shape) {
            if (other != self && other.contains(x, y, z)) return true;
        }
        return false;
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float red, float green, float blue, float alpha) {
        vertex(buffer, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(buffer, pose, x2, y2, z2, red, green, blue, alpha);
        vertex(buffer, pose, x3, y3, z3, red, green, blue, alpha);
        vertex(buffer, pose, x1, y1, z1, red, green, blue, alpha);
        vertex(buffer, pose, x3, y3, z3, red, green, blue, alpha);
        vertex(buffer, pose, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z,
                               float red, float green, float blue, float alpha) {
        buffer.addVertex(pose, x, y, z).setColor(red, green, blue, alpha);
    }

    private static void line(VertexConsumer buffer, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float red, float green, float blue, float alpha, float width) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (normal.lengthSquared() > 0.0f) normal.normalize();
        buffer.addVertex(pose, x1, y1, z1)
            .setColor(red, green, blue, alpha)
            .setNormal(pose, normal)
            .setLineWidth(width);
        buffer.addVertex(pose, x2, y2, z2)
            .setColor(red, green, blue, alpha)
            .setNormal(pose, normal)
            .setLineWidth(width);
    }

    private static boolean shouldHideForEtherwarp(Minecraft client) {
        if (client.player == null || !client.player.isCrouching()) return false;
        ItemStack stack = client.player.getMainHandItem();
        CustomData customData = stack.getComponents().get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;

        CompoundTag tag = customData.copyTag();
        String id = tag.getString("id").orElse("");
        boolean teleportSword = id.equals("ASPECT_OF_THE_VOID") || id.equals("ASPECT_OF_THE_END");
        return teleportSword && tag.getByte("ethermerge").orElse((byte) 0) == 1;
    }

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static Mode getMode() { return mode; }
    public static void setMode(Mode value) { mode = value == null ? Mode.FILLED_OUTLINE : value; }
    public static void cycleMode() { mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length]; }
    public static float[] getFillColor() { return fillColor.clone(); }
    public static void setFillColor(float red, float green, float blue) { fillColor = color(red, green, blue); }
    public static String getFillHex() { return formatHex(fillColor, fillAlpha); }
    public static boolean setFillHex(String value) {
        int[] rgba = parseHex(value, true);
        if (rgba == null) return false;
        setFillColor(rgba[0] / 255.0f, rgba[1] / 255.0f, rgba[2] / 255.0f);
        if (rgba[3] >= 0) setFillAlpha(rgba[3] / 255.0f);
        return true;
    }
    public static float getFillAlpha() { return fillAlpha; }
    public static void setFillAlpha(float value) { fillAlpha = clamp(value); }
    public static float[] getOutlineColor() { return outlineColor.clone(); }
    public static void setOutlineColor(float red, float green, float blue) { outlineColor = color(red, green, blue); }
    public static String getOutlineHex() { return formatHex(outlineColor, outlineAlpha); }
    public static boolean setOutlineHex(String value) {
        int[] rgb = parseHex(value, true);
        if (rgb == null) return false;
        setOutlineColor(rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f);
        if (rgb[3] >= 0) setOutlineAlpha(rgb[3] / 255.0f);
        return true;
    }
    public static float getOutlineAlpha() { return outlineAlpha; }
    public static void setOutlineAlpha(float value) { outlineAlpha = clamp(value); }
    public static float getLineWidth() { return lineWidth; }
    public static void setLineWidth(float value) { lineWidth = Math.max(1.0f, Math.min(10.0f, value)); }
    public static boolean isPhase() { return phase; }
    public static void setPhase(boolean value) { phase = value; }
    public static boolean isHideDuringEtherwarp() { return hideDuringEtherwarp; }
    public static void setHideDuringEtherwarp(boolean value) { hideDuringEtherwarp = value; }

    private static float[] color(float red, float green, float blue) {
        return new float[]{clamp(red), clamp(green), clamp(blue)};
    }

    private static String formatHex(float[] color, Float alpha) {
        int red = Math.round(clamp(color[0]) * 255.0f);
        int green = Math.round(clamp(color[1]) * 255.0f);
        int blue = Math.round(clamp(color[2]) * 255.0f);
        if (alpha == null) return String.format("#%02X%02X%02X", red, green, blue);
        return String.format("0x%02X%02X%02X%02X", Math.round(clamp(alpha) * 255.0f), red, green, blue);
    }

    private static int[] parseHex(String value, boolean allowAlpha) {
        if (value == null) return null;
        String hex = value.trim();
        boolean argb = hex.startsWith("0x") || hex.startsWith("0X");
        if (hex.startsWith("#")) hex = hex.substring(1);
        else if (argb) hex = hex.substring(2);

        if (hex.length() == 3 || (allowAlpha && hex.length() == 4)) {
            StringBuilder expanded = new StringBuilder(hex.length() * 2);
            for (int i = 0; i < hex.length(); i++) {
                expanded.append(hex.charAt(i)).append(hex.charAt(i));
            }
            hex = expanded.toString();
        }
        if (hex.length() != 6 && (!allowAlpha || hex.length() != 8)) return null;

        try {
            int red = Integer.parseInt(hex.substring(argb && hex.length() == 8 ? 2 : 0,
                argb && hex.length() == 8 ? 4 : 2), 16);
            int green = Integer.parseInt(hex.substring(argb && hex.length() == 8 ? 4 : 2,
                argb && hex.length() == 8 ? 6 : 4), 16);
            int blue = Integer.parseInt(hex.substring(argb && hex.length() == 8 ? 6 : 4,
                argb && hex.length() == 8 ? 8 : 6), 16);
            int alpha = hex.length() == 8
                ? Integer.parseInt(hex.substring(argb ? 0 : 6, argb ? 2 : 8), 16)
                : -1;
            return new int[]{red, green, blue, alpha};
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
