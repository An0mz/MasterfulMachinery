package io.ticticboom.mods.mm.client.fx;

public final class FxNoise {
    private FxNoise() {
    }

    public static double hash(long i, long s) {
        long n = i * 0x632BE59BD9B4E019L + s * 0x9E3779B97F4A7C15L;
        n ^= (n >>> 31);
        n *= 0xBF58476D1CE4E5B9L;
        n ^= (n >>> 29);
        return (n >>> 11) * 0x1.0p-53;
    }

    public static double value(double x, long s) {
        long i = (long) Math.floor(x);
        double f = x - i;
        f = f * f * (3 - 2 * f);
        double a = hash(i, s);
        double b = hash(i + 1, s);
        return a + (b - a) * f;
    }

    public static double clamp(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    public static double ease(double x) {
        x = clamp(x);
        return x * x * (3 - 2 * x);
    }
}
