package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class GlowLayer implements FxLayer {
    private final ResourceLocation texture;
    private final String at;
    private final boolean halo;
    private final boolean outerOnly;
    private final Set<Direction> faces;
    private final double offset;
    private final FxValue size;
    private final FxValue alpha;
    private final int color;
    private final boolean portColor;
    private final boolean additive;
    private final double pulse;
    private final double pulseSpeed;
    private final double flicker;
    private final int frames;
    private final double frameRate;

    private GlowLayer(JsonObject json) {
        texture = FxTexture.required(json, "glow");
        at = json.has("at") ? json.get("at").getAsString() : "glow";
        String mode = json.has("mode") ? json.get("mode").getAsString() : "faces";
        if (!mode.equals("faces") && !mode.equals("halo")) {
            throw new IllegalArgumentException("glow 'mode' must be faces or halo");
        }
        halo = mode.equals("halo");
        faces = EnumSet.allOf(Direction.class);
        boolean outer = true;
        if (json.has("faces")) {
            JsonElement el = json.get("faces");
            if (el.isJsonArray()) {
                faces.clear();
                for (JsonElement d : el.getAsJsonArray()) {
                    Direction dir = Direction.byName(d.getAsString().toLowerCase(Locale.ROOT));
                    if (dir == null) {
                        throw new IllegalArgumentException("glow 'faces' has an unknown direction: " + d.getAsString());
                    }
                    faces.add(dir);
                }
            } else {
                String value = el.getAsString();
                if (!value.equals("outer") && !value.equals("all")) {
                    throw new IllegalArgumentException("glow 'faces' must be outer, all or a list of directions");
                }
                outer = value.equals("outer");
            }
        }
        outerOnly = outer;
        offset = json.has("offset") ? json.get("offset").getAsDouble() : 0.01;
        size = FxValue.parse(json, "size", 1.0);
        alpha = FxValue.parse(json, "alpha", 1.0);
        color = FxColor.parse(json, "color", 0xFFFFFF);
        portColor = FxColor.port(json);
        String blend = json.has("blend") ? json.get("blend").getAsString() : "additive";
        if (!blend.equals("translucent") && !blend.equals("additive")) {
            throw new IllegalArgumentException("glow 'blend' must be translucent or additive");
        }
        additive = blend.equals("additive");
        pulse = json.has("pulse") ? json.get("pulse").getAsDouble() : 0;
        pulseSpeed = json.has("pulse_speed") ? json.get("pulse_speed").getAsDouble() : 1;
        flicker = FxNoise.clamp(json.has("flicker") ? json.get("flicker").getAsDouble() : 0);
        frames = Math.max(1, json.has("frames") ? json.get("frames").getAsInt() : 1);
        frameRate = json.has("frame_rate") ? json.get("frame_rate").getAsDouble() : 10;
    }

    public static GlowLayer parse(JsonObject json) {
        return new GlowLayer(json);
    }

    @Override
    @Nullable
    public State create(FxShared shared) {
        if (halo) {
            double[] points = shared.points(at);
            if (points == null || points.length < 3) {
                Ref.LOG.warn("Glow layer: halo mode needs a non-empty point anchor named '{}'", at);
                return null;
            }
            return new Halos(points, shared.seed());
        }
        long[] cells = shared.area(at);
        if (cells == null || cells.length == 0) {
            Ref.LOG.warn("Glow layer: faces mode needs a non-empty area anchor named '{}'", at);
            return null;
        }
        var set = new LongOpenHashSet(cells);
        var list = new ArrayList<double[]>();
        for (long cell : cells) {
            for (Direction listed : faces) {
                Direction dir = shared.rotation.rotate(listed);
                if (outerOnly && set.contains(BlockPos.offset(cell, dir))) {
                    continue;
                }
                list.add(face(BlockPos.getX(cell), BlockPos.getY(cell), BlockPos.getZ(cell), dir));
            }
        }
        if (list.isEmpty()) {
            Ref.LOG.warn("Glow layer: no faces to draw for '{}'", at);
            return null;
        }
        return new Faces(list.toArray(new double[0][]), shared.seed());
    }

    private double[] face(int x, int y, int z, Direction dir) {
        double o = offset;
        double x0 = x, x1 = x + 1, y0 = y, y1 = y + 1, z0 = z, z1 = z + 1;
        double[] q = switch (dir) {
            case UP -> new double[]{x0, y1 + o, z1, x1, y1 + o, z1, x1, y1 + o, z0, x0, y1 + o, z0};
            case DOWN -> new double[]{x0, y0 - o, z0, x1, y0 - o, z0, x1, y0 - o, z1, x0, y0 - o, z1};
            case NORTH -> new double[]{x1, y1, z0 - o, x1, y0, z0 - o, x0, y0, z0 - o, x0, y1, z0 - o};
            case SOUTH -> new double[]{x0, y1, z1 + o, x0, y0, z1 + o, x1, y0, z1 + o, x1, y1, z1 + o};
            case WEST -> new double[]{x0 - o, y1, z0, x0 - o, y0, z0, x0 - o, y0, z1, x0 - o, y1, z1};
            case EAST -> new double[]{x1 + o, y1, z1, x1 + o, y0, z1, x1 + o, y0, z0, x1 + o, y1, z0};
        };
        double[] result = new double[18];
        System.arraycopy(q, 0, result, 0, 12);
        result[12] = dir.getStepX();
        result[13] = dir.getStepY();
        result[14] = dir.getStepZ();
        result[15] = x + 0.5 + dir.getStepX() * (0.5 + o);
        result[16] = y + 0.5 + dir.getStepY() * (0.5 + o);
        result[17] = z + 0.5 + dir.getStepZ() * (0.5 + o);
        return result;
    }

    private int strength(FxContext ctx, double phase, long seed) {
        double ws = ctx.intensity.now();
        double a = alpha.at(ws) * ctx.fade;
        if (pulse != 0) {
            a *= 1 + pulse * Math.sin(ctx.time * pulseSpeed * Math.PI * 2 + phase);
        }
        if (flicker > 0) {
            a *= 1 - flicker * FxNoise.value(ctx.time * 14 + phase * 10, seed);
        }
        return (int) (255 * FxNoise.clamp(a));
    }

    private int color(int alphaByte, FxContext ctx) {
        int rgb = FxColor.pick(portColor, color, ctx);
        return additive ? FxColor.additive(alphaByte, rgb) : FxColor.argb(alphaByte, rgb, 1.0);
    }

    private float frameV(FxContext ctx, double phase) {
        if (frames <= 1) {
            return 0;
        }
        int frame = (int) Math.floorMod((long) Math.floor(ctx.time * frameRate + phase * frames), frames);
        return (float) frame / frames;
    }

    private RenderType renderType() {
        return additive ? RenderType.eyes(texture) : RenderType.entityTranslucent(texture);
    }

    private final class Faces implements State {
        private final double[][] quads;
        private final long seed;
        private RenderType type;

        Faces(double[][] quads, long seed) {
            this.quads = quads;
            this.seed = seed;
        }

        @Override
        public double reach() {
            return Math.abs(offset);
        }

        @Override
        public void render(FxContext ctx) {
            int a = strength(ctx, 0, seed);
            if (a < 2) {
                return;
            }
            if (type == null) {
                type = renderType();
            }
            VertexConsumer vc = ctx.buffers.getBuffer(type);
            int col = color(a, ctx);
            float v0 = frameV(ctx, 0);
            float v1 = v0 + 1f / frames;
            int light = LightTexture.FULL_BRIGHT;
            for (double[] q : quads) {
                double toCamera = (ctx.cameraX - q[15]) * q[12] + (ctx.cameraY - q[16]) * q[13] + (ctx.cameraZ - q[17]) * q[14];
                if (toCamera <= 0) {
                    continue;
                }
                FxMesh.quad(vc, ctx.pose,
                        q[0], q[1], q[2], 0, v0,
                        q[3], q[4], q[5], 0, v1,
                        q[6], q[7], q[8], 1, v1,
                        q[9], q[10], q[11], 1, v0,
                        col, light, q[12], q[13], q[14]);
            }
        }
    }

    private final class Halos implements State {
        private final double[] points;
        private final long seed;
        private RenderType type;

        Halos(double[] points, long seed) {
            this.points = points;
            this.seed = seed;
        }

        @Override
        public double reach() {
            return Math.max(size.idle(), size.working()) * (1 + Math.abs(pulse)) * 0.5;
        }

        @Override
        public void render(FxContext ctx) {
            double s = size.at(ctx.intensity.now()) * 0.5;
            if (s <= 0) {
                return;
            }
            if (type == null) {
                type = renderType();
            }
            VertexConsumer vc = null;
            for (int p = 0; p + 2 < points.length; p += 3) {
                double phase = FxNoise.hash(p, seed) * Math.PI * 2;
                int a = strength(ctx, phase, seed + p);
                if (a < 2) {
                    continue;
                }
                if (vc == null) {
                    vc = ctx.buffers.getBuffer(type);
                }
                double grow = pulse != 0 ? 1 + pulse * 0.5 * Math.sin(ctx.time * pulseSpeed * Math.PI * 2 + phase) : 1;
                double h = s * grow;
                double lx = ctx.leftX * h, ly = ctx.leftY * h, lz = ctx.leftZ * h;
                double ux = ctx.upX * h, uy = ctx.upY * h, uz = ctx.upZ * h;
                double x = points[p], y = points[p + 1], z = points[p + 2];
                float v0 = frameV(ctx, phase);
                float v1 = v0 + 1f / frames;
                FxMesh.facing(vc, ctx.pose,
                        x + lx + ux, y + ly + uy, z + lz + uz, 0, v0,
                        x + lx - ux, y + ly - uy, z + lz - uz, 0, v1,
                        x - lx - ux, y - ly - uy, z - lz - uz, 1, v1,
                        x - lx + ux, y - ly + uy, z - lz + uz, 1, v0,
                        color(a, ctx), LightTexture.FULL_BRIGHT, -ctx.lookX, -ctx.lookY, -ctx.lookZ, 0, 1, 0);
            }
        }
    }
}
