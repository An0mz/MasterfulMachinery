package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class RippleLayer implements FxLayer {
    private static final int SIDES = 16;

    private final ResourceLocation texture;
    private final int color;
    private final boolean portColor;
    private final boolean portTexture;
    private final boolean glow;
    private final double alpha;
    private final double life;
    private final double width;
    private final double speed;
    private final int max;

    private RippleLayer(JsonObject json) {
        texture = FxTexture.required(json, "ripple");
        color = FxColor.parse(json, "color", 0xFFFFFF);
        portColor = FxColor.port(json);
        portTexture = FxColor.portTexture(json);
        glow = json.has("glow") && json.get("glow").getAsBoolean();
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
            FxLog.warnOnce("Effect has more than one ripple layer; only the last one is used");
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
        private RenderType renderType;

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
            if (ctx.lod >= 2) {
                return;
            }
            VertexConsumer vc = null;
            FxFluidSprite sprite = portTexture ? ctx.portStill : null;
            float ua = sprite == null ? 0.1f : sprite.u(0.4);
            float ub = sprite == null ? 0.12f : sprite.u(0.45);
            float va = sprite == null ? 0.1f : sprite.v(0.4);
            float vb = sprite == null ? 0.12f : sprite.v(0.45);
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
                    if (sprite != null) {
                        vc = ctx.buffers.getBuffer(FxFluidSprite.renderType(false));
                    } else {
                        if (renderType == null) {
                            renderType = RenderType.entityTranslucent(texture);
                        }
                        vc = ctx.buffers.getBuffer(renderType);
                    }
                }
                int col = FxColor.argb(a, FxColor.pick(portColor, color, ctx, sprite), 1.25);
                int light = glow ? LightTexture.FULL_BRIGHT : c.lightAt(c.landing() + 0.5);
                double y = c.landing() + 0.017;
                double ri = Math.max(0, rr - width);
                double[] cos = FxTrig.cos(SIDES);
                double[] sin = FxTrig.sin(SIDES);
                for (int s = 0; s < SIDES; s++) {
                    FxMesh.vertex(vc, ctx.pose, x[i] + cos[s] * ri, y, z[i] + sin[s] * ri, ua, va, col, light, 0, 1, 0);
                    FxMesh.vertex(vc, ctx.pose, x[i] + cos[s] * rr, y, z[i] + sin[s] * rr, ub, va, col, light, 0, 1, 0);
                    FxMesh.vertex(vc, ctx.pose, x[i] + cos[s + 1] * rr, y, z[i] + sin[s + 1] * rr, ub, vb, col, light, 0, 1, 0);
                    FxMesh.vertex(vc, ctx.pose, x[i] + cos[s + 1] * ri, y, z[i] + sin[s + 1] * ri, ua, vb, col, light, 0, 1, 0);
                }
            }
        }
    }
}
