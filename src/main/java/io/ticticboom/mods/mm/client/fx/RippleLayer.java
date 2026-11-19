package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class RippleLayer implements FxLayer {
    private static final int SIDES = 16;

    private final ResourceLocation texture;
    private final int color;
    private final double alpha;
    private final double life;
    private final double width;
    private final double speed;
    private final int max;

    private RippleLayer(JsonObject json) {
        texture = FxTexture.required(json, "ripple");
        color = FxColor.parse(json, "color", 0xFFFFFF);
        alpha = number(json, "alpha", 1.0);
        life = Math.max(0.05, number(json, "life", 0.55));
        width = number(json, "width", 0.014);
        speed = number(json, "speed", 0.42);
        max = Math.max(1, Math.min(256, (int) number(json, "max", 32)));
    }

    public static RippleLayer parse(JsonObject json) {
        return new RippleLayer(json);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    @Override
    public State create(FxShared shared) {
        Ripples ripples = new Ripples();
        if (shared.ripples != null) {
            Ref.LOG.warn("Effect has more than one ripple layer; only the last one is used");
        }
        shared.ripples = ripples;
        return ripples;
    }

    public final class Ripples implements State {
        private final double[] t0 = new double[max];
        private final double[] x = new double[max];
        private final double[] z = new double[max];
        private final double[] strength = new double[max];
        private final FxColumn[] column = new FxColumn[max];
        private int count;

        public void clear() {
            for (int i = 0; i < count; i++) {
                column[i] = null;
            }
            count = 0;
        }

        public void spawn(double time, double px, double pz, double power, FxColumn source) {
            if (count == max) {
                return;
            }
            int i = count++;
            t0[i] = time;
            x[i] = px;
            z[i] = pz;
            strength[i] = power;
            column[i] = source;
        }

        @Override
        public void update(FxContext ctx) {
            int write = 0;
            for (int i = 0; i < count; i++) {
                if (ctx.time - t0[i] > life) {
                    continue;
                }
                t0[write] = t0[i];
                x[write] = x[i];
                z[write] = z[i];
                strength[write] = strength[i];
                column[write] = column[i];
                write++;
            }
            for (int i = write; i < count; i++) {
                column[i] = null;
            }
            count = write;
        }

        @Override
        public void render(FxContext ctx) {
            VertexConsumer vc = null;
            for (int i = 0; i < count; i++) {
                FxColumn c = column[i];
                double age = ctx.time - t0[i];
                if (age < 0 || age > life || !c.hasPool || !c.landingClear) {
                    continue;
                }
                double f = age / life;
                double maxR = 0.05 + 0.23 * Math.sqrt(c.pool);
                double rr = Math.min(maxR * 1.02, 0.015 + speed * age * Math.sqrt(strength[i]));
                int a = (int) (110 * strength[i] * Math.pow(1 - f, 1.5) * ctx.fade * alpha);
                if (a < 3) {
                    continue;
                }
                if (vc == null) {
                    vc = ctx.buffers.getBuffer(RenderType.entityTranslucent(texture));
                }
                int col = FxColor.argb(a, color, 1.25);
                int light = ctx.light(c.x, c.landing() + 0.5, c.z);
                double y = c.landing() + 0.017;
                double ri = Math.max(0, rr - width);
                for (int s = 0; s < SIDES; s++) {
                    double a0 = s * Math.PI * 2 / SIDES;
                    double a1 = (s + 1) * Math.PI * 2 / SIDES;
                    FxMesh.vertex(vc, ctx.pose, x[i] + Math.cos(a0) * ri, y, z[i] + Math.sin(a0) * ri, 0.1f, 0.1f, col, light, 0, 1, 0);
                    FxMesh.vertex(vc, ctx.pose, x[i] + Math.cos(a0) * rr, y, z[i] + Math.sin(a0) * rr, 0.12f, 0.1f, col, light, 0, 1, 0);
                    FxMesh.vertex(vc, ctx.pose, x[i] + Math.cos(a1) * rr, y, z[i] + Math.sin(a1) * rr, 0.12f, 0.12f, col, light, 0, 1, 0);
                    FxMesh.vertex(vc, ctx.pose, x[i] + Math.cos(a1) * ri, y, z[i] + Math.sin(a1) * ri, 0.1f, 0.12f, col, light, 0, 1, 0);
                }
            }
        }
    }
}
