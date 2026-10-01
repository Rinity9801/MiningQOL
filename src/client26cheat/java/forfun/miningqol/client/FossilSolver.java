package forfun.miningqol.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Picks the next tile to dig in the Fossil Excavator grid, SkyHanni's solver in Java.
 *
 * <p>The grid is 9 wide by 6 tall (chest slot = x + 9y). Until a fossil tile shows, an
 * opening sequence is followed. After that every fossil shape, in each rotation and flip
 * it can appear in, is placed at every offset; placements that cover a dug-empty tile or
 * miss a found tile are discarded, and the undug tile covered by the most surviving
 * placements is the best guess.
 */
public final class FossilSolver {
    private FossilSolver() {}

    public record Tile(int x, int y) {
        public static Tile ofSlot(int slot) {
            return new Tile(slot % 9, slot / 9);
        }

        public int slot() {
            return x + y * 9;
        }
    }

    /** The solver's answer: a slot to click, or a completed / impossible board. */
    public record Result(Integer slot, double confidence, int possibilities, boolean completed, boolean impossible,
                         List<String> possibleTypes) {}

    private record Shape(List<Tile> tiles) {
        int width() {
            int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
            for (Tile t : tiles) { max = Math.max(max, t.x()); min = Math.min(min, t.x()); }
            return max - min;
        }

        int height() {
            int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
            for (Tile t : tiles) { max = Math.max(max, t.y()); min = Math.min(min, t.y()); }
            return max - min;
        }

        Shape moveTo(int dx, int dy) {
            List<Tile> out = new ArrayList<>(tiles.size());
            for (Tile t : tiles) out.add(new Tile(t.x() + dx, t.y() + dy));
            return new Shape(out);
        }

        Shape rotate(int degrees) {
            int w = width(), h = height();
            List<Tile> out = new ArrayList<>(tiles.size());
            switch (degrees) {
                case 90 -> { for (Tile t : tiles) out.add(new Tile(t.y(), w - t.x())); }
                case 180 -> { for (Tile t : tiles) out.add(new Tile(w - t.x(), h - t.y())); }
                case 270 -> { for (Tile t : tiles) out.add(new Tile(h - t.y(), t.x())); }
                default -> { return this; }
            }
            return new Shape(out);
        }

        Shape flip() {
            int h = height();
            List<Tile> out = new ArrayList<>(tiles.size());
            for (Tile t : tiles) out.add(new Tile(t.x(), h - t.y()));
            return new Shape(out);
        }
    }

    private enum Mutation {
        ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270, FLIP_ROTATE_0, FLIP_ROTATE_90, FLIP_ROTATE_180, FLIP_ROTATE_270;

        Shape apply(Shape s) {
            return switch (this) {
                case ROTATE_0 -> s;
                case ROTATE_90 -> s.rotate(90);
                case ROTATE_180 -> s.rotate(180);
                case ROTATE_270 -> s.rotate(270);
                case FLIP_ROTATE_0 -> s.flip();
                case FLIP_ROTATE_90 -> s.rotate(90).flip();
                case FLIP_ROTATE_180 -> s.rotate(180).flip();
                case FLIP_ROTATE_270 -> s.rotate(270).flip();
            };
        }

        static final List<Mutation> ALL = List.of(values());
        static final List<Mutation> ROTATIONS = List.of(ROTATE_0, ROTATE_90, ROTATE_180, ROTATE_270);
    }

    private enum FossilType {
        TUSK("Tusk", "12.5%", Mutation.ALL, t(0, 2), t(0, 3), t(0, 4), t(1, 1), t(2, 0), t(3, 1), t(3, 3), t(4, 2)),
        WEBBED("Webbed", "10%", List.of(Mutation.ROTATE_0, Mutation.FLIP_ROTATE_0),
            t(0, 2), t(1, 1), t(2, 0), t(3, 0), t(3, 1), t(3, 2), t(3, 3), t(4, 0), t(5, 1), t(6, 2)),
        CLUB("Club", "9.1%", List.of(Mutation.ROTATE_0, Mutation.ROTATE_180, Mutation.FLIP_ROTATE_0, Mutation.FLIP_ROTATE_180),
            t(0, 2), t(0, 3), t(1, 2), t(1, 3), t(2, 1), t(3, 0), t(4, 0), t(5, 0), t(6, 0), t(6, 2), t(7, 1)),
        SPINE("Spine", "8.3%", Mutation.ROTATIONS,
            t(0, 2), t(1, 1), t(1, 2), t(2, 0), t(2, 1), t(2, 2), t(3, 0), t(3, 1), t(3, 2), t(4, 1), t(4, 2), t(5, 2)),
        CLAW("Claw", "7.7%", Mutation.ALL,
            t(0, 3), t(1, 2), t(1, 4), t(2, 1), t(2, 3), t(3, 1), t(3, 2), t(3, 4), t(4, 0), t(4, 1), t(4, 2), t(4, 3), t(5, 1)),
        FOOTPRINT("Footprint", "7.7%", Mutation.ROTATIONS,
            t(0, 2), t(1, 1), t(1, 2), t(1, 3), t(2, 1), t(2, 2), t(2, 3), t(3, 0), t(3, 2), t(3, 4), t(4, 0), t(4, 2), t(4, 4)),
        HELIX("Helix", "7.1%", Mutation.ALL,
            t(0, 0), t(0, 1), t(0, 2), t(0, 4), t(1, 0), t(1, 2), t(1, 4), t(2, 0), t(2, 4), t(3, 0), t(3, 1), t(3, 2), t(3, 3), t(3, 4)),
        UGLY("Ugly", "6.2%", Mutation.ROTATIONS,
            t(0, 1), t(1, 0), t(1, 1), t(1, 2), t(2, 0), t(2, 1), t(2, 2), t(2, 3), t(3, 0), t(3, 1), t(3, 2), t(3, 3), t(4, 0), t(4, 1), t(4, 2), t(5, 1));

        final String displayName;
        final String firstPercentage;
        final List<Mutation> mutations;
        final Shape shape;

        FossilType(String displayName, String firstPercentage, List<Mutation> mutations, Tile... tiles) {
            this.displayName = displayName;
            this.firstPercentage = firstPercentage;
            this.mutations = mutations;
            this.shape = new Shape(List.of(tiles));
        }

        private static Tile t(int x, int y) {
            return new Tile(x, y);
        }
    }

    private record Opening(Tile tile, double chance, int possibilities) {}

    /** With fewer than 18 charges: solves 361/404 boards in 16 clicks, 400/404 in 18. */
    private static final List<Opening> RISKY_OPENING = List.of(
        new Opening(new Tile(4, 2), 0.515, 404), new Opening(new Tile(5, 3), 0.393, 196),
        new Opening(new Tile(3, 2), 0.513, 119), new Opening(new Tile(7, 2), 0.345, 58),
        new Opening(new Tile(1, 3), 0.342, 38), new Opening(new Tile(3, 4), 0.6, 25),
        new Opening(new Tile(5, 1), 0.8, 10), new Opening(new Tile(4, 3), 1.0, 2));

    /** With 18 charges: solves every board in 18 clicks. */
    private static final List<Opening> SAFE_OPENING = List.of(
        new Opening(new Tile(4, 2), 0.515, 404), new Opening(new Tile(5, 4), 0.413, 196),
        new Opening(new Tile(3, 3), 0.461, 115), new Opening(new Tile(5, 2), 0.387, 62),
        new Opening(new Tile(3, 1), 0.342, 38), new Opening(new Tile(7, 3), 0.48, 25),
        new Opening(new Tile(1, 2), 0.846, 13), new Opening(new Tile(3, 4), 1.0, 2));

    /**
     * @param fossilSlots slots showing a found fossil tile
     * @param dirtSlots   slots still undug
     * @param percentage  the "Fossil Excavation Progress" of the first fossil tile ("8.3%"), or null
     * @param maxCharges  chisel charges the dig started with (picks the opening sequence)
     */
    public static Result findBestTile(Set<Integer> fossilSlots, Set<Integer> dirtSlots, String percentage, int maxCharges) {
        Set<Tile> invalid = new HashSet<>();
        for (int i = 0; i < 54; i++) {
            if (!fossilSlots.contains(i) && !dirtSlots.contains(i)) invalid.add(Tile.ofSlot(i));
        }
        Set<Tile> found = new HashSet<>();
        for (int slot : fossilSlots) found.add(Tile.ofSlot(slot));

        List<Opening> sequence = maxCharges < 18 ? RISKY_OPENING : SAFE_OPENING;
        boolean inOpening = found.isEmpty();
        if (inOpening) {
            for (Tile t : invalid) {
                boolean inSeq = false;
                for (Opening o : sequence) if (o.tile().equals(t)) { inSeq = true; break; }
                if (!inSeq) { inOpening = false; break; }
            }
        }
        if (inOpening) {
            int taken = invalid.size();
            if (taken >= sequence.size()) return new Result(null, 0, 0, false, true, List.of());
            Opening next = sequence.get(taken);
            return new Result(next.tile().slot(), next.chance(), next.possibilities(), false, false, List.of());
        }

        List<FossilType> types = new ArrayList<>();
        for (FossilType type : FossilType.values()) {
            if (percentage == null || type.firstPercentage.equals(percentage)) types.add(type);
        }
        List<String> typeNames = new ArrayList<>();
        if (percentage != null) for (FossilType type : types) typeNames.add(type.displayName);

        Map<Tile, Integer> counts = new HashMap<>();
        int total = 0;
        for (int x = 0; x <= 8; x++) {
            for (int y = 0; y <= 5; y++) {
                for (FossilType type : types) {
                    for (Mutation mutation : type.mutations) {
                        Shape placed = mutation.apply(type.shape).moveTo(x, y);
                        if (!isValidPlacement(placed, invalid, found)) continue;
                        total++;
                        for (Tile t : placed.tiles()) counts.merge(t, 1, Integer::sum);
                    }
                }
            }
        }
        for (Tile t : found) counts.remove(t);

        Tile best = null;
        int bestCount = -1;
        for (Map.Entry<Tile, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) { bestCount = e.getValue(); best = e.getKey(); }
        }
        if (best == null) {
            return fossilSlots.isEmpty()
                ? new Result(null, 0, 0, false, true, typeNames)
                : new Result(null, 0, 0, true, false, typeNames);
        }
        return new Result(best.slot(), bestCount / (double) total, total, false, false, typeNames);
    }

    private static boolean isValidPlacement(Shape shape, Set<Tile> invalid, Set<Tile> found) {
        for (Tile t : shape.tiles()) {
            if (t.x() < 0 || t.y() < 0 || t.x() >= 9 || t.y() >= 6) return false;
            if (invalid.contains(t)) return false;
        }
        for (Tile f : found) {
            if (!shape.tiles().contains(f)) return false;
        }
        return true;
    }

    public static String describe(Result r) {
        if (r.impossible()) return "no possible fossils on board";
        if (r.completed()) return "fossil found";
        return String.format(Locale.ROOT, "slot %d (%.1f%%, %d placements)", r.slot(), r.confidence() * 100, r.possibilities());
    }
}
