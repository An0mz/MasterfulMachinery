package io.ticticboom.mods.mm.client.fx;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class DripLayer implements FxLayer {
    private final String at;
    @Nullable
    private final String to;
    private final ResourceLocation texture;
    private final int color;
    private final boolean portColor;
    private final boolean portTexture;
    private final boolean glow;
    private final double alpha;
    private final FxValue rate;
    private final double size;
    private final double dropSize;
    private final boolean hideWhenBlocked;

    private final int[] skip;

    private DripLayer(JsonObject json) {
        skip = FxSkip.parse(json);
        at = json.has("at") ? json.get("at").getAsString() : "drips";
        to = json.has("to") ? json.get("to").getAsString() : "floor";
        texture = FxTexture.required(json, "drip");
        color = FxColor.parse(json, "color", 0xFFFFFF);
        portColor = FxColor.port(json);
        portTexture = FxColor.portTexture(json);
        glow = json.has("glow") && json.get("glow").getAsBoolean();
        alpha = number(json, "alpha", 0.96);
        rate = FxValue.parse(json, "rate", 1.0);
        size = number(json, "size", 0.03);
        dropSize = number(json, "drop_size", 0.036);
        hideWhenBlocked = !json.has("hide_when_blocked") || json.get("hide_when_blocked").getAsBoolean();
    }

    public static DripLayer parse(JsonObject json) {
        return new DripLayer(json);
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    @Override
    @Nullable
    public State create(FxShared shared) {
        FxColumn[] columns = FxSkip.keep(shared.columns(at, to), skip);
        if (columns == null) {
            return null;
        }
        for (FxColumn c : columns) {
            if (!c.hasLanding()) {
                FxLog.warnOnce("Drip layer: drip point {} of '{}' has no landing height ('to': '{}')", c.index, at, to);
            }
        }
        return new Beads(shared, columns);
    }

    private boolean visible(FxColumn c) {
        return c.blockAbove && c.hasLanding() && (!hideWhenBlocked || c.pathClear);
    }

    private final class Beads implements State {
        private final FxShared shared;
        private final FxColumn[] columns;
        private final double[] radius = new double[5];
        private final double[] dy = new double[5];
        private RenderType renderType;

        Beads(FxShared shared, FxColumn[] columns) {
            this.shared = shared;
            this.columns = columns;
        }

        @Override
        @Nullable
        public String problem(double intensity) {
            String problem = FxColumn.problem(columns, true, hideWhenBlocked, false);
            return problem != null ? problem : intensity > 0.5 ? "drip_working" : null;
        }

        @Override
        public void update(FxContext ctx) {
            double ws = ctx.intensity.now();
            double scale = rate.at(ws);
            for (FxColumn c : columns) {
                if (!c.blockAbove) {
                    continue;
                }
                double r = c.idleRate * (0.75 + 0.5 * FxNoise.value(ctx.time * 0.07, c.seed + 5)) * scale;
                c.bead += ctx.dt * r * (1.0 - 0.85 * ws);
                if (c.bead < c.beadThreshold) {
                    continue;
                }
                if (visible(c) && shared.drops != null) {
                    shared.drops.spawn(ctx.time, c.x, c.top - 0.05, c.z, 0.25, 0, 0, dropSize + dropSize / 3 * shared.random.nextDouble(), c);
                }
                c.bead = 0.04;
                c.beadThreshold = 0.85 + 0.3 * shared.random.nextDouble();
            }
        }

        @Override
        public void render(FxContext ctx) {
            if (ctx.lod >= 2) {
                return;
            }
            VertexConsumer vc = null;
            int sides = ctx.lod == 0 ? 6 : 4;
            FxFluidSprite sprite = portTexture ? ctx.portFlowing : null;
            int col = FxColor.argb((int) (255 * alpha * ctx.fade), FxColor.pick(portColor, color, ctx, sprite), 1.0);
            for (FxColumn c : columns) {
                if (!visible(c)) {
                    continue;
                }
                double b = Math.max(0, c.bead);
                double rb = size * 0.4 + size * Math.cbrt(b);
                rb = Math.max(rb, c.rootRadius * 1.15);
                double neck = b > 0.7 ? (b - 0.7) / 0.3 * rb * 1.3 : 0;
                radius[0] = rb * 0.95;
                dy[0] = 0;
                radius[1] = rb * 1.05;
                dy[1] = -rb * 0.75;
                radius[2] = rb * 0.85;
                dy[2] = -rb * 1.35 - neck * 0.55;
                radius[3] = rb * 0.45;
                dy[3] = -rb * 1.8 - neck;
                radius[4] = 0;
                dy[4] = -rb * 2.0 - neck;
                if (vc == null) {
                    if (sprite != null) {
                        vc = ctx.buffers.getBuffer(FxFluidSprite.renderType(true));
                    } else {
                        if (renderType == null) {
                            renderType = RenderType.entityTranslucentCull(texture);
                        }
                        vc = ctx.buffers.getBuffer(renderType);
                    }
                }
                FxMesh.lathe(vc, ctx.pose, c.x, c.top - 0.002, c.z, radius, dy, 5, sides, col, glow ? LightTexture.FULL_BRIGHT : c.lightAt(c.top - 0.5),
                        c.index * 0.25, ctx.time * 0.05, sprite);
            }
        }
    }
}
