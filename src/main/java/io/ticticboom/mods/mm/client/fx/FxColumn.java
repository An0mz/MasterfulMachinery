package io.ticticboom.mods.mm.client.fx;

import org.jetbrains.annotations.Nullable;

public final class FxColumn {
    public final int index;
    public final double x;
    public final double top;
    public final double z;
    public final long seed;
    public final double phase;
    public final double idleRate;
    public double fall = -1;
    public boolean hasPool;
    public boolean blockAbove = true;
    public boolean pathClear = true;
    public boolean landingClear = true;
    public double bead;
    public double beadThreshold = 1;
    public double pool = 0.05;
    public double inflow;
    public double rootRadius;
    public double swirl;
    final int[] light = new int[66];
    int lightTop;
    int lightCount;
    public double nextBreak;
    public double nextRipple;

    FxColumn(int index, double x, double top, double z, long seed) {
        this.index = index;
        this.x = x;
        this.top = top;
        this.z = z;
        this.seed = seed + index * 7919L;
        this.phase = FxNoise.hash(index, seed) * Math.PI * 2;
        this.idleRate = 1.0 / (3.6 + 3.2 * FxNoise.hash(index + 37, seed));
    }

    public double landing() {
        return top - fall;
    }

    public boolean hasLanding() {
        return fall > 0;
    }

    @Nullable
    static String problem(FxColumn[] columns, boolean needsBlockAbove, boolean needsPath, boolean needsLanding) {
        for (FxColumn c : columns) {
            if (needsBlockAbove && !c.blockAbove) {
                return "no_block_above";
            }
            if (!c.hasLanding()) {
                return "no_landing";
            }
            if (needsPath && !c.pathClear) {
                return "blocked";
            }
            if (needsLanding && !c.landingClear) {
                return "landing_blocked";
            }
        }
        return null;
    }

    public int lightAt(double y) {
        if (lightCount == 0) {
            return 0;
        }
        int index = lightTop - (int) Math.floor(y);
        return light[Math.max(0, Math.min(lightCount - 1, index))];
    }
}
