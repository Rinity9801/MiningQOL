package forfun.miningqol.client.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stand-in for the {@code MultiBufferSource.BufferSource} that 26.2 removed. World renderers keep
 * their immediate-mode shape ({@code getBuffer(type)}, vertices, {@code endBatch()}), but the
 * vertices are recorded per render type and handed to the level's {@link SubmitNodeCollector} on
 * {@code endBatch()}, which draws them in this frame's feature pass.
 *
 * <p>Vertices are camera-relative and must NOT be multiplied by the view matrix: the feature pass
 * draws with the camera rotation already on the model-view matrix.
 */
public final class WorldBuffers {
    private static final PoseStack IDENTITY = new PoseStack();
    private static SubmitNodeCollector collector;

    private final Map<RenderType, Recorder> recorders = new LinkedHashMap<>();

    private WorldBuffers() {}

    /** Called by the level-render hook around the mod's world renderers. */
    public static void beginFrame(SubmitNodeCollector frameCollector) {
        collector = frameCollector;
    }

    public static void endFrame() {
        collector = null;
    }

    /** A fresh batch for this frame's submissions. */
    public static WorldBuffers get() {
        return new WorldBuffers();
    }

    /** This frame's collector, for submits that aren't vertices (text). Null outside the hook. */
    public static SubmitNodeCollector collector() {
        return collector;
    }

    public VertexConsumer getBuffer(RenderType type) {
        return recorders.computeIfAbsent(type, t -> new Recorder());
    }

    public void endBatch() {
        SubmitNodeCollector target = collector;
        if (target != null) {
            recorders.forEach((type, recorder) -> {
                if (!recorder.isEmpty()) {
                    target.submitCustomGeometry(IDENTITY, type, (pose, out) -> recorder.replay(out));
                }
            });
        }
        recorders.clear();
    }

    /** Records VertexConsumer calls in order so they replay into the real buffer unchanged. */
    private static final class Recorder implements VertexConsumer {
        private static final int VERTEX = 0, COLOR = 1, COLOR_ARGB = 2, UV = 3, UV1 = 4, UV2 = 5,
            NORMAL = 6, LINE_WIDTH = 7;

        private int[] ops = new int[256];
        private int[] args = new int[1024]; // floats stored as raw bits so ints stay exact
        private int opCount;
        private int argCount;

        boolean isEmpty() {
            return opCount == 0;
        }

        void replay(VertexConsumer out) {
            int a = 0;
            for (int i = 0; i < opCount; i++) {
                switch (ops[i]) {
                    case VERTEX -> {
                        out.addVertex(f(a), f(a + 1), f(a + 2));
                        a += 3;
                    }
                    case COLOR -> {
                        out.setColor(args[a], args[a + 1], args[a + 2], args[a + 3]);
                        a += 4;
                    }
                    case COLOR_ARGB -> out.setColor(args[a++]);
                    case UV -> {
                        out.setUv(f(a), f(a + 1));
                        a += 2;
                    }
                    case UV1 -> {
                        out.setUv1(args[a], args[a + 1]);
                        a += 2;
                    }
                    case UV2 -> {
                        out.setUv2(args[a], args[a + 1]);
                        a += 2;
                    }
                    case NORMAL -> {
                        out.setNormal(f(a), f(a + 1), f(a + 2));
                        a += 3;
                    }
                    case LINE_WIDTH -> out.setLineWidth(f(a++));
                    default -> throw new IllegalStateException("Unknown vertex op " + ops[i]);
                }
            }
        }

        private float f(int index) {
            return Float.intBitsToFloat(args[index]);
        }

        private void op(int op) {
            if (opCount == ops.length) ops = Arrays.copyOf(ops, opCount * 2);
            ops[opCount++] = op;
        }

        private void arg(int value) {
            if (argCount == args.length) args = Arrays.copyOf(args, argCount * 2);
            args[argCount++] = value;
        }

        private void arg(float value) {
            arg(Float.floatToRawIntBits(value));
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            op(VERTEX);
            arg(x);
            arg(y);
            arg(z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            op(COLOR);
            arg(r);
            arg(g);
            arg(b);
            arg(a);
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            op(COLOR_ARGB);
            arg(argb);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            op(UV);
            arg(u);
            arg(v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            op(UV1);
            arg(u);
            arg(v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            op(UV2);
            arg(u);
            arg(v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            op(NORMAL);
            arg(x);
            arg(y);
            arg(z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            op(LINE_WIDTH);
            arg(width);
            return this;
        }
    }
}
