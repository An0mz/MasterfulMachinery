package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class EmitterLayer implements FxLayer {
    private final ResourceLocation texture;
    private final String at;
    private final FxValue rate;
    private final int max;
    private final double lifeMin;
    private final double lifeMax;
    private final double[] velocity;
    private final double[] velocitySpread;
    private final double[] box;
    private final double gravity;
    private final double drag;
    private final double sizeStart;
    private final double sizeEnd;
    private final int colorStart;
    private final int colorEnd;
    private final boolean portColor;
    private final double endShade;
    private final double alphaStart;
    private final double alphaEnd;
    private final int frames;
    private final double spinMin;
    private final double spinMax;
    private final boolean glow;
    private final boolean additive;
    private final boolean collide;

    private EmitterLayer(JsonObject json) {
        texture = FxTexture.required(json, "emitter");
        at = json.has("at") ? json.get("at").getAsString() : "emit";
        rate = FxValue.parse(json, "rate", 10);
        max = Math.max(1, Math.min(1024, json.has("max") ? json.get("max").getAsInt() : 128));
        double[] life = pair(json, "life", 1.0, 1.5);
        lifeMin = Math.max(0.05, Math.min(life[0], life[1]));
        lifeMax = Math.max(lifeMin, Math.max(life[0], life[1]));
        velocity = vector(json, "velocity");
        velocitySpread = vector(json, "velocity_spread");
        box = vector(json, "box");
        gravity = number(json, "gravity", 0);
        drag = Math.max(0, number(json, "drag", 0));
        double[] size = pair(json, "size", 0.2, 0.2);
        sizeStart = size[0];
        sizeEnd = size[1];
        int[] colors = colors(json);
        colorStart = colors[0];
        colorEnd = colors[1];
        portColor = FxColor.port(json);
        double startLuma = FxColor.luminance(colorStart);
        endShade = startLuma <= 0 ? 1 : Math.min(2, FxColor.luminance(colorEnd) / startLuma);
        double[] alpha = pair(json, "alpha", 1.0, 0.0);
        alphaStart = alpha[0];
        alphaEnd = alpha[1];
        frames = Math.max(1, json.has("frames") ? json.get("frames").getAsInt() : 1);
        double[] spin = pair(json, "spin", 0, 0);
        spinMin = Math.toRadians(Math.min(spin[0], spin[1]));
        spinMax = Math.toRadians(Math.max(spin[0], spin[1]));
        glow = json.has("glow") && json.get("glow").getAsBoolean();
        String blend = json.has("blend") ? json.get("blend").getAsString() : "translucent";
        if (!blend.equals("translucent") && !blend.equals("additive")) {
            throw new IllegalArgumentException("emitter 'blend' must be translucent or additive");
        }
        additive = blend.equals("additive");
        collide = json.has("collide") && json.get("collide").getAsBoolean();
    }

    public static EmitterLayer parse(JsonObject json) {
        return new EmitterLayer(json);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static double[] pair(JsonObject json, String key, double first, double second) {
        if (!json.has(key)) {
            return new double[]{first, second};
        }
        JsonElement el = json.get(key);
        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            if (a.size() != 2) {
                throw new IllegalArgumentException("emitter '" + key + "' must be a number or [start, end]");
            }
            return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble()};
        }
        double v = el.getAsDouble();
        return new double[]{v, v};
    }

    private static double[] vector(JsonObject json, String key) {
        if (!json.has(key)) {
            return new double[]{0, 0, 0};
        }
        JsonElement el = json.get(key);
        if (!el.isJsonArray() || el.getAsJsonArray().size() != 3) {
            throw new IllegalArgumentException("emitter '" + key + "' must be [x, y, z]");
        }
        JsonArray a = el.getAsJsonArray();
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    private static int[] colors(JsonObject json) {
        if (!json.has("color")) {
            return new int[]{0xFFFFFF, 0xFFFFFF};
        }
        JsonElement el = json.get("color");
        var holder = new JsonObject();
        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            if (a.size() != 2) {
                throw new IllegalArgumentException("emitter 'color' must be a colour or [start, end]");
            }
            holder.add("a", a.get(0));
            holder.add("b", a.get(1));
            return new int[]{FxColor.parse(holder, "a", 0xFFFFFF), FxColor.parse(holder, "b", 0xFFFFFF)};
        }
        holder.add("a", el);
        int c = FxColor.parse(holder, "a", 0xFFFFFF);
        return new int[]{c, c};
    }

    @Override
    @Nullable
    public State create(FxShared shared) {
        double[] points = shared.points(at);
        long[] cells = points == null ? shared.area(at) : null;
        if ((points == null || points.length < 3) && (cells == null || cells.length == 0)) {
            Ref.LOG.warn("Emitter layer: no point or area anchor named '{}', or it is empty", at);
            return null;
        }
        return new Particles(shared, points, cells);
    }

    private final class Particles implements State {
        private final FxShared shared;
        @Nullable
        private final double[] points;
        @Nullable
        private final long[] cells;
        private final double[] x = new double[max];
        private final double[] y = new double[max];
        private final double[] z = new double[max];
        private final double[] vx = new double[max];
        private final double[] vy = new double[max];
        private final double[] vz = new double[max];
        private final double[] age = new double[max];
        private final double[] life = new double[max];
        private final double[] roll = new double[max];
        private final double[] spin = new double[max];
        private final boolean[] stuck = new boolean[max];
        private final boolean[] free = new boolean[max];
        private final int[] lights = new int[max];
        private final double baseVx;
        private final double baseVz;
        private final double spreadVx;
        private final double spreadVz;
        private final double boxX;
        private final double boxZ;
        private int count;
        private double pending;
        private RenderType renderType;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        Particles(FxShared shared, @Nullable double[] points, @Nullable long[] cells) {
            this.shared = shared;
            this.points = points;
            this.cells = cells;
            this.baseVx = shared.rotateX(velocity[0], velocity[2]);
            this.baseVz = shared.rotateZ(velocity[0], velocity[2]);
            this.spreadVx = Math.abs(shared.rotateX(velocitySpread[0], velocitySpread[2]));
            this.spreadVz = Math.abs(shared.rotateZ(velocitySpread[0], velocitySpread[2]));
            this.boxX = Math.abs(shared.rotateX(box[0], box[2]));
            this.boxZ = Math.abs(shared.rotateZ(box[0], box[2]));
        }

        @Override
        public void reset() {
            count = 0;
            pending = 0;
        }

        @Override
        public double reach() {
            double speed = Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2])
                    + Math.sqrt(velocitySpread[0] * velocitySpread[0] + velocitySpread[1] * velocitySpread[1] + velocitySpread[2] * velocitySpread[2]);
            double fallen = 0.5 * Math.abs(gravity) * lifeMax * lifeMax;
            double boxReach = Math.max(Math.abs(box[0]), Math.max(Math.abs(box[1]), Math.abs(box[2])));
            return Math.min(64, speed * lifeMax + fallen + boxReach + Math.max(sizeStart, sizeEnd));
        }

        private double signed(double range) {
            return range == 0 ? 0 : (shared.random.nextDouble() * 2 - 1) * range;
        }

        private void spawn(FxContext ctx) {
            int i = count++;
            if (points != null) {
                int p = shared.random.nextInt(points.length / 3) * 3;
                x[i] = points[p] + signed(boxX);
                y[i] = points[p + 1] + signed(box[1]);
                z[i] = points[p + 2] + signed(boxZ);
            } else {
                long cell = cells[shared.random.nextInt(cells.length)];
                x[i] = BlockPos.getX(cell) + shared.random.nextDouble();
                y[i] = BlockPos.getY(cell) + shared.random.nextDouble();
                z[i] = BlockPos.getZ(cell) + shared.random.nextDouble();
            }
            vx[i] = baseVx + signed(spreadVx);
            vy[i] = velocity[1] + signed(velocitySpread[1]);
            vz[i] = baseVz + signed(spreadVz);
            age[i] = 0;
            life[i] = lifeMin + (lifeMax - lifeMin) * shared.random.nextDouble();
            roll[i] = shared.random.nextDouble() * Math.PI * 2;
            spin[i] = spinMin + (spinMax - spinMin) * shared.random.nextDouble();
            stuck[i] = false;
            boolean inside = blocked(ctx, x[i], y[i], z[i]);
            free[i] = collide && inside;
            lights[i] = glow || additive ? LightTexture.FULL_BRIGHT : ctx.light(x[i], y[i] + (inside ? 1 : 0), z[i]);
        }

        private void remove(int i) {
            int last = --count;
            x[i] = x[last];
            y[i] = y[last];
            z[i] = z[last];
            vx[i] = vx[last];
            vy[i] = vy[last];
            vz[i] = vz[last];
            age[i] = age[last];
            life[i] = life[last];
            roll[i] = roll[last];
            spin[i] = spin[last];
            stuck[i] = stuck[last];
            free[i] = free[last];
            lights[i] = lights[last];
        }

        private boolean blocked(FxContext ctx, double px, double py, double pz) {
            cursor.set(ctx.origin.getX() + (int) Math.floor(px), ctx.origin.getY() + (int) Math.floor(py), ctx.origin.getZ() + (int) Math.floor(pz));
            var state = ctx.level.getBlockState(cursor);
            return !state.isAir() && !state.getCollisionShape(ctx.level, cursor).isEmpty();
        }

        @Override
        public void update(FxContext ctx) {
            double dt = Math.min(ctx.dt, 0.25);
            pending += Math.max(0, rate.at(ctx.intensity.now())) * dt;
            while (pending >= 1) {
                if (count == max) {
                    pending = 0;
                    break;
                }
                pending -= 1;
                spawn(ctx);
            }
            double damp = Math.max(0, 1 - drag * dt);
            for (int i = count - 1; i >= 0; i--) {
                age[i] += dt;
                if (age[i] >= life[i]) {
                    remove(i);
                    continue;
                }
                roll[i] += spin[i] * dt;
                if (stuck[i]) {
                    continue;
                }
                vy[i] -= gravity * dt;
                vx[i] *= damp;
                vy[i] *= damp;
                vz[i] *= damp;
                double nx = x[i] + vx[i] * dt;
                double ny = y[i] + vy[i] * dt;
                double nz = z[i] + vz[i] * dt;
                if (collide) {
                    boolean hit = blocked(ctx, nx, ny, nz);
                    if (free[i]) {
                        if (!hit) {
                            free[i] = false;
                        }
                    } else if (hit) {
                        stuck[i] = true;
                        continue;
                    }
                }
                x[i] = nx;
                y[i] = ny;
                z[i] = nz;
            }
        }

        @Override
        public void render(FxContext ctx) {
            if (count == 0) {
                return;
            }
            if (renderType == null) {
                renderType = additive ? RenderType.eyes(texture) : RenderType.entityTranslucent(texture);
            }
            VertexConsumer vc = ctx.buffers.getBuffer(renderType);
            float frameHeight = 1f / frames;
            int start = colorStart;
            int end = colorEnd;
            if (portColor && ctx.portColor >= 0) {
                start = ctx.portColor;
                end = FxColor.argb(0, ctx.portColor, endShade);
            }
            for (int i = 0; i < count; i++) {
                double f = Math.min(1, age[i] / life[i]);
                double size = (sizeStart + (sizeEnd - sizeStart) * f) * 0.5;
                if (size <= 0) {
                    continue;
                }
                double alpha = (alphaStart + (alphaEnd - alphaStart) * f) * ctx.fade;
                int alphaByte = (int) (255 * FxNoise.clamp(alpha));
                if (alphaByte < 2) {
                    continue;
                }
                int col = mix(start, end, f, alphaByte);
                double cos = Math.cos(roll[i]);
                double sin = Math.sin(roll[i]);
                double ax = (ctx.leftX * cos + ctx.upX * sin) * size;
                double ay = (ctx.leftY * cos + ctx.upY * sin) * size;
                double az = (ctx.leftZ * cos + ctx.upZ * sin) * size;
                double bx = (ctx.upX * cos - ctx.leftX * sin) * size;
                double by = (ctx.upY * cos - ctx.leftY * sin) * size;
                double bz = (ctx.upZ * cos - ctx.leftZ * sin) * size;
                int frame = Math.min(frames - 1, (int) (f * frames));
                float v0 = frame * frameHeight;
                float v1 = v0 + frameHeight;
                FxMesh.facing(vc, ctx.pose,
                        x[i] + ax + bx, y[i] + ay + by, z[i] + az + bz, 0, v0,
                        x[i] + ax - bx, y[i] + ay - by, z[i] + az - bz, 0, v1,
                        x[i] - ax - bx, y[i] - ay - by, z[i] - az - bz, 1, v1,
                        x[i] - ax + bx, y[i] - ay + by, z[i] - az + bz, 1, v0,
                        col, lights[i], -ctx.lookX, -ctx.lookY, -ctx.lookZ, 0, 1, 0);
            }
        }

        private int mix(int a, int b, double f, int alpha) {
            int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * f);
            int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * f);
            int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * f);
            if (additive) {
                r = r * alpha / 255;
                g = g * alpha / 255;
                bl = bl * alpha / 255;
                alpha = 255;
            }
            return alpha << 24 | r << 16 | g << 8 | bl;
        }
    }
}
