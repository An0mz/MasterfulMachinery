package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class SeepLayer implements FxLayer {
    private static final int[] DEFAULT_PALETTE = {0x240306, 0x37050a, 0x4c080d, 0x5f0c11, 0x741416, 0x8c2421, 0xa8473f};
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Ref.ID, "textures/fx/blood_white.png");
    private static final int MAX_BLOCKS = 16;
    private static final double OFF = 0.0045;
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private final String at;
    private final String top;
    private final int fixedColor;
    @Nullable
    private final ResourceLocation tintTexture;
    private final int[] palette;
    private final boolean portColor;
    private final boolean glow;
    private final double[] shades = new double[7];
    private final double alpha;
    private final double chance;
    private final double speed;
    private final boolean stains;
    private final boolean dripPatch;
    private final int faceMask;
    private final int[] pointMasks;
    private final int streams;
    private final int[] sideStreams;
    private final int[][] pointStreams;

    private final int[] skip;

    private SeepLayer(JsonObject json) {
        skip = FxSkip.parse(json);
        at = json.has("at") ? json.get("at").getAsString() : "drips";
        top = json.has("top") ? json.get("top").getAsString() : "seep_top";
        fixedColor = FxColor.parse(json, "color", -1);
        ResourceLocation picked = null;
        if (json.has("texture")) {
            picked = ResourceLocation.tryParse(json.get("texture").getAsString());
            if (picked == null) {
                throw new IllegalArgumentException("seep 'texture' is not a valid id: " + json.get("texture").getAsString());
            }
        }
        tintTexture = TEXTURE.equals(picked) ? null : picked;
        palette = json.has("palette") ? parsePalette(json.getAsJsonArray("palette")) : DEFAULT_PALETTE;
        portColor = FxColor.port(json) || FxColor.portTexture(json);
        glow = json.has("glow") && json.get("glow").getAsBoolean();
        double mean = 0;
        for (int c : palette) {
            mean += FxColor.luminance(c) / palette.length;
        }
        for (int i = 0; i < 7; i++) {
            shades[i] = mean <= 0 ? 1 : FxColor.luminance(palette[i]) / mean;
        }
        alpha = json.has("alpha") ? json.get("alpha").getAsDouble() : 1.0;
        chance = json.has("chance") ? json.get("chance").getAsDouble() : 0.6;
        speed = json.has("speed") ? json.get("speed").getAsDouble() : 1.0;
        stains = !json.has("stains") || json.get("stains").getAsBoolean();
        dripPatch = !json.has("drip_patch") || json.get("drip_patch").getAsBoolean();
        if (json.has("streams") && !(json.get("streams").isJsonPrimitive() && json.get("streams").getAsJsonPrimitive().isNumber())) {
            throw new IllegalArgumentException("seep 'streams' must be a number from 1 to 4");
        }
        streams = Math.max(1, Math.min(4, json.has("streams") ? json.get("streams").getAsInt() : 1));
        sideStreams = json.has("side_streams") ? countsOf(json.get("side_streams")) : new int[4];
        if (json.has("point_streams")) {
            if (!json.get("point_streams").isJsonArray()) {
                throw new IllegalArgumentException("seep 'point_streams' must be a list with one entry per drip point");
            }
            JsonArray list = json.getAsJsonArray("point_streams");
            pointStreams = new int[list.size()][];
            for (int i = 0; i < list.size(); i++) {
                pointStreams[i] = list.get(i).isJsonNull() ? null : countsOf(list.get(i));
            }
        } else {
            pointStreams = new int[0][];
        }
        faceMask = json.has("faces") ? maskOf(json.get("faces")) : -1;
        if (json.has("point_faces")) {
            if (!json.get("point_faces").isJsonArray()) {
                throw new IllegalArgumentException("seep 'point_faces' must be a list with one entry per drip point");
            }
            JsonArray list = json.getAsJsonArray("point_faces");
            pointMasks = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                pointMasks[i] = list.get(i).isJsonNull() ? -1 : maskOf(list.get(i));
            }
        } else {
            pointMasks = new int[0];
        }
    }

    private static int maskOf(com.google.gson.JsonElement value) {
        var listed = new JsonArray();
        if (value.isJsonArray()) {
            listed = value.getAsJsonArray();
        } else {
            listed.add(value);
        }
        int mask = 0;
        for (var element : listed) {
            Direction dir = element.isJsonPrimitive() ? Direction.byName(element.getAsString().toLowerCase(java.util.Locale.ROOT)) : null;
            if (dir == null || dir.getAxis() == Direction.Axis.Y) {
                throw new IllegalArgumentException("seep 'faces' must list north, east, south or west");
            }
            mask |= 1 << dir.get2DDataValue();
        }
        return mask;
    }

    private static int[] countsOf(com.google.gson.JsonElement value) {
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException("seep streams per side must be an object like {\"north\": 3, \"east\": 1}");
        }
        int[] counts = new int[4];
        for (var entry : value.getAsJsonObject().entrySet()) {
            Direction dir = Direction.byName(entry.getKey().toLowerCase(java.util.Locale.ROOT));
            if (dir == null || dir.getAxis() == Direction.Axis.Y) {
                throw new IllegalArgumentException("seep streams per side must be named north, east, south or west");
            }
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("seep streams per side must be numbers from 1 to 4");
            }
            int n = entry.getValue().getAsInt();
            counts[dir.get2DDataValue()] = n < 1 ? 0 : Math.min(4, n);
        }
        return counts;
    }

    private static Direction direction(int index) {
        return switch (index) {
            case 0 -> Direction.WEST;
            case 1 -> Direction.EAST;
            case 2 -> Direction.NORTH;
            default -> Direction.SOUTH;
        };
    }

    private static int index(int dx, int dz) {
        for (int d = 0; d < 4; d++) {
            if (DIRECTIONS[d][0] == dx && DIRECTIONS[d][1] == dz) {
                return d;
            }
        }
        return 0;
    }

    private static int rotatedMask(int faces, Rotation rotation) {
        if (faces < 0) {
            return -1;
        }
        int mask = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if ((faces & (1 << dir.get2DDataValue())) != 0) {
                Direction turned = rotation.rotate(dir);
                mask |= 1 << index(turned.getStepX(), turned.getStepZ());
            }
        }
        return mask;
    }

    private static int[] parsePalette(JsonArray array) {
        if (array.size() != 7) {
            throw new IllegalArgumentException("seep 'palette' needs 7 colours, darkest first");
        }
        int[] result = new int[7];
        for (int i = 0; i < 7; i++) {
            var holder = new JsonObject();
            holder.add("c", array.get(i));
            result[i] = FxColor.parse(holder, "c", 0);
        }
        return result;
    }

    public static SeepLayer parse(JsonObject json) {
        return new SeepLayer(json);
    }

    @Override
    @Nullable
    public State create(FxShared shared) {
        FxColumn[] columns = FxSkip.keep(shared.columns(at, null), skip);
        Double seepTop = shared.height(top);
        double[] tops = seepTop == null ? shared.points(top) : null;
        if (columns == null) {
            return null;
        }
        if (seepTop == null && (tops == null || tops.length < 3)) {
            FxLog.warnOnce("Seep layer: no height or point anchor named '{}'", top);
            return null;
        }
        var faces = new Face[columns.length][];
        for (int i = 0; i < columns.length; i++) {
            FxColumn c = columns[i];
            double columnTop = seepTop != null ? seepTop : tops[Math.min(c.index, tops.length / 3 - 1) * 3 + 1];
            int rows = (int) Math.round((columnTop - c.top) * 16);
            if (rows <= 0) {
                continue;
            }
            rows = Math.min(rows, MAX_BLOCKS * 16);
            RandomSource random = RandomSource.create(shared.seed() ^ 0x5DEECE66DL ^ (c.index * 0x9E3779B97F4A7C15L));
            int[] perSide = sideCounts(c.index, shared.rotation);
            int most = Math.max(Math.max(perSide[0], perSide[1]), Math.max(perSide[2], perSide[3]));
            int x1 = (int) Math.floor(c.x + 1e-6);
            int z1 = (int) Math.floor(c.z + 1e-6);
            boolean spanX = Math.abs(c.x - x1) < 1e-6;
            boolean spanZ = Math.abs(c.z - z1) < 1e-6;
            int x0 = spanX ? x1 - 1 : x1;
            int z0 = spanZ ? z1 - 1 : z1;
            int depthRows = spanX || spanZ ? 2 : 1;
            int lines = depthRows > 1 ? Math.max(2, most) : most;
            faces[i] = new Face[depthRows * 4 * lines];
            int f = 0;
            for (int r = 0; r < depthRows; r++) {
                for (int s = 0; s < lines; s++) {
                    for (int d = 0; d < 4; d++) {
                        boolean xFace = DIRECTIONS[d][0] != 0;
                        int width = xFace ? z1 - z0 + 1 : x1 - x0 + 1;
                        int depth = xFace ? x1 - x0 + 1 : z1 - z0 + 1;
                        int n = perSide[d];
                        boolean pair = n == 1 && width > 1;
                        boolean active = (s < n || pair && s == 1) && r < depth;
                        int across = 0;
                        int slot = -1;
                        if (active && n > 1) {
                            int spot = 3 + (int) Math.round(s * (width * 16 - 7.0) / (n - 1));
                            across = spot / 16;
                            slot = Math.max(1, Math.min(14, spot % 16));
                        } else if (width > 1) {
                            int preferred = FxNoise.hash(d * 7 + r, c.seed) < 0.5 ? 0 : 1;
                            across = pair && s == 1 ? 1 - preferred : preferred;
                        }
                        int along = Math.min(r, depth - 1);
                        int cx = xFace ? x0 + along : x0 + across;
                        int cz = xFace ? z0 + across : z0 + along;
                        Face face = new Face(c, cx, cz, spanX, spanZ, DIRECTIONS[d][0], DIRECTIONS[d][1], columnTop, rows, random, slot, active, s == 0);
                        if (pair && s == 1) {
                            face.twin = faces[i][f - 4];
                        }
                        faces[i][f++] = face;
                    }
                }
            }
        }
        int[] toWorld = new int[4];
        for (int d = 0; d < 4; d++) {
            Direction layoutDir = direction(d);
            Direction worldDir = shared.rotation.rotate(layoutDir);
            toWorld[d] = index(worldDir.getStepX(), worldDir.getStepZ());
        }
        for (int i = 0; i < columns.length; i++) {
            if (faces[i] == null) {
                continue;
            }
            RandomSource rolls = RandomSource.create(shared.seed() ^ 0x51EE7L ^ (columns[i].index * 0x2545F4914F6CDD1DL));
            for (int d = 0; d < 4; d++) {
                double roll = rolls.nextDouble();
                for (int s = toWorld[d]; s < faces[i].length; s += 4) {
                    faces[i][s].pickRoll = roll;
                }
            }
        }
        int[] chosen = new int[columns.length];
        for (int i = 0; i < columns.length; i++) {
            int own = columns[i].index < pointMasks.length ? pointMasks[columns[i].index] : -1;
            chosen[i] = rotatedMask(own >= 0 ? own : faceMask, shared.rotation);
        }
        int baseTint = fixedColor >= 0 ? fixedColor : tintTexture == null ? -1 : FxPortColor.texture(tintTexture);
        return new Seeps(columns, faces, chosen, toWorld, baseTint);
    }

    private int[] sideCounts(int point, Rotation rotation) {
        int[] own = point < pointStreams.length ? pointStreams[point] : null;
        int[] result = new int[4];
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int side = dir.get2DDataValue();
            int count = own != null && own[side] > 0 ? own[side] : sideStreams[side] > 0 ? sideStreams[side] : streams;
            Direction turned = rotation.rotate(dir);
            result[index(turned.getStepX(), turned.getStepZ())] = count;
        }
        return result;
    }

    private static int insetOf(VoxelShape shape, Face f, double y, double u, int fallback) {
        double edge;
        if (f.xFace) {
            edge = f.sign > 0 ? shape.max(Direction.Axis.X, y, u) : shape.min(Direction.Axis.X, y, u);
        } else {
            edge = f.sign > 0 ? shape.max(Direction.Axis.Z, u, y) : shape.min(Direction.Axis.Z, u, y);
        }
        if (Double.isInfinite(edge)) {
            return fallback;
        }
        int px = (int) Math.round((f.sign > 0 ? 1 - edge : edge) * 16);
        return Math.max(0, Math.min(15, px));
    }

    private int color(int tint, int index, double a) {
        int al = (int) Math.max(0, Math.min(255, a));
        int i = Math.max(0, Math.min(6, index));
        return tint >= 0 ? FxColor.argb(al, tint, shades[i]) : al << 24 | palette[i];
    }

    private static final class Face {
        final FxColumn column;
        final boolean xFace;
        final int sign;
        final int nx;
        final int nz;
        final int cellX;
        final int cellZ;
        final double plane;
        final double uCenter;
        final double u0;
        final double phase;
        final double flowSpeed;
        final double seepTop;
        final int rows;
        final int[] path;
        final long seed;
        double pickRoll;
        @Nullable
        Face twin;
        final boolean active;
        final boolean lead;
        final boolean spans;
        final double stainY;
        final double stainU;
        final double stainLen;
        final double stainPhase;
        final int[] light = new int[MAX_BLOCKS];
        final byte[] inset;
        boolean shaped;
        int stainInset;
        final int cellMin;
        int stainLight;
        double crawl;
        boolean exposed;
        boolean stainExposed;
        boolean hides;
        boolean stainHides;
        boolean ringAbove;
        boolean rivulet;
        boolean stain;

        Face(FxColumn column, int cellX, int cellZ, boolean spanX, boolean spanZ, int dx, int dz, double seepTop, int rows, RandomSource random,
             int slot, boolean active, boolean lead) {
            this.column = column;
            this.active = active;
            this.lead = lead;
            this.spans = spanX || spanZ;
            this.xFace = dx != 0;
            this.sign = xFace ? dx : dz;
            this.nx = dx;
            this.nz = dz;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.plane = xFace ? (sign < 0 ? cellX : cellX + 1) : (sign < 0 ? cellZ : cellZ + 1);
            this.uCenter = xFace ? (spanZ ? cellZ + 0.5 : column.z) : (spanX ? cellX + 0.5 : column.x);
            this.seepTop = seepTop;
            this.rows = rows;
            this.pickRoll = random.nextDouble();
            this.u0 = uCenter + (random.nextDouble() - 0.5) * 0.3;
            this.phase = random.nextDouble();
            this.flowSpeed = 0.8 + 0.4 * random.nextDouble();
            this.seed = (long) (phase * 1e9);
            double base = (xFace ? cellZ : cellX) + 0.5;
            int seams = Math.max(1, (int) Math.round(seepTop - column.top));
            this.stainY = seepTop - random.nextInt(seams);
            this.stainU = base + (random.nextDouble() - 0.5) * 0.5;
            this.stainLen = 0.2 + 0.35 * random.nextDouble();
            this.stainPhase = random.nextDouble();
            this.path = new int[rows];
            this.inset = new byte[rows];
            this.cellMin = (xFace ? cellZ : cellX) * 16;
            int target = (int) Math.floor(uCenter * 16);
            int start = slot >= 0 ? cellMin + slot
                    : Math.max(cellMin + 3, Math.min(cellMin + 12, target + (u0 < uCenter ? -1 : 1) * (FxNoise.hash(7, seed) < 0.5 ? 1 : 0)));
            int cur = start;
            for (int row = 0; row < rows; row++) {
                boolean seamRow = row > 0 && row % 16 == 0;
                if ((seamRow && FxNoise.hash(row, seed) < 0.45) || FxNoise.hash(row + 900, seed) < 0.02) {
                    int next = cur + (FxNoise.hash(row, seed + 3) < 0.5 ? -1 : 1);
                    if (Math.abs(next - start) <= 1) {
                        cur = next;
                    }
                }
                path[row] = cur;
            }
        }
    }

    private final class Seeps implements State {
        private final FxColumn[] columns;
        private final Face[][] faces;
        private final int[] lastMask;
        private final int[] chosen;
        private final int[] toWorld;
        private final int baseTint;
        private int tint = -1;
        private RenderType renderType;

        Seeps(FxColumn[] columns, Face[][] faces, int[] chosen, int[] toWorld, int baseTint) {
            this.baseTint = baseTint;
            this.columns = columns;
            this.faces = faces;
            this.chosen = chosen;
            this.toWorld = toWorld;
            this.lastMask = new int[columns.length];
            Arrays.fill(lastMask, -1);
        }

        @Override
        public void scan(FxContext ctx) {
            for (int i = 0; i < columns.length; i++) {
                Face[] list = faces[i];
                if (list == null) {
                    continue;
                }
                int mask = 0;
                boolean changed = false;
                for (int d = 0; d < list.length; d++) {
                    Face f = list[d];
                    if (!f.active) {
                        continue;
                    }
                    int topBlock = (int) Math.round(f.seepTop);
                    boolean ok = f.spans || f.column.blockAbove;
                    boolean hides = true;
                    for (int y = topBlock - 1; y >= (int) Math.round(f.column.top) && ok; y--) {
                        ok = ctx.passable(f.cellX + f.nx, y, f.cellZ + f.nz) && ctx.solid(f.cellX, y, f.cellZ);
                        hides &= ok && ctx.opaque(f.cellX, y, f.cellZ);
                    }
                    f.hides = hides;
                    changed |= f.exposed != ok;
                    f.exposed = ok;
                    f.ringAbove = ctx.solid(f.cellX + f.nx, topBlock, f.cellZ + f.nz);
                    int by = (int) Math.floor(f.stainY - 0.001);
                    f.stainExposed = ctx.solid(f.cellX, by, f.cellZ) && ctx.passable(f.cellX + f.nx, by, f.cellZ + f.nz);
                    f.stainHides = ctx.opaque(f.cellX, by, f.cellZ);
                    f.stainLight = ctx.light(f.cellX + f.nx + 0.5, by + 0.5, f.cellZ + f.nz + 0.5);
                    VoxelShape stainShape = ctx.shape(BlockPos.asLong(f.cellX, by, f.cellZ));
                    f.stainInset = stainShape == Shapes.block() ? 0
                            : insetOf(stainShape, f, f.stainY - 0.03 - by, f.stainU - (f.xFace ? f.cellZ : f.cellX), 0);
                    if (ok) {
                        mask |= 1 << (d % 4);
                    }
                    int blocks = Math.min(MAX_BLOCKS, (f.rows + 15) / 16);
                    for (int k = 0; k < blocks; k++) {
                        f.light[k] = ctx.light(f.cellX + f.nx + 0.5, topBlock - 1 - k + 0.5, f.cellZ + f.nz + 0.5);
                    }
                    if (ok) {
                        shape(ctx, f);
                    }
                }
                if (mask != lastMask[i] || changed) {
                    lastMask[i] = mask;
                    pick(list, mask, chosen[i]);
                }
            }
        }

        private void shape(FxContext ctx, Face f) {
            int topPx = (int) Math.round(f.seepTop * 16);
            int lastBlock = Integer.MIN_VALUE;
            VoxelShape shape = null;
            boolean full = true;
            boolean any = false;
            int last = -1;
            int firstValid = -1;
            for (int row = 0; row < f.rows; row++) {
                double cy = (topPx - row - 0.5) / 16.0;
                int by = (int) Math.floor(cy);
                if (by != lastBlock) {
                    lastBlock = by;
                    shape = ctx.shape(BlockPos.asLong(f.cellX, by, f.cellZ));
                    full = shape == Shapes.block();
                }
                last = full ? 0 : insetOf(shape, f, cy - by, (f.path[row] - f.cellMin + 0.5) / 16.0, last);
                if (last >= 0 && firstValid < 0) {
                    firstValid = row;
                }
                f.inset[row] = (byte) last;
                any |= last > 0;
            }
            int lead = firstValid < 0 ? 0 : f.inset[firstValid];
            for (int row = 0; row < f.rows && (firstValid < 0 || row < firstValid); row++) {
                f.inset[row] = (byte) lead;
            }
            f.shaped = any;
        }

        private void pick(Face[] list, int mask, int chosen) {
            if (chosen >= 0) {
                for (int d = 0; d < list.length; d++) {
                    list[d].rivulet = list[d].active && list[d].exposed && (chosen & (1 << (d % 4))) != 0;
                }
            } else {
                for (int d = 0; d < 4; d++) {
                    boolean on = (mask & (1 << d)) != 0 && list[d].pickRoll < chance;
                    for (int s = d; s < list.length; s += 4) {
                        list[s].rivulet = on && list[s].active;
                    }
                }
                boolean any = false;
                for (Face f : list) {
                    any |= f.rivulet;
                }
                if (!any && mask != 0) {
                    int first = (int) (FxNoise.hash(11, list[0].column.seed) * 4);
                    for (int n = 0; n < 4; n++) {
                        int d = toWorld[(first + n) % 4];
                        if ((mask & (1 << d)) != 0) {
                            for (int s = d; s < list.length; s += 4) {
                                list[s].rivulet = list[s].active;
                            }
                            break;
                        }
                    }
                }
            }
            for (Face f : list) {
                if (f.twin != null && f.rivulet && f.twin.rivulet && f.twin.exposed) {
                    f.rivulet = false;
                }
            }
            for (int d = 0; d < list.length; d++) {
                list[d].stain = stains && list[d].lead && list[d].active && !list[d].rivulet;
            }
        }

        @Override
        public void update(FxContext ctx) {
            double ws = ctx.intensity.now();
            double base = (1.6 + 9.0 * ws) * speed * ctx.dt;
            for (Face[] list : faces) {
                if (list == null) {
                    continue;
                }
                for (Face f : list) {
                    f.crawl += base * f.flowSpeed;
                }
            }
        }

        private int color(int index, double a) {
            return SeepLayer.this.color(tint, index, a);
        }

        @Override
        public void render(FxContext ctx) {
            tint = FxColor.pick(portColor, baseTint, ctx);
            VertexConsumer vc = null;
            double ws = ctx.intensity.now();
            for (int i = 0; i < columns.length; i++) {
                Face[] list = faces[i];
                if (list == null || !columns[i].blockAbove && !list[0].spans) {
                    continue;
                }
                if (vc == null) {
                    if (renderType == null) {
                        renderType = RenderType.entityTranslucent(TEXTURE);
                    }
                    vc = ctx.buffers.getBuffer(renderType);
                }
                for (Face f : list) {
                    boolean behind = ((f.xFace ? ctx.cameraX : ctx.cameraZ) - f.plane) * f.sign <= 0;
                    if (f.rivulet && f.exposed) {
                        boolean facing = !behind || !f.hides;
                        if (ctx.lod == 0) {
                            rivulet(vc, ctx, f, ws, facing);
                        } else if (facing) {
                            simpleRivulet(vc, ctx, f, ws);
                        }
                    } else if ((!behind || !f.stainHides) && f.stain && f.stainExposed && ctx.lod < 2) {
                        stain(vc, ctx, f, ws);
                    }
                }
                if (dripPatch && ctx.lod < 2) {
                    patch(vc, ctx, columns[i], ws);
                }
            }
        }

        private void rivulet(VertexConsumer vc, FxContext ctx, Face r, double ws, boolean facing) {
            long seed = r.seed;
            int[] col = r.path;
            int rows = r.rows;
            double a = (215 + 35 * ws) * ctx.fade * alpha;
            int moved = (int) Math.floor(r.crawl + r.phase * 37);
            int period = ws > 0.5 ? 9 : 26;
            int topPx = (int) Math.round(r.seepTop * 16);
            int underLight = r.column.lightAt(r.column.top - 0.5);
            int light0 = r.light[0];

            int lw = 2 + (int) (FxNoise.hash(1, seed) * 3);
            if (facing) {
                walls(vc, ctx, r, ws, a, moved, period, topPx, lw, light0);
            }
            if (r.ringAbove) {
                ringJoint(vc, ctx, r, col[0], lw, seed, a, light0);
            }
            int edgeU = col[rows - 1];
            int inward = -r.sign;
            int dripU = (int) Math.floor((r.xFace ? r.column.z : r.column.x) * 16);
            int planePx = (int) Math.round(r.plane * 16) - r.sign * r.inset[rows - 1];
            int startAcross = planePx + (inward < 0 ? -1 : 0);
            int dripAcross = (int) Math.floor((r.xFace ? r.column.x : r.column.z) * 16);
            if (r.inset[rows - 1] > 0 && dripAcross != startAcross) {
                inward = Integer.signum(dripAcross - startAcross);
            }
            int acrossDist = Math.abs(dripAcross - startAcross);
            int under = Math.max(acrossDist, Math.abs(dripU - edgeU)) + 1;
            int u = edgeU;
            int lastAcross = Integer.MIN_VALUE;
            int lastU = Integer.MIN_VALUE;
            for (int k = 0; k < under; k++) {
                int gap = Math.abs(dripU - u);
                if (gap > 0 && (gap > under - 1 - k || FxNoise.hash(k + 300, seed) < 0.4)) {
                    u += Integer.signum(dripU - u);
                }
                int across = startAcross + inward * Math.min(k, acrossDist);
                if (across == lastAcross && u == lastU) {
                    continue;
                }
                lastAcross = across;
                lastU = u;
                int ph = Math.floorMod(rows + k - moved, period);
                int idx = ph < 3 ? 4 : (ws > 0.5 ? 3 : 2);
                under(vc, ctx, r.xFace, r.column.top, across, u, color(idx, a), underLight);
                if (ph < 3) {
                    under(vc, ctx, r.xFace, r.column.top, across, u + 1, color(2, a), underLight);
                }
            }
            int phEdge = Math.floorMod(rows - 1 - moved, period);
            if (phEdge < 3) {
                double edgePlane = r.plane - r.sign * r.inset[rows - 1] / 16.0;
                double ex = r.xFace ? edgePlane - r.sign * 0.5 / 16 : (edgeU + 0.5) / 16.0;
                double ez = r.xFace ? (edgeU + 0.5) / 16.0 : edgePlane - r.sign * 0.5 / 16;
                double hh = 0.5 / 16 * (1 + phEdge * 0.5);
                FxMesh.box(vc, ctx.pose, ex, r.column.top - hh, ez, 0.5 / 16, hh, color(3, a), glow ? LightTexture.FULL_BRIGHT : underLight);
            }
        }

        private void walls(VertexConsumer vc, FxContext ctx, Face r, double ws, double a, int moved, int period, int topPx, int lw, int light0) {
            long seed = r.seed;
            int[] col = r.path;
            int rows = r.rows;
            for (int k = -lw; k <= lw; k++) {
                if (FxNoise.hash(k + 50, seed) < 0.25 && Math.abs(k) > 1) {
                    continue;
                }
                int u = inCell(r, col[0] + k);
                if (Math.abs(k) <= 1 && rows > 1 && r.inset[1] != r.inset[0]) {
                    px(vc, ctx, r, u, topPx, 1, 1, color(1, a), light0, r.inset[0]);
                    px(vc, ctx, r, u, topPx - 1, 1, 1, color(1, a), light0, r.inset[1]);
                } else {
                    px(vc, ctx, r, u, topPx, 1, Math.abs(k) <= 1 ? 2 : 1, color(1, a), light0, r.inset[0]);
                }
            }
            int side = FxNoise.hash(2, seed) < 0.5 ? -lw : lw;
            int sideLen = 2 + (int) (FxNoise.hash(3, seed) * 4);
            for (int k = 0; k < sideLen; k++) {
                boolean last = k == sideLen - 1;
                px(vc, ctx, r, inCell(r, col[0] + side), topPx - 1 - k, 1, 1, color(last ? 1 : 2, a * (last ? 0.7 : 1)), light0,
                        r.inset[Math.min(k + 1, rows - 1)]);
            }
            int runX = Integer.MIN_VALUE;
            int coreTop = 0;
            int coreLen = 0;
            int coreCol = 0;
            int coreLight = 0;
            int coreIn = 0;
            int wideIn = 0;
            int wideTop = 0;
            int wideLen = 0;
            int wideCol = 0;
            int wideLight = 0;
            for (int row = 0; row < rows; row++) {
                int y = topPx - row;
                int l = r.light[Math.min(r.light.length - 1, row / 16)];
                int x = col[row];
                int in = r.inset[row];
                int ph = Math.floorMod(row - moved, period);
                boolean bead = ph < 3;
                boolean wide = ws > 0.5 || FxNoise.hash(row + 200, seed) < 0.2 + 0.6 * ws;
                boolean wider = ws > 0.4 && FxNoise.hash(row + 400, seed) < 0.45 * ws;
                int core = ws > 0.6 ? 4 : (ws > 0.3 ? 3 : 2);
                if (ws > 0.3 && Math.floorMod(row - moved * 2, 5) == 0) {
                    core = 5;
                }
                if (x != runX) {
                    if (coreLen > 0) {
                        px(vc, ctx, r, runX, coreTop, 1, coreLen, coreCol, coreLight, coreIn);
                    }
                    if (wideLen > 0) {
                        px(vc, ctx, r, runX + 1, wideTop, 1, wideLen, wideCol, wideLight, wideIn);
                    }
                    coreLen = 0;
                    wideLen = 0;
                    runX = x;
                }
                if (wider && !bead) {
                    px(vc, ctx, r, x - 1, y, 1, 1, color(1, a * 0.9), l, in);
                }
                if (bead) {
                    px(vc, ctx, r, x - 1, y, 1, 1, color(1, a), l, in);
                }
                int cc = bead ? color(ph == 0 ? 4 : 3, a) : color(core, a);
                if (coreLen > 0 && (cc != coreCol || l != coreLight || in != coreIn)) {
                    px(vc, ctx, r, x, coreTop, 1, coreLen, coreCol, coreLight, coreIn);
                    coreLen = 0;
                }
                if (coreLen == 0) {
                    coreTop = y;
                    coreCol = cc;
                    coreLight = l;
                    coreIn = in;
                }
                coreLen++;
                if (bead || wide) {
                    int sc = bead ? color(2, a) : color(ws > 0.5 ? 2 : 1, a * 0.95);
                    if (wideLen > 0 && (sc != wideCol || l != wideLight || in != wideIn)) {
                        px(vc, ctx, r, x + 1, wideTop, 1, wideLen, wideCol, wideLight, wideIn);
                        wideLen = 0;
                    }
                    if (wideLen == 0) {
                        wideTop = y;
                        wideCol = sc;
                        wideLight = l;
                        wideIn = in;
                    }
                    wideLen++;
                } else if (wideLen > 0) {
                    px(vc, ctx, r, x + 1, wideTop, 1, wideLen, wideCol, wideLight, wideIn);
                    wideLen = 0;
                }
                if (row > 0 && col[row - 1] != x) {
                    px(vc, ctx, r, Math.min(x, col[row - 1]), y + 1, 1, 1, color(2, a), l, r.inset[row - 1]);
                }
                if (row > 0 && r.inset[row - 1] != in) {
                    step(vc, ctx, r, x, y, r.inset[row - 1], in, color(2, a), l);
                }
            }
            if (coreLen > 0) {
                px(vc, ctx, r, runX, coreTop, 1, coreLen, coreCol, coreLight, coreIn);
            }
            if (wideLen > 0) {
                px(vc, ctx, r, runX + 1, wideTop, 1, wideLen, wideCol, wideLight, wideIn);
            }
        }

        private void simpleRivulet(VertexConsumer vc, FxContext ctx, Face r, double ws) {
            double a = (215 + 35 * ws) * ctx.fade * alpha;
            int topPx = (int) Math.round(r.seepTop * 16);
            int w = ws > 0.5 ? 3 : 2;
            int col = color(ws > 0.5 ? 3 : 2, a);
            if (!r.shaped) {
                px(vc, ctx, r, r.path[0], topPx, w, r.rows, col, r.light[0]);
                return;
            }
            int start = 0;
            for (int row = 1; row <= r.rows; row++) {
                if (row == r.rows || r.inset[row] != r.inset[start]) {
                    px(vc, ctx, r, r.path[0], topPx - start, w, row - start, col, r.light[Math.min(r.light.length - 1, start / 16)], r.inset[start]);
                    if (row < r.rows) {
                        step(vc, ctx, r, r.path[0], topPx - row, r.inset[start], r.inset[row], col, r.light[Math.min(r.light.length - 1, row / 16)]);
                    }
                    start = row;
                }
            }
        }

        private void step(VertexConsumer vc, FxContext ctx, Face f, int uPx, int yPx, int from, int to, int col, int light) {
            double a = f.plane - f.sign * from / 16.0;
            double b = f.plane - f.sign * to / 16.0;
            double n0 = Math.min(a, b);
            double n1 = Math.max(a, b);
            boolean onTop = to < from;
            double y = yPx / 16.0 + (onTop ? OFF : -OFF);
            double u0 = uPx / 16.0;
            double u1 = (uPx + 2) / 16.0;
            if (f.xFace) {
                flat(vc, ctx, n0, n1, y, u0, u1, col, light, onTop ? 1 : -1);
            } else {
                flat(vc, ctx, u0, u1, y, n0, n1, col, light, onTop ? 1 : -1);
            }
        }

        private static int inCell(Face f, int u) {
            return Math.max(f.cellMin, Math.min(f.cellMin + 15, u));
        }

        private void ringJoint(VertexConsumer vc, FxContext ctx, Face r, int colPx, int lw, long seed, double a, int light) {
            int planePx = (int) Math.round(r.plane * 16) - r.sign * r.inset[0];
            double y = r.seepTop - OFF;
            for (int k = -lw; k <= lw; k++) {
                int depth = Math.abs(k) <= 1 ? 2 : (FxNoise.hash(k + 80, seed) < 0.5 ? 1 : 0);
                for (int d = 0; d < depth; d++) {
                    int out = planePx + (r.sign > 0 ? d : -d - 1);
                    int u = inCell(r, colPx + k);
                    int col = color(d == 0 ? 1 : 0, a);
                    if (r.xFace) {
                        flat(vc, ctx, out / 16.0, (out + 1) / 16.0, y, u / 16.0, (u + 1) / 16.0, col, light, -1);
                    } else {
                        flat(vc, ctx, u / 16.0, (u + 1) / 16.0, y, out / 16.0, (out + 1) / 16.0, col, light, -1);
                    }
                }
            }
        }

        private void stain(VertexConsumer vc, FxContext ctx, Face st, double ws) {
            long seed = (long) (st.stainPhase * 1e9) + 7;
            int by = (int) Math.floor(st.stainY - 0.001);
            int l = st.stainLight;
            double a = (200 + 30 * ws) * ctx.fade * alpha;
            int y = (int) Math.round(st.stainY * 16);
            int u0 = (int) Math.floor(st.stainU * 16);
            int cellMin = (st.xFace ? st.cellZ : st.cellX) * 16;
            int len = 2 + (int) (st.stainLen * 4);
            u0 = Math.max(cellMin + 4, Math.min(cellMin + 12 - len, u0));
            for (int k = 0; k < len; k++) {
                px(vc, ctx, st, u0 + k, y, 1, 1, color(1, a * (k == 0 || k == len - 1 ? 0.75 : 1)), l, st.stainInset);
            }
            int dripU = u0 + len / 2;
            int dl = 2 + (int) (FxNoise.hash(10, seed) * 4);
            for (int k = 1; k <= dl; k++) {
                px(vc, ctx, st, dripU, y - k, 1, 1, color(k == dl ? 1 : 2, a * (k == dl ? 0.75 : 1)), l, st.stainInset);
            }
        }

        private void patch(VertexConsumer vc, FxContext ctx, FxColumn c, double ws) {
            double a = (220 + 30 * ws) * ctx.fade * alpha;
            int light = c.lightAt(c.top - 0.5);
            int x = (int) Math.floor(c.x * 16);
            int z = (int) Math.floor(c.z * 16);
            long seed = c.seed;
            underPixel(vc, ctx, c.top, x, z, color(3, a), light);
            underPixel(vc, ctx, c.top, x - 1, z, color(1, a), light);
            underPixel(vc, ctx, c.top, x + 1, z, color(2, a), light);
            underPixel(vc, ctx, c.top, x, z - 1, color(2, a), light);
            underPixel(vc, ctx, c.top, x, z + 1, color(1, a), light);
            if (FxNoise.hash(1, seed) < 0.6) {
                underPixel(vc, ctx, c.top, x + 1, z + 1, color(1, a * 0.8), light);
            }
            if (FxNoise.hash(2, seed) < 0.5) {
                underPixel(vc, ctx, c.top, x - 1, z - 1, color(1, a * 0.8), light);
            }
            if (ws > 0.4) {
                underPixel(vc, ctx, c.top, x + 2, z, color(1, a * 0.8), light);
                underPixel(vc, ctx, c.top, x, z - 2, color(1, a * 0.8), light);
            }
        }

        private void px(VertexConsumer vc, FxContext ctx, Face f, int uPx, int yPx, int wPx, int hPx, int col, int light) {
            px(vc, ctx, f, uPx, yPx, wPx, hPx, col, light, 0);
        }

        private void px(VertexConsumer vc, FxContext ctx, Face f, int uPx, int yPx, int wPx, int hPx, int col, int light, int inset) {
            if (glow) {
                light = LightTexture.FULL_BRIGHT;
            }
            double u0 = uPx / 16.0;
            double u1 = (uPx + wPx) / 16.0;
            double y1 = yPx / 16.0;
            double y0 = (yPx - hPx) / 16.0;
            double p = f.plane - f.sign * inset / 16.0 + f.sign * OFF;
            if (f.xFace) {
                FxMesh.quad(vc, ctx.pose, p, y1, u0, 0, 0, p, y1, u1, 1, 0, p, y0, u1, 1, 1, p, y0, u0, 0, 1, col, light, f.sign, 0, 0);
            } else {
                FxMesh.quad(vc, ctx.pose, u0, y1, p, 0, 0, u1, y1, p, 1, 0, u1, y0, p, 1, 1, u0, y0, p, 0, 1, col, light, 0, 0, f.sign);
            }
        }

        private void under(VertexConsumer vc, FxContext ctx, boolean xFace, double bottom, int across, int u, int col, int light) {
            if (xFace) {
                underPixel(vc, ctx, bottom, across, u, col, light);
            } else {
                underPixel(vc, ctx, bottom, u, across, col, light);
            }
        }

        private void underPixel(VertexConsumer vc, FxContext ctx, double bottom, int xPx, int zPx, int col, int light) {
            flat(vc, ctx, xPx / 16.0, (xPx + 1) / 16.0, bottom - OFF, zPx / 16.0, (zPx + 1) / 16.0, col, light, -1);
        }

        private void flat(VertexConsumer vc, FxContext ctx, double x0, double x1, double y, double z0, double z1, int col, int light, int ny) {
            if (glow) {
                light = LightTexture.FULL_BRIGHT;
            }
            FxMesh.quad(vc, ctx.pose, x0, y, z0, 0, 0, x1, y, z0, 1, 0, x1, y, z1, 1, 1, x0, y, z1, 0, 1, col, light, 0, ny, 0);
        }
    }
}
