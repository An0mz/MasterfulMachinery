package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class StreamLayer implements FxLayer {
    private static final int MAX_SEGMENTS = 44;
    private static final int MAX_FALL = 64;
    private static final double BREAK_MIN_INTENSITY = 0.3;

    private final String from;
    private final String to;
    private final ResourceLocation texture;
    private final int color;
    private final boolean portColor;
    private final boolean glow;
    private final FxValue alpha;
    private final double radius;
    private final double gravity;
    private final double startSpeed;
    private final double sway;
    private final double bulge;
    private final double flare;
    private final boolean breakup;
    private final double breakupStart;
    private final double breakupLength;
    private final double gaps;
    private final double noise;
    private final double trickle;
    private final double textureSpeed;
    private final double textureWidth;
    private final boolean hideWhenBlocked;
    private final boolean needsBlockAbove;
    private final boolean droplets;

    private final int[] skip;

    private StreamLayer(JsonObject json) {
        skip = FxSkip.parse(json);
        from = json.has("from") ? json.get("from").getAsString() : "drips";
        to = json.has("to") ? json.get("to").getAsString() : "floor";
        texture = FxTexture.required(json, "stream");
        color = FxColor.parse(json, "color", 0xFFFFFF);
        portColor = FxColor.port(json);
        glow = json.has("glow") && json.get("glow").getAsBoolean();
        alpha = FxValue.parse(json, "alpha", 0.925);
        radius = number(json, "radius", 0.085);
        gravity = number(json, "gravity", 9.81);
        startSpeed = number(json, "start_speed", 0.55);
        sway = number(json, "sway", 0.02);
        bulge = number(json, "bulge", 0.22);
        flare = number(json, "flare", 0.55);
        breakup = !json.has("breakup") || json.get("breakup").getAsBoolean();
        breakupStart = number(json, "breakup_start", 2.2);
        breakupLength = number(json, "breakup_length", 3.6);
        gaps = number(json, "gaps", 0.1);
        noise = number(json, "noise", 1.0);
        trickle = number(json, "trickle", 0.2);
        textureSpeed = number(json, "texture_speed", 7.0 / 64.0);
        textureWidth = number(json, "texture_width", 0.25);
        hideWhenBlocked = !json.has("hide_when_blocked") || json.get("hide_when_blocked").getAsBoolean();
        needsBlockAbove = !json.has("needs_block_above") || json.get("needs_block_above").getAsBoolean();
        droplets = !json.has("droplets") || json.get("droplets").getAsBoolean();
        if (gravity <= 0 || startSpeed <= 0 || radius < 0) {
            throw new IllegalArgumentException("stream needs gravity and start_speed above 0 and radius of 0 or more");
        }
    }

    public static StreamLayer parse(JsonObject json) {
        return new StreamLayer(json);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    @Override
    @Nullable
    public State create(FxShared shared) {
        FxColumn[] columns = FxSkip.keep(shared.columns(from, to), skip);
        if (columns == null) {
            return null;
        }
        Track[] tracks = new Track[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].hasLanding()) {
                tracks[i] = new Track(columns[i]);
            }
        }
        return new StreamState(shared, tracks, columns);
    }

    boolean visible(FxColumn c) {
        return (!needsBlockAbove || c.blockAbove) && (!hideWhenBlocked || c.pathClear);
    }

    double fallTime(double d) {
        return (Math.sqrt(startSpeed * startSpeed + 2 * gravity * d) - startSpeed) / gravity;
    }

    double speedAt(double d) {
        return Math.sqrt(startSpeed * startSpeed + 2 * gravity * d);
    }

    double flow(FxIntensity intensity, FxColumn c, double te) {
        double wh = intensity.at(te);
        double n = 0.6 + 0.32 * FxNoise.value(te * 0.55, c.seed) + 0.16 * (FxNoise.value(te * 2.3, c.seed + 1) - 0.5);
        n = 0.76 + noise * (n - 0.76);
        double gate = gaps > 0 ? FxNoise.clamp((FxNoise.value(te * 0.8, c.seed + 2) - gaps) / 0.09) : 1;
        double work = wh * n * gate;
        double rare = FxNoise.clamp((FxNoise.value(te * 0.085, c.seed + 3) - 0.84) / 0.05);
        return work + (1 - wh) * trickle * rare;
    }

    double streamRadius(FxIntensity intensity, FxColumn c, double d, double te) {
        return streamRadius(intensity, c, d, te, Math.pow(startSpeed / speedAt(d), 0.18), Math.pow(d / c.fall, 1.2));
    }

    double streamRadius(FxIntensity intensity, FxColumn c, double d, double te, double thin, double depthShape) {
        double q = flow(intensity, c, te);
        double shape = Math.sqrt(Math.max(0, (q - 0.07) / 0.93));
        if (shape <= 0) {
            return 0;
        }
        double vari = 1 + bulge * depthShape * Math.sin(te * Math.PI * 2 * 5.3 + c.phase);
        double flareScale = d < 0.14 ? 1 + flare * (1 - d / 0.14) : 1;
        if (breakup) {
            double breakAt = breakupStart + breakupLength * Math.min(1, q) + 1.2 * FxNoise.value(te * 0.3, c.seed + 9);
            double k = FxNoise.clamp((d - breakAt) / 1.6);
            if (k > 0) {
                double pulse = 0.5 + 0.5 * Math.sin(te * Math.PI * 2 * 13.0 + c.phase * 3.0 + 0.8 * Math.sin(te * 3.1));
                double bead = FxNoise.clamp((pulse - 0.28) / 0.3);
                bead = bead * bead * (3 - 2 * bead) * 1.22;
                vari *= (1 - k) + k * bead;
            }
        }
        return Math.max(0.0, radius * shape * thin * vari * flareScale);
    }

    double centerX(FxColumn c, double d, double te, double time) {
        return sideX(c, d / c.fall, te, sway * 0.3 * Math.sin(time * 0.37 + c.phase));
    }

    double centerZ(FxColumn c, double d, double te, double time) {
        return sideZ(c, d / c.fall, te, sway * 0.3 * Math.sin(time * 0.37 + c.phase));
    }

    private double sideX(FxColumn c, double k, double te, double drift) {
        double amp = 0.003 + sway * k * k;
        double wx = 0.6 * Math.sin(te * 2.7 + c.phase) + 0.4 * Math.sin(te * 6.1 + c.phase * 1.7);
        return c.x + amp * wx + drift * k;
    }

    private double sideZ(FxColumn c, double k, double te, double drift) {
        double amp = 0.003 + sway * k * k;
        double wz = 0.6 * Math.cos(te * 2.3 + c.phase * 0.6) + 0.4 * Math.sin(te * 5.3 + c.phase * 2.3);
        return c.z + amp * wz + drift * k * 0.6;
    }

    private static final class Track {
        final FxColumn column;
        final double[] px = new double[MAX_SEGMENTS + 1];
        final double[] py = new double[MAX_SEGMENTS + 1];
        final double[] pz = new double[MAX_SEGMENTS + 1];
        final double[] pr = new double[MAX_SEGMENTS + 1];
        final double[] pv = new double[MAX_SEGMENTS + 1];
        final double[] depth = new double[MAX_SEGMENTS + 1];
        final double[] delay = new double[MAX_SEGMENTS + 1];
        final double[] thin = new double[MAX_SEGMENTS + 1];
        final double[] depthShape = new double[MAX_SEGMENTS + 1];
        final double[] along = new double[MAX_SEGMENTS + 1];
        int cachedSegments = -1;

        Track(FxColumn column) {
            this.column = column;
        }
    }

    private final class StreamState implements State {
        private final FxShared shared;
        private final Track[] tracks;
        private final FxColumn[] columns;
        private final int[] sideColor = new int[6];
        private RenderType renderType;

        StreamState(FxShared shared, Track[] tracks, FxColumn[] columns) {
            this.shared = shared;
            this.tracks = tracks;
            this.columns = columns;
        }

        @Override
        @Nullable
        public String problem(double intensity) {
            String problem = FxColumn.problem(columns, needsBlockAbove, hideWhenBlocked, false);
            return problem != null ? problem : alpha.at(intensity) <= 0 ? "hidden_now" : null;
        }

        @Override
        public void update(FxContext ctx) {
            double ws = ctx.intensity.now();
            for (Track track : tracks) {
                if (track == null) {
                    continue;
                }
                FxColumn c = track.column;
                boolean visible = visible(c);
                c.inflow = visible ? flow(ctx.intensity, c, ctx.time - fallTime(c.fall)) : 0;
                c.rootRadius = visible ? streamRadius(ctx.intensity, c, 0.02, ctx.time - fallTime(0.02)) : 0;
                if (!droplets || !visible || shared.drops == null || ws <= BREAK_MIN_INTENSITY || ctx.time < c.nextBreak) {
                    continue;
                }
                double d = c.fall * (0.17 + 0.61 * shared.random.nextDouble());
                double te = ctx.time - fallTime(d);
                if (flow(ctx.intensity, c, te) > 0.25) {
                    double angle = shared.random.nextDouble() * Math.PI * 2;
                    double sideSpeed = 0.05 + 0.1 * shared.random.nextDouble();
                    double r = streamRadius(ctx.intensity, c, d, te);
                    shared.drops.spawn(ctx.time, centerX(c, d, te, ctx.time) + Math.cos(angle) * r, c.top - d,
                            centerZ(c, d, te, ctx.time) + Math.sin(angle) * r, speedAt(d) * 0.92,
                            Math.cos(angle) * sideSpeed, Math.sin(angle) * sideSpeed, 0.018 + 0.012 * shared.random.nextDouble(), c);
                }
                c.nextBreak = ctx.time + (0.45 + 1.9 * shared.random.nextDouble()) / Math.max(ws, BREAK_MIN_INTENSITY);
            }
        }

        @Override
        public void render(FxContext ctx) {
            VertexConsumer vc = null;
            int tint = FxColor.pick(portColor, color, ctx);
            int segments = ctx.lod == 0 ? 44 : ctx.lod == 1 ? 28 : 18;
            int sides = ctx.lod == 0 ? 6 : 4;
            int a = (int) (255 * FxNoise.clamp(alpha.at(ctx.intensity.now())) * ctx.fade);
            double[] cos = FxTrig.cos(sides);
            double[] sin = FxTrig.sin(sides);
            for (int s = 0; s < sides; s++) {
                double nx = (cos[s] + cos[s + 1]) * 0.5;
                double nz = (sin[s] + sin[s + 1]) * 0.5;
                sideColor[s] = FxColor.argb(a, tint, 0.86 + 0.14 * (nx * 0.6 - nz * 0.8));
            }
            for (Track track : tracks) {
                if (track == null || !visible(track.column)) {
                    continue;
                }
                FxColumn c = track.column;
                if (track.cachedSegments != segments) {
                    track.cachedSegments = segments;
                    for (int k = 0; k <= segments; k++) {
                        double d = Math.pow((double) k / segments, 1.35) * (c.fall - 0.012);
                        track.depth[k] = d;
                        track.delay[k] = fallTime(d);
                        track.thin[k] = Math.pow(startSpeed / speedAt(d), 0.18);
                        track.depthShape[k] = Math.pow(d / c.fall, 1.2);
                        track.along[k] = d / c.fall;
                    }
                }
                double scrollBase = Math.floor(ctx.time * textureSpeed);
                double drift = sway * 0.3 * Math.sin(ctx.time * 0.37 + c.phase);
                boolean any = false;
                for (int k = 0; k <= segments; k++) {
                    double d = track.depth[k];
                    double te = ctx.time - track.delay[k];
                    track.px[k] = sideX(c, track.along[k], te, drift);
                    track.pz[k] = sideZ(c, track.along[k], te, drift);
                    track.py[k] = c.top - d;
                    track.pr[k] = streamRadius(ctx.intensity, c, d, te, track.thin[k], track.depthShape[k]);
                    track.pv[k] = te * textureSpeed - scrollBase;
                    if (track.pr[k] > 0.004) {
                        any = true;
                    }
                }
                if (!any) {
                    continue;
                }
                if (vc == null) {
                    if (renderType == null) {
                        renderType = RenderType.entityTranslucentCull(texture);
                    }
                    vc = ctx.buffers.getBuffer(renderType);
                }
                double uOff = (c.index * 4 % 12) / 16.0;
                for (int k = 0; k < segments; k++) {
                    double r0 = track.pr[k];
                    double r1 = track.pr[k + 1];
                    if (r0 < 0.004 && r1 < 0.004) {
                        continue;
                    }
                    int light = glow ? LightTexture.FULL_BRIGHT : c.lightAt((track.py[k] + track.py[k + 1]) * 0.5);
                    for (int s = 0; s < sides; s++) {
                        double c0 = cos[s], s0 = sin[s], c1 = cos[s + 1], s1 = sin[s + 1];
                        float u0 = (float) (uOff + s * textureWidth / sides);
                        float u1 = (float) (uOff + (s + 1) * textureWidth / sides);
                        double nx = (c0 + c1) * 0.5;
                        double nz = (s0 + s1) * 0.5;
                        int col = sideColor[s];
                        FxMesh.quad(vc, ctx.pose,
                                track.px[k] + c0 * r0, track.py[k], track.pz[k] + s0 * r0, u0, (float) track.pv[k],
                                track.px[k + 1] + c0 * r1, track.py[k + 1], track.pz[k + 1] + s0 * r1, u0, (float) track.pv[k + 1],
                                track.px[k + 1] + c1 * r1, track.py[k + 1], track.pz[k + 1] + s1 * r1, u1, (float) track.pv[k + 1],
                                track.px[k] + c1 * r0, track.py[k], track.pz[k] + s1 * r0, u1, (float) track.pv[k],
                                col, light, nx, 0, nz);
                    }
                }
            }
        }
    }
}
