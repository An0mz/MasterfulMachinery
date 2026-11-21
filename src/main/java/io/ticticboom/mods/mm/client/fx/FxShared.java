package io.ticticboom.mods.mm.client.fx;

import io.ticticboom.mods.mm.structure.attachment.visuals.EffectVisual;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class FxShared {
    private static final double MAX_FALL = 64;

    public final RandomSource random;
    private final long seed;
    private final EffectVisual.Resolved anchors;
    private final Map<String, FxColumn[]> columns = new HashMap<>();
    @Nullable
    public DropsLayer.Drops drops;
    @Nullable
    public RippleLayer.Ripples ripples;

    public final Rotation rotation;
    public int portColor = -1;

    FxShared(EffectVisual.Resolved anchors, long seed, Rotation rotation) {
        this.anchors = anchors;
        this.rotation = rotation;
        this.seed = seed;
        this.random = RandomSource.create(seed);
    }

    @Nullable
    public Double height(String name) {
        return anchors.hasHeight(name) ? anchors.height(name, 0) : null;
    }

    public long seed() {
        return seed;
    }

    @Nullable
    public double[] points(String name) {
        return anchors.points(name);
    }

    @Nullable
    public long[] area(String name) {
        return anchors.area(name);
    }

    public double rotateX(double x, double z) {
        return switch (rotation) {
            case NONE -> x;
            case CLOCKWISE_90 -> -z;
            case CLOCKWISE_180 -> -x;
            case COUNTERCLOCKWISE_90 -> z;
        };
    }

    public double rotateZ(double x, double z) {
        return switch (rotation) {
            case NONE -> z;
            case CLOCKWISE_90 -> x;
            case CLOCKWISE_180 -> -z;
            case COUNTERCLOCKWISE_90 -> -x;
        };
    }

    @Nullable
    public FxColumn[] columns(String at, @Nullable String to) {
        FxColumn[] result = columns.get(at);
        if (result == null) {
            double[] points = anchors.points(at);
            if (points == null) {
                FxLog.warnOnce("Effect: no point anchor named '{}'", at);
                return null;
            }
            result = new FxColumn[points.length / 3];
            for (int i = 0; i < result.length; i++) {
                result[i] = new FxColumn(i, points[i * 3], points[i * 3 + 1], points[i * 3 + 2], seed);
            }
            columns.put(at, result);
        }
        if (to != null) {
            double[] ends = anchors.points(to);
            boolean height = anchors.hasHeight(to);
            if (!height && ends == null) {
                FxLog.warnOnce("Effect: no height or point anchor named '{}'", to);
            }
            for (FxColumn column : result) {
                if (column.hasLanding()) {
                    continue;
                }
                double bottom;
                if (height) {
                    bottom = anchors.height(to, column.top);
                } else if (ends != null && ends.length >= 3) {
                    int j = Math.min(column.index, ends.length / 3 - 1);
                    bottom = ends[j * 3 + 1];
                } else {
                    continue;
                }
                double fall = Math.min(MAX_FALL, column.top - bottom);
                if (fall > 0.05) {
                    column.fall = fall;
                }
            }
        }
        return result;
    }

    void reset(boolean working, double time) {
        for (FxColumn[] list : columns.values()) {
            for (FxColumn c : list) {
                c.bead = random.nextDouble() * 0.8;
                c.beadThreshold = 0.85 + 0.3 * random.nextDouble();
                c.pool = working ? 0.72 : 0.06;
                c.nextBreak = time + random.nextDouble();
                c.nextRipple = time;
            }
        }
        if (drops != null) {
            drops.clear();
        }
        if (ripples != null) {
            ripples.clear();
        }
    }

    void refreshLight(FxContext ctx) {
        for (FxColumn[] list : columns.values()) {
            for (FxColumn c : list) {
                c.lightTop = (int) Math.floor(c.top - 1e-6);
                c.lightCount = c.hasLanding() ? Math.min(c.light.length, (int) Math.ceil(c.fall) + 1) : 1;
                for (int b = 0; b < c.lightCount; b++) {
                    c.light[b] = ctx.light(c.x, c.lightTop - b + 0.5, c.z);
                }
            }
        }
    }

    void scan(FxContext ctx) {
        for (FxColumn[] list : columns.values()) {
            for (FxColumn c : list) {
                int bx = (int) Math.floor(c.x);
                int bz = (int) Math.floor(c.z);
                int topBlock = (int) Math.ceil(c.top - 1e-6);
                c.blockAbove = ctx.solid(bx, topBlock, bz) || ctx.solid(bx, (int) Math.floor(c.top + 1e-6), bz);
                if (!c.hasLanding()) {
                    continue;
                }
                int landingBlock = (int) Math.ceil(c.landing() - 0.01);
                boolean clear = true;
                for (int y = (int) Math.floor(c.top + 1e-6) - 1; y >= landingBlock && clear; y--) {
                    clear = ctx.passable(bx, y, bz, c.x - bx, c.z - bz);
                }
                c.pathClear = clear;
                c.landingClear = ctx.passable(bx, landingBlock, bz, c.x - bx, c.z - bz);
            }
        }
    }
}
