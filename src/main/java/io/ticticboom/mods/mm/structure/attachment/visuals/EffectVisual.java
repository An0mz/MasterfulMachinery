package io.ticticboom.mods.mm.structure.attachment.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class EffectVisual {
    public enum AnchorKind { POINTS, HEIGHT, AREA, BLOCKS }

    public record Anchor(AnchorKind kind, double[][] points, double height, @Nullable VisualRegion area, @Nullable int[][] blocks) {
        Anchor(AnchorKind kind, double[][] points, double height, @Nullable VisualRegion area) {
            this(kind, points, height, area, null);
        }
    }

    public record Resolved(Map<String, double[]> points, Map<String, Double> heights, Map<String, long[]> areas, @Nullable AABB bounds) {
        @Nullable
        public double[] points(String name) {
            return points.get(name);
        }

        @Nullable
        public long[] area(String name) {
            return areas.get(name);
        }

        public double height(String name, double fallback) {
            Double h = heights.get(name);
            return h == null ? fallback : h;
        }

        public boolean hasHeight(String name) {
            return heights.containsKey(name);
        }
    }

    private final ResourceLocation fx;
    private final Map<String, Anchor> anchors;
    private final @Nullable String port;
    @Getter(lombok.AccessLevel.NONE)
    private final Map<Rotation, Resolved> resolvedCache = new EnumMap<>(Rotation.class);

    public EffectVisual(ResourceLocation fx, Map<String, Anchor> anchors, @Nullable String port) {
        this.fx = fx;
        this.anchors = anchors;
        this.port = port;
    }

    public static EffectVisual parse(JsonObject json) {
        if (!json.has("fx")) {
            throw new IllegalArgumentException("effect visual needs 'fx'");
        }
        ResourceLocation fx = ResourceLocation.tryParse(json.get("fx").getAsString());
        if (fx == null) {
            throw new IllegalArgumentException("effect 'fx' is not a valid id: " + json.get("fx").getAsString());
        }
        var anchors = new LinkedHashMap<String, Anchor>();
        if (json.has("anchors")) {
            for (var entry : json.getAsJsonObject("anchors").entrySet()) {
                anchors.put(entry.getKey(), parseAnchor(entry.getKey(), entry.getValue()));
            }
        }
        String port = json.has("port") ? json.get("port").getAsString() : null;
        return new EffectVisual(fx, anchors, port);
    }

    private static Anchor parseAnchor(String name, JsonElement el) {
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
            return new Anchor(AnchorKind.HEIGHT, null, el.getAsDouble(), null);
        }
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            return new Anchor(AnchorKind.AREA, null, 0, new VisualRegion(null, null, el.getAsString()));
        }
        if (el.isJsonArray()) {
            JsonArray array = el.getAsJsonArray();
            if (array.size() == 3 && array.get(0).isJsonPrimitive()) {
                return new Anchor(AnchorKind.POINTS, new double[][]{point(name, array)}, 0, null);
            }
            double[][] points = new double[array.size()][];
            for (int i = 0; i < array.size(); i++) {
                if (!array.get(i).isJsonArray()) {
                    throw new IllegalArgumentException("effect anchor '" + name + "' must be [x, y, z] or a list of them");
                }
                points[i] = point(name, array.get(i).getAsJsonArray());
            }
            return new Anchor(AnchorKind.POINTS, points, 0, null);
        }
        if (el.isJsonObject() && el.getAsJsonObject().has("blocks")) {
            JsonElement list = el.getAsJsonObject().get("blocks");
            if (!list.isJsonArray()) {
                throw new IllegalArgumentException("effect anchor '" + name + "' blocks must be a list of [x, y, z]");
            }
            int[][] blocks = new int[list.getAsJsonArray().size()][];
            for (int i = 0; i < blocks.length; i++) {
                JsonElement entry = list.getAsJsonArray().get(i);
                if (!entry.isJsonArray() || entry.getAsJsonArray().size() != 3) {
                    throw new IllegalArgumentException("effect anchor '" + name + "' blocks must be a list of [x, y, z]");
                }
                JsonArray b = entry.getAsJsonArray();
                blocks[i] = new int[]{b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt()};
            }
            return new Anchor(AnchorKind.BLOCKS, null, 0, null, blocks);
        }
        if (el.isJsonObject() && el.getAsJsonObject().has("from")) {
            return new Anchor(AnchorKind.AREA, null, 0, VisualRegion.parse(el.getAsJsonObject()));
        }
        throw new IllegalArgumentException("effect anchor '" + name + "' must be a point, a list of points, a height, a layout key or a from/to box");
    }

    private static double[] point(String name, JsonArray array) {
        if (array.size() != 3) {
            throw new IllegalArgumentException("effect anchor '" + name + "' points must be [x, y, z]");
        }
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    public void validate(ResourceLocation structureId, StructureLayout layout) {
        if (port != null && !VisualRegion.isKey(layout, port)) {
            Ref.LOG.warn("Structure {}: effect port '{}' is not a key in the layout", structureId, port);
        }
        for (var entry : anchors.entrySet()) {
            var area = entry.getValue().area();
            if (area != null) {
                area.validate(structureId, layout, "effect anchor '" + entry.getKey() + "'", "effect");
            }
        }
    }

    public Resolved resolve(StructureLayout layout, Rotation rotation) {
        Resolved resolved = resolvedCache.get(rotation);
        if (resolved == null) {
            resolved = compute(layout, rotation);
            resolvedCache.put(rotation, resolved);
        }
        return resolved;
    }

    private Resolved compute(StructureLayout layout, Rotation rotation) {
        BlockPos origin = layout.getCharGrid().getControllerPos();
        var points = new LinkedHashMap<String, double[]>();
        var heights = new LinkedHashMap<String, Double>();
        var areas = new LinkedHashMap<String, long[]>();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (var entry : anchors.entrySet()) {
            Anchor anchor = entry.getValue();
            switch (anchor.kind()) {
                case POINTS -> {
                    double[] flat = new double[anchor.points().length * 3];
                    for (int i = 0; i < anchor.points().length; i++) {
                        rotatePoint(anchor.points()[i], origin, rotation, flat, i * 3);
                        minX = Math.min(minX, flat[i * 3]);
                        maxX = Math.max(maxX, flat[i * 3]);
                        minY = Math.min(minY, flat[i * 3 + 1]);
                        maxY = Math.max(maxY, flat[i * 3 + 1]);
                        minZ = Math.min(minZ, flat[i * 3 + 2]);
                        maxZ = Math.max(maxZ, flat[i * 3 + 2]);
                    }
                    points.put(entry.getKey(), flat);
                }
                case HEIGHT -> {
                    double y = anchor.height() - origin.getY();
                    heights.put(entry.getKey(), y);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
                case BLOCKS -> {
                    long[] cells = new long[anchor.blocks().length];
                    double[] center = new double[3];
                    for (int i = 0; i < cells.length; i++) {
                        int[] b = anchor.blocks()[i];
                        rotatePoint(new double[]{b[0] + 0.5, b[1] + 0.5, b[2] + 0.5}, origin, rotation, center, 0);
                        int x = (int) Math.floor(center[0]);
                        int y = (int) Math.floor(center[1]);
                        int z = (int) Math.floor(center[2]);
                        cells[i] = BlockPos.asLong(x, y, z);
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x + 1);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y + 1);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z + 1);
                    }
                    areas.put(entry.getKey(), cells);
                }
                case AREA -> {
                    long[] cells = anchor.area().relativeCells(layout, rotation);
                    areas.put(entry.getKey(), cells);
                    for (long cell : cells) {
                        minX = Math.min(minX, BlockPos.getX(cell));
                        maxX = Math.max(maxX, BlockPos.getX(cell) + 1);
                        minY = Math.min(minY, BlockPos.getY(cell));
                        maxY = Math.max(maxY, BlockPos.getY(cell) + 1);
                        minZ = Math.min(minZ, BlockPos.getZ(cell));
                        maxZ = Math.max(maxZ, BlockPos.getZ(cell) + 1);
                    }
                }
            }
        }
        AABB bounds = null;
        if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
            bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(1);
        } else if (minY <= maxY) {
            bounds = new AABB(-1, minY, -1, 2, maxY, 2).inflate(1);
        }
        return new Resolved(Map.copyOf(points), Map.copyOf(heights), Map.copyOf(areas), bounds);
    }

    public static void rotatePoint(double[] point, BlockPos origin, Rotation rotation, double[] out, int index) {
        int cx = (int) Math.floor(point[0]);
        int cz = (int) Math.floor(point[2]);
        double fx = point[0] - cx;
        double fz = point[2] - cz;
        BlockPos cell = new BlockPos(cx - origin.getX(), 0, cz - origin.getZ()).rotate(rotation);
        double rx = switch (rotation) {
            case NONE -> fx;
            case CLOCKWISE_90 -> 1 - fz;
            case CLOCKWISE_180 -> 1 - fx;
            case COUNTERCLOCKWISE_90 -> fz;
        };
        double rz = switch (rotation) {
            case NONE -> fz;
            case CLOCKWISE_90 -> fx;
            case CLOCKWISE_180 -> 1 - fz;
            case COUNTERCLOCKWISE_90 -> 1 - fx;
        };
        out[index] = cell.getX() + rx;
        out[index + 1] = point[1] - origin.getY();
        out[index + 2] = cell.getZ() + rz;
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", "effect");
        json.addProperty("fx", fx.toString());
        if (port != null) {
            json.addProperty("port", port);
        }
        var anchorJson = new JsonObject();
        for (var entry : anchors.entrySet()) {
            Anchor anchor = entry.getValue();
            switch (anchor.kind()) {
                case HEIGHT -> anchorJson.add(entry.getKey(), new JsonPrimitive(anchor.height()));
                case AREA -> {
                    if (anchor.area().getArea() != null) {
                        anchorJson.addProperty(entry.getKey(), anchor.area().getArea());
                    } else {
                        var box = new JsonObject();
                        anchor.area().writeJson(box);
                        anchorJson.add(entry.getKey(), box);
                    }
                }
                case BLOCKS -> {
                    var list = new JsonArray();
                    for (int[] b : anchor.blocks()) {
                        var array = new JsonArray();
                        array.add(b[0]);
                        array.add(b[1]);
                        array.add(b[2]);
                        list.add(array);
                    }
                    var holder = new JsonObject();
                    holder.add("blocks", list);
                    anchorJson.add(entry.getKey(), holder);
                }
                case POINTS -> {
                    var list = new JsonArray();
                    for (double[] p : anchor.points()) {
                        var array = new JsonArray();
                        array.add(p[0]);
                        array.add(p[1]);
                        array.add(p[2]);
                        list.add(array);
                    }
                    anchorJson.add(entry.getKey(), list);
                }
            }
        }
        json.add("anchors", anchorJson);
        return json;
    }
}
