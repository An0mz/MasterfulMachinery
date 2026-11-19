package io.ticticboom.mods.mm.client.fx;

public final class FxIntensity {
    private static final double STEP = 0.05;
    private static final int SIZE = 128;

    private final double rise;
    private final double fall;
    private final double hold;
    private final double[] history = new double[SIZE];
    private long bucket = Long.MIN_VALUE;
    private double raw;
    private double lastWorking = -1e9;

    public FxIntensity(double rise, double fall, double hold) {
        this.rise = Math.max(0.01, rise);
        this.fall = Math.max(0.01, fall);
        this.hold = Math.max(0, hold);
    }

    public void reset(boolean working, double time) {
        raw = working ? 1 : 0;
        if (working) {
            lastWorking = time;
        }
        bucket = Long.MIN_VALUE;
        push(time);
    }

    public void update(boolean working, double time, double dt) {
        if (working) {
            lastWorking = time;
        }
        boolean target = working || time - lastWorking < hold;
        raw = target ? Math.min(1, raw + dt / rise) : Math.max(0, raw - dt / fall);
        push(time);
    }

    public double now() {
        return FxNoise.ease(raw);
    }

    public double at(double time) {
        double fb = time / STEP;
        long b = (long) Math.floor(fb);
        if (b >= bucket) {
            return now();
        }
        if (bucket - b >= SIZE - 1) {
            return history[(int) Math.floorMod(bucket + 1, SIZE)];
        }
        double f = fb - b;
        double a = history[(int) Math.floorMod(b, SIZE)];
        double c = b + 1 >= bucket ? now() : history[(int) Math.floorMod(b + 1, SIZE)];
        return a + (c - a) * f;
    }

    private void push(double time) {
        long b = (long) Math.floor(time / STEP);
        double v = now();
        if (bucket == Long.MIN_VALUE || b - bucket > SIZE) {
            for (int i = 0; i < SIZE; i++) {
                history[i] = v;
            }
        } else {
            double prev = history[(int) Math.floorMod(bucket, SIZE)];
            for (long k = bucket + 1; k < b; k++) {
                history[(int) Math.floorMod(k, SIZE)] = prev + (v - prev) * (k - bucket) / (double) (b - bucket);
            }
        }
        history[(int) Math.floorMod(b, SIZE)] = v;
        bucket = b;
    }
}
