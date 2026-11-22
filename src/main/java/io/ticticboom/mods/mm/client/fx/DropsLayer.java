package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class DropsLayer implements FxLayer {
    private final ResourceLocation texture;
    private final int color;
    private final boolean portColor;
    private final boolean glow;
    private final double alpha;
    private final double gravity;
    private final int max;
    private final double stretch;
    private final double splash;

    private DropsLayer(JsonObject json) {
        texture = FxTexture.required(json, "drops");
        color = FxColor.parse(json, "color", 0xFFFFFF);
        portColor = FxColor.port(json);
        glow = json.has("glow") && json.get("glow").getAsBoolean();
        alpha = number(json, "alpha", 0.96);
        gravity = number(json, "gravity", 9.81);
        max = Math.max(1, Math.min(256, (int) number(json, "max", 48)));
        stretch = number(json, "stretch", 0.06);
        splash = number(json, "splash", 0.12);
        if (gravity <= 0) {
            throw new IllegalArgumentException("drops 'gravity' must be above 0");
        }
    }

    public static DropsLayer parse(JsonObject json) {
        return new DropsLayer(json);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    @Override
    public State create(FxShared shared) {
        Drops drops = new Drops(shared);
        if (shared.drops != null) {
            FxLog.warnOnce("Effect has more than one drops layer; only the last one is used");
        }
        shared.drops = drops;
        return drops;
    }

    public final class Drops implements State {
        private final FxShared shared;
        private final double[] t0 = new double[max];
        private final double[] x0 = new double[max];
        private final double[] y0 = new double[max];
        private final double[] z0 = new double[max];
        private final double[] vy = new double[max];
        private final double[] vx = new double[max];
        private final double[] vz = new double[max];
        private final double[] size = new double[max];
        private final FxColumn[] column = new FxColumn[max];
        private final double[] radius = new double[5];
        private final double[] dy = new double[5];
        private int count;
        private RenderType renderType;

        Drops(FxShared shared) {
            this.shared = shared;
        }

        public void clear() {
            for (int i = 0; i < count; i++) {
                column[i] = null;
            }
            count = 0;
        }

        public void spawn(double time, double x, double y, double z, double speedDown, double speedX, double speedZ, double dropSize, FxColumn source) {
            if (count == max) {
                remove(0);
            }
            int i = count++;
            t0[i] = time;
            x0[i] = x;
            y0[i] = y;
            z0[i] = z;
            vy[i] = speedDown;
            vx[i] = speedX;
            vz[i] = speedZ;
            size[i] = dropSize;
            column[i] = source;
        }

        private void remove(int i) {
            int last = count - 1;
            int moved = last - i;
            System.arraycopy(t0, i + 1, t0, i, moved);
            System.arraycopy(x0, i + 1, x0, i, moved);
            System.arraycopy(y0, i + 1, y0, i, moved);
            System.arraycopy(z0, i + 1, z0, i, moved);
            System.arraycopy(vy, i + 1, vy, i, moved);
            System.arraycopy(vx, i + 1, vx, i, moved);
            System.arraycopy(vz, i + 1, vz, i, moved);
            System.arraycopy(size, i + 1, size, i, moved);
            System.arraycopy(column, i + 1, column, i, moved);
            column[last] = null;
            count = last;
        }

        private double age(int i, double time) {
            return Math.max(0, time - t0[i]);
        }

        private double y(int i, double time) {
            double a = age(i, time);
            return y0[i] - vy[i] * a - 0.5 * gravity * a * a;
        }

        @Override
        public void update(FxContext ctx) {
            for (int i = count - 1; i >= 0; i--) {
                FxColumn c = column[i];
                if (y(i, ctx.time) > c.landing() + 0.01) {
                    continue;
                }
                double a = age(i, ctx.time);
                if (c.hasPool && c.landingClear) {
                    c.pool = Math.min(1, c.pool + splash * (size[i] / 0.04) * (1 - c.pool));
                    if (shared.ripples != null) {
                        shared.ripples.spawn(ctx.time, x0[i] + vx[i] * a, z0[i] + vz[i] * a, 0.55 + 0.4 * (size[i] / 0.045), c);
                    }
                }
                remove(i);
            }
        }

        @Override
        public void render(FxContext ctx) {
            if (count == 0) {
                return;
            }
            if (renderType == null) {
                renderType = RenderType.entityTranslucentCull(texture);
            }
            VertexConsumer vc = ctx.buffers.getBuffer(renderType);
            int col = FxColor.argb((int) (255 * alpha * ctx.fade), FxColor.pick(portColor, color, ctx), 1.0);
            for (int i = 0; i < count; i++) {
                double a = age(i, ctx.time);
                double x = x0[i] + vx[i] * a;
                double y = y(i, ctx.time);
                double z = z0[i] + vz[i] * a;
                if (y > column[i].top) {
                    continue;
                }
                double speed = vy[i] + gravity * a;
                double s = size[i] * 0.5;
                double h = s * (1.0 + Math.min(1.1, speed * stretch));
                radius[0] = 0;
                dy[0] = h;
                radius[1] = s * 0.55;
                dy[1] = h * 0.5;
                radius[2] = s;
                dy[2] = 0;
                radius[3] = s * 0.85;
                dy[3] = -h * 0.5;
                radius[4] = 0;
                dy[4] = -h * 0.75;
                FxMesh.lathe(vc, ctx.pose, x, y, z, radius, dy, 5, 5, col, glow ? LightTexture.FULL_BRIGHT : column[i].lightAt(y), size[i] * 37 % 1, t0[i]);
            }
        }
    }
}
