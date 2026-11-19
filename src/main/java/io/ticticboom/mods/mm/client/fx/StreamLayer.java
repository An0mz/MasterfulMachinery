package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class StreamLayer implements FxLayer {
    private static final int MAX_SEGMENTS = 44;
    private static final int MAX_FALL = 64;
    private static final double BREAK_MIN_INTENSITY = 0.3;
    private static final double LIGHT_INTERVAL = 0.25;

    private final String from;
    private final String to;
    private final ResourceLocation texture;
    private final int color;
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

    private StreamLayer(JsonObject json) {
        from = json.has("from") ? json.get("from").getAsString() : "drips";
        to = json.has("to") ? json.get("to").getAsString() : "floor";
        texture = FxTexture.required(json, "stream");
        color = FxColor.parse(json, "color", 0xFFFFFF);
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
        FxColumn[] columns = shared.columns(from, to);
        if (columns == null) {
            return null;
        }
        Track[] tracks = new Track[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].hasLanding()) {
                tracks[i] = new Track(columns[i]);
            }
        }
        return new StreamState(shared, tracks);
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
        double q = flow(intensity, c, te);
        double shape = Math.sqrt(Math.max(0, (q - 0.07) / 0.93));
        if (shape <= 0) {
            return 0;
        }
        double thin = Math.pow(startSpeed / speedAt(d), 0.18);
        double vari = 1 + bulge * Math.pow(d / c.fall, 1.2) * Math.sin(te * Math.PI * 2 * 5.3 + c.phase);
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
        double k = d / c.fall;
        double amp = 0.003 + sway * k * k;
        double wx = 0.6 * Math.sin(te * 2.7 + c.phase) + 0.4 * Math.sin(te * 6.1 + c.phase * 1.7);
        return c.x + amp * wx + sway * 0.3 * k * Math.sin(time * 0.37 + c.phase);
    }

    double centerZ(FxColumn c, double d, double te, double time) {
        double k = d / c.fall;
        double amp = 0.003 + sway * k * k;
        double wz = 0.6 * Math.cos(te * 2.3 + c.phase * 0.6) + 0.4 * Math.sin(te * 5.3 + c.phase * 2.3);
        return c.z + amp * wz + sway * 0.3 * k * Math.sin(time * 0.37 + c.phase) * 0.6;
    }

    private static final class Track {
        final FxColumn column;
        final double[] px = new double[MAX_SEGMENTS + 1];
        final double[] py = new double[MAX_SEGMENTS + 1];
        final double[] pz = new double[MAX_SEGMENTS + 1];
        final double[] pr = new double[MAX_SEGMENTS + 1];
        final double[] pv = new double[MAX_SEGMENTS + 1];
        final int[] light = new int[MAX_FALL + 2];
        double lightTime = -1e9;

        Track(FxColumn column) {
            this.column = column;
        }
    }

    private final class StreamState implements State {
        private final FxShared shared;
        private final Track[] tracks;

        StreamState(FxShared shared, Track[] tracks) {
            this.shared = shared;
            this.tracks = tracks;
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
            int segments = ctx.distance < 32 ? 44 : (ctx.distance < 60 ? 28 : 18);
            int sides = ctx.distance < 48 ? 6 : 4;
            int a = (int) (255 * FxNoise.clamp(alpha.at(ctx.intensity.now())) * ctx.fade);
            for (Track track : tracks) {
                if (track == null || !visible(track.column)) {
                    continue;
                }
                FxColumn c = track.column;
                double scrollBase = Math.floor(ctx.time * textureSpeed);
                boolean any = false;
                for (int k = 0; k <= segments; k++) {
                    double f = (double) k / segments;
                    double d = Math.pow(f, 1.35) * (c.fall - 0.012);
                    double te = ctx.time - fallTime(d);
                    track.px[k] = centerX(c, d, te, ctx.time);
                    track.pz[k] = centerZ(c, d, te, ctx.time);
                    track.py[k] = c.top - d;
                    track.pr[k] = streamRadius(ctx.intensity, c, d, te);
                    track.pv[k] = te * textureSpeed - scrollBase;
                    if (track.pr[k] > 0.004) {
                        any = true;
                    }
                }
                if (!any) {
                    continue;
                }
                int topBlock = (int) Math.floor(c.top - 1e-6);
                int blocks = Math.min(track.light.length, (int) Math.ceil(c.fall) + 1);
                if (Math.abs(ctx.time - track.lightTime) > LIGHT_INTERVAL) {
                    track.lightTime = ctx.time;
                    for (int b = 0; b < blocks; b++) {
                        track.light[b] = ctx.light(c.x, topBlock - b + 0.5, c.z);
                    }
                }
                if (vc == null) {
                    vc = ctx.buffers.getBuffer(RenderType.entityTranslucentCull(texture));
                }
                double uOff = (c.index * 4 % 12) / 16.0;
                for (int k = 0; k < segments; k++) {
                    double r0 = track.pr[k];
                    double r1 = track.pr[k + 1];
                    if (r0 < 0.004 && r1 < 0.004) {
                        continue;
                    }
                    int lightIndex = Math.max(0, Math.min(blocks - 1, topBlock - (int) Math.floor((track.py[k] + track.py[k + 1]) * 0.5)));
                    int light = track.light[lightIndex];
                    for (int s = 0; s < sides; s++) {
                        double a0 = s * Math.PI * 2 / sides;
                        double a1 = (s + 1) * Math.PI * 2 / sides;
                        double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
                        float u0 = (float) (uOff + s * textureWidth / sides);
                        float u1 = (float) (uOff + (s + 1) * textureWidth / sides);
                        double nx = (c0 + c1) * 0.5;
                        double nz = (s0 + s1) * 0.5;
                        int col = FxColor.argb(a, color, 0.86 + 0.14 * (nx * 0.6 - nz * 0.8));
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
