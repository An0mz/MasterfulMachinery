package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class BeamLayer implements FxLayer {
    private static final int MAX_SEGMENTS = 64;

    private final ResourceLocation texture;
    private final String from;
    private final String to;
    private final FxValue width;
    private final FxValue alpha;
    private final int color;
    private final boolean portColor;
    private final boolean additive;
    private final boolean glow;
    private final int segments;
    private final double arc;
    private final double jitter;
    private final double jitterRate;
    private final double scroll;
    private final double textureLength;
    private final double pulse;
    private final double pulseSpeed;
    private final double flicker;
    private final double taper;

    private BeamLayer(JsonObject json) {
        texture = FxTexture.required(json, "beam");
        from = json.has("from") ? json.get("from").getAsString() : "from";
        to = json.has("to") ? json.get("to").getAsString() : "to";
        width = FxValue.parse(json, "width", 0.15);
        alpha = FxValue.parse(json, "alpha", 1.0);
        color = FxColor.parse(json, "color", 0xFFFFFF);
        portColor = FxColor.port(json);
        String blend = json.has("blend") ? json.get("blend").getAsString() : "additive";
        if (!blend.equals("translucent") && !blend.equals("additive")) {
            throw new IllegalArgumentException("beam 'blend' must be translucent or additive");
        }
        additive = blend.equals("additive");
        glow = !json.has("glow") || json.get("glow").getAsBoolean();
        segments = Math.max(1, Math.min(MAX_SEGMENTS, json.has("segments") ? json.get("segments").getAsInt() : 16));
        arc = number(json, "arc", 0);
        jitter = Math.max(0, number(json, "jitter", 0));
        jitterRate = Math.max(0, number(json, "jitter_rate", 12));
        scroll = number(json, "scroll", 1.0);
        textureLength = Math.max(0.01, number(json, "texture_length", 1.0));
        pulse = number(json, "pulse", 0);
        pulseSpeed = number(json, "pulse_speed", 2.0);
        flicker = FxNoise.clamp(number(json, "flicker", 0));
        taper = FxNoise.clamp(number(json, "taper", 0));
    }

    public static BeamLayer parse(JsonObject json) {
        return new BeamLayer(json);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    @Override
    @Nullable
    public State create(FxShared shared) {
        double[] a = shared.points(from);
        double[] b = shared.points(to);
        if (a == null || b == null || a.length < 3 || b.length < 3) {
            Ref.LOG.warn("Beam layer: needs non-empty point anchors '{}' and '{}'", from, to);
            return null;
        }
        int n = a.length / 3;
        int m = b.length / 3;
        int count = Math.max(n, m);
        Beam[] beams = new Beam[count];
        for (int i = 0; i < count; i++) {
            int ia = (i % n) * 3;
            int ib = (i % m) * 3;
            beams[i] = new Beam(a[ia], a[ia + 1], a[ia + 2], b[ib], b[ib + 1], b[ib + 2], shared.seed() + i * 977L);
        }
        return new Beams(shared, beams);
    }

    private final class Beam {
        final double ax;
        final double ay;
        final double az;
        final double dx;
        final double dy;
        final double dz;
        final double length;
        final double p1x;
        final double p1y;
        final double p1z;
        final double p2x;
        final double p2y;
        final double p2z;
        final long seed;
        final double phase;
        final double[] px = new double[MAX_SEGMENTS + 1];
        final double[] py = new double[MAX_SEGMENTS + 1];
        final double[] pz = new double[MAX_SEGMENTS + 1];
        final double[] offA = new double[MAX_SEGMENTS + 1];
        final double[] offB = new double[MAX_SEGMENTS + 1];
        final double[] sx = new double[MAX_SEGMENTS + 1];
        final double[] sy = new double[MAX_SEGMENTS + 1];
        final double[] sz = new double[MAX_SEGMENTS + 1];
        double nextJitter;

        Beam(double ax, double ay, double az, double bx, double by, double bz, long seed) {
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.dx = bx - ax;
            this.dy = by - ay;
            this.dz = bz - az;
            this.length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            this.seed = seed;
            this.phase = FxNoise.hash(3, seed) * Math.PI * 2;
            double ux = length > 0 ? dx / length : 0;
            double uy = length > 0 ? dy / length : 1;
            double uz = length > 0 ? dz / length : 0;
            double rx = Math.abs(uy) < 0.9 ? 0 : 1;
            double ry = Math.abs(uy) < 0.9 ? 1 : 0;
            double c1x = uy * 0 - uz * ry;
            double c1y = uz * rx - ux * 0;
            double c1z = ux * ry - uy * rx;
            double l1 = Math.sqrt(c1x * c1x + c1y * c1y + c1z * c1z);
            c1x /= l1;
            c1y /= l1;
            c1z /= l1;
            this.p1x = c1x;
            this.p1y = c1y;
            this.p1z = c1z;
            this.p2x = uy * c1z - uz * c1y;
            this.p2y = uz * c1x - ux * c1z;
            this.p2z = ux * c1y - uy * c1x;
        }
    }

    private final class Beams implements State {
        private final FxShared shared;
        private final Beam[] beams;
        private RenderType renderType;

        @Override
        public double reach() {
            return Math.abs(arc) + jitter * 1.5 + Math.max(width.idle(), width.working()) * (1 + Math.abs(pulse));
        }

        Beams(FxShared shared, Beam[] beams) {
            this.shared = shared;
            this.beams = beams;
        }

        @Override
        public void reset() {
            for (Beam beam : beams) {
                beam.nextJitter = 0;
            }
        }

        @Override
        public void update(FxContext ctx) {
            if (jitter <= 0) {
                return;
            }
            for (Beam beam : beams) {
                if (ctx.time < beam.nextJitter) {
                    continue;
                }
                beam.nextJitter = ctx.time + (jitterRate > 0 ? 1.0 / jitterRate : 1e9);
                for (int k = 0; k <= segments; k++) {
                    beam.offA[k] = shared.random.nextDouble() * 2 - 1;
                    beam.offB[k] = shared.random.nextDouble() * 2 - 1;
                }
            }
        }

        @Override
        public void render(FxContext ctx) {
            double ws = ctx.intensity.now();
            double w = width.at(ws);
            double a = alpha.at(ws) * ctx.fade;
            if (w <= 0.001 || a <= 0.005) {
                return;
            }
            if (renderType == null) {
                renderType = additive ? RenderType.eyes(texture) : RenderType.entityTranslucent(texture);
            }
            VertexConsumer vc = ctx.buffers.getBuffer(renderType);
            double base = Math.floor(ctx.time * scroll / textureLength);
            for (Beam beam : beams) {
                if (beam.length < 1e-3) {
                    continue;
                }
                double flick = flicker > 0 ? 1 - flicker * FxNoise.value(ctx.time * 18 + beam.phase * 10, beam.seed) : 1;
                double bw = Math.max(0, w * (1 + pulse * Math.sin(ctx.time * pulseSpeed * Math.PI * 2 + beam.phase)));
                int light = glow || additive ? LightTexture.FULL_BRIGHT
                        : ctx.light(beam.ax + beam.dx * 0.5, beam.ay + beam.dy * 0.5, beam.az + beam.dz * 0.5);
                int al = (int) (255 * FxNoise.clamp(a * flick));
                int tint = FxColor.pick(portColor, color, ctx);
                int col = additive ? FxColor.additive(al, tint) : FxColor.argb(al, tint, 1.0);
                for (int k = 0; k <= segments; k++) {
                    double t = (double) k / segments;
                    double sag = arc * 4 * t * (1 - t);
                    double amp = jitter * Math.sin(Math.PI * t);
                    beam.px[k] = beam.ax + beam.dx * t + (beam.p1x * beam.offA[k] + beam.p2x * beam.offB[k]) * amp;
                    beam.py[k] = beam.ay + beam.dy * t + sag + (beam.p1y * beam.offA[k] + beam.p2y * beam.offB[k]) * amp;
                    beam.pz[k] = beam.az + beam.dz * t + (beam.p1z * beam.offA[k] + beam.p2z * beam.offB[k]) * amp;
                }
                double lastX = 0, lastY = 0, lastZ = 0;
                boolean haveSide = false;
                for (int k = 0; k <= segments; k++) {
                    int prev = Math.max(0, k - 1);
                    int next = Math.min(segments, k + 1);
                    double tx = beam.px[next] - beam.px[prev];
                    double ty = beam.py[next] - beam.py[prev];
                    double tz = beam.pz[next] - beam.pz[prev];
                    double cx = beam.px[k] - ctx.cameraX;
                    double cy = beam.py[k] - ctx.cameraY;
                    double cz = beam.pz[k] - ctx.cameraZ;
                    double nx = ty * cz - tz * cy;
                    double ny = tz * cx - tx * cz;
                    double nz = tx * cy - ty * cx;
                    double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (nl < 1e-9) {
                        nx = lastX;
                        ny = lastY;
                        nz = lastZ;
                    } else {
                        nx /= nl;
                        ny /= nl;
                        nz /= nl;
                        if (haveSide && nx * lastX + ny * lastY + nz * lastZ < 0) {
                            nx = -nx;
                            ny = -ny;
                            nz = -nz;
                        }
                        lastX = nx;
                        lastY = ny;
                        lastZ = nz;
                        haveSide = true;
                    }
                    beam.sx[k] = nx;
                    beam.sy[k] = ny;
                    beam.sz[k] = nz;
                }
                if (!haveSide) {
                    continue;
                }
                for (int k = 0; k < segments; k++) {
                    double t0 = (double) k / segments;
                    double t1 = (double) (k + 1) / segments;
                    double h0 = bw * 0.5 * (1 - taper * Math.abs(t0 * 2 - 1));
                    double h1 = bw * 0.5 * (1 - taper * Math.abs(t1 * 2 - 1));
                    double x0 = beam.px[k], y0 = beam.py[k], z0 = beam.pz[k];
                    double x1 = beam.px[k + 1], y1 = beam.py[k + 1], z1 = beam.pz[k + 1];
                    double ax0 = beam.sx[k] * h0, ay0 = beam.sy[k] * h0, az0 = beam.sz[k] * h0;
                    double ax1 = beam.sx[k + 1] * h1, ay1 = beam.sy[k + 1] * h1, az1 = beam.sz[k + 1] * h1;
                    float v0 = (float) (t0 * beam.length / textureLength - ctx.time * scroll / textureLength + base);
                    float v1 = (float) (t1 * beam.length / textureLength - ctx.time * scroll / textureLength + base);
                    FxMesh.facing(vc, ctx.pose,
                            x0 + ax0, y0 + ay0, z0 + az0, 0, v0,
                            x1 + ax1, y1 + ay1, z1 + az1, 0, v1,
                            x1 - ax1, y1 - ay1, z1 - az1, 1, v1,
                            x0 - ax0, y0 - ay0, z0 - az0, 1, v0,
                            col, light, ctx.cameraX - (x0 + x1) * 0.5, ctx.cameraY - (y0 + y1) * 0.5, ctx.cameraZ - (z0 + z1) * 0.5, 0, 1, 0);
                }
            }
        }
    }
}
