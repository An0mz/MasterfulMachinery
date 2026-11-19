package io.ticticboom.mods.mm.client.fx;

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
}
