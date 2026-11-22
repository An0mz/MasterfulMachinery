package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class PoolLayer implements FxLayer {
    private static final int POINTS = 22;
    private static final double[] RINGS = {0.0, 0.5, 0.88, 1.0, 1.0};
    private static final double[] GROW = {0, 0, 0, 0, 0.022};
    private static final double[] SHADE = {0.62, 0.8, 0.9, 0.62, 0.55};
    private static final double[] RING_ALPHA = {245, 240, 225, 190, 0};

    private final String at;
    @Nullable
    private final String to;
    private final ResourceLocation texture;
    private final int color;
    private final boolean portColor;
    private final boolean glow;
    private final double alpha;
    private final double size;
    private final double base;
    private final double fill;
    private final double drain;
    private final FxValue swirl;
    private final boolean hideWhenBlocked;

    private final int[] skip;

    private PoolLayer(JsonObject json) {
        skip = FxSkip.parse(json);
        at = json.has("at") ? json.get("at").getAsString() : "drips";
        to = json.has("to") ? json.get("to").getAsString() : "floor";
        texture = FxTexture.required(json, "pool");
        color = FxColor.parse(json, "color", 0xFFFFFF);
        portColor = FxColor.port(json);
        glow = json.has("glow") && json.get("glow").getAsBoolean();
        alpha = number(json, "alpha", 1.0);
        size = number(json, "size", 0.21);
        base = number(json, "base", 0.05);
        fill = number(json, "fill", 0.55);
        drain = number(json, "drain", 0.16);
        swirl = FxValue.parse(json, "swirl", 0);
        hideWhenBlocked = !json.has("hide_when_blocked") || json.get("hide_when_blocked").getAsBoolean();
    }

    public static PoolLayer parse(JsonObject json) {
        return new PoolLayer(json);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private double swirlSpeed(double intensity) {
        return swirl.idle() == 0 && swirl.working() == 0 ? 0.12 + 0.55 * intensity : swirl.at(intensity);
    }

    @Override
    @Nullable
    public State create(FxShared shared) {
        FxColumn[] columns = FxSkip.keep(shared.columns(at, to), skip);
        if (columns == null) {
            return null;
        }
        for (FxColumn c : columns) {
            c.hasPool = true;
            if (!c.hasLanding()) {
                FxLog.warnOnce("Pool layer: drip point {} of '{}' has no landing height ('to': '{}')", c.index, at, to);
            }
        }
        return new Pools(shared, columns);
    }

    private boolean visible(FxColumn c) {
        return c.blockAbove && c.hasLanding() && (!hideWhenBlocked || c.landingClear);
    }

    private final class Pools implements State {
        private final FxShared shared;
        private final FxColumn[] columns;
        private final double[][] xs = new double[RINGS.length][POINTS];
        private final double[][] zs = new double[RINGS.length][POINTS];
        private final float[][] us = new float[RINGS.length][POINTS];
        private final float[][] vs = new float[RINGS.length][POINTS];
        private final int[] colors = new int[RINGS.length];
        private RenderType renderType;

        Pools(FxShared shared, FxColumn[] columns) {
            this.shared = shared;
            this.columns = columns;
        }

        @Override
        @Nullable
        public String problem(double intensity) {
            return FxColumn.problem(columns, true, false, hideWhenBlocked);
        }

        @Override
        public void update(FxContext ctx) {
            double turnSpeed = swirlSpeed(ctx.intensity.now());
            for (FxColumn c : columns) {
                c.swirl += ctx.dt * turnSpeed;
                if (!visible(c)) {
                    continue;
                }
                c.pool += ctx.dt * (fill * c.inflow * (1 - c.pool) - drain * c.pool);
                c.pool = Math.max(0.035, Math.min(1, c.pool));
                if (c.inflow > 0.15 && ctx.time >= c.nextRipple && shared.ripples != null) {
                    shared.ripples.spawn(ctx.time, c.x, c.z, 0.35 + 0.4 * Math.min(1, c.inflow), c);
                    c.nextRipple = ctx.time + 0.16 + 0.22 * shared.random.nextDouble();
                }
            }
        }

        @Override
        public void render(FxContext ctx) {
            VertexConsumer vc = null;
            int tint = FxColor.pick(portColor, color, ctx);
            double ws = ctx.intensity.now();
            for (FxColumn c : columns) {
                if (!visible(c)) {
                    continue;
                }
                if (vc == null) {
                    if (renderType == null) {
                        renderType = RenderType.entityTranslucent(texture);
                    }
                    vc = ctx.buffers.getBuffer(renderType);
                }
                double p = c.pool;
                double r = base + size * Math.sqrt(p);
                double y = c.landing() + 0.012;
                double a0 = (0.62 + 0.3 * Math.sqrt(p)) * ctx.fade * alpha;
                for (int k = 0; k < RINGS.length; k++) {
                    colors[k] = FxColor.argb((int) (RING_ALPHA[k] * a0), tint, SHADE[k]);
                }
                double turn = c.swirl;
                double ph = c.phase;
                double uo = 0.5 + 0.12 * Math.cos(ph);
                double vo = 0.5 + 0.12 * Math.sin(ph);
                double[] cos = FxTrig.cos(POINTS);
                double[] sin = FxTrig.sin(POINTS);
                int step = ctx.lod == 0 ? 1 : 2;
                for (int i = 0; i < POINTS; i += step) {
                    double a = i * Math.PI * 2 / POINTS;
                    double shape = 1 + 0.16 * Math.sin(2 * a + ph + 0.21 * ctx.time) + 0.11 * Math.sin(3 * a + ph * 2.3 - 0.13 * ctx.time)
                            + 0.07 * Math.sin(5 * a + ph * 3.1 + 0.33 * ctx.time) + 0.12 * (FxNoise.value(i * 0.7 + ph * 5, 21) - 0.5);
                    for (int k = 0; k < RINGS.length; k++) {
                        double rr = r * RINGS[k] * shape + GROW[k];
                        xs[k][i] = c.x + cos[i] * rr;
                        zs[k][i] = c.z + sin[i] * rr;
                        double ta = a + turn * (1.15 - RINGS[k]);
                        double tr = rr * 0.5;
                        us[k][i] = (float) (uo + Math.cos(ta) * tr);
                        vs[k][i] = (float) (vo + Math.sin(ta) * tr);
                    }
                }
                int light = glow ? LightTexture.FULL_BRIGHT : c.lightAt(c.landing() + 0.5);
                for (int k = 0; k < RINGS.length - 1; k++) {
                    for (int i = 0; i < POINTS; i += step) {
                        int j = (i + step) % POINTS;
                        FxMesh.vertex(vc, ctx.pose, xs[k][i], y, zs[k][i], us[k][i], vs[k][i], colors[k], light, 0, 1, 0);
                        FxMesh.vertex(vc, ctx.pose, xs[k + 1][i], y, zs[k + 1][i], us[k + 1][i], vs[k + 1][i], colors[k + 1], light, 0, 1, 0);
                        FxMesh.vertex(vc, ctx.pose, xs[k + 1][j], y, zs[k + 1][j], us[k + 1][j], vs[k + 1][j], colors[k + 1], light, 0, 1, 0);
                        FxMesh.vertex(vc, ctx.pose, xs[k][j], y, zs[k][j], us[k][j], vs[k][j], colors[k], light, 0, 1, 0);
                    }
                }
            }
        }
    }
}
