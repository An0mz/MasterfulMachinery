package io.ticticboom.mods.mm.client.fx;

public final class FxTrig {
    private static final int MAX = 32;
    private static final double[][] COS = new double[MAX + 1][];
    private static final double[][] SIN = new double[MAX + 1][];

    static {
        for (int n = 1; n <= MAX; n++) {
            COS[n] = new double[n + 1];
            SIN[n] = new double[n + 1];
            for (int i = 0; i <= n; i++) {
                double a = i * Math.PI * 2 / n;
                COS[n][i] = Math.cos(a);
                SIN[n][i] = Math.sin(a);
            }
        }
    }

    private FxTrig() {
    }

    public static double[] cos(int sides) {
        return COS[Math.max(1, Math.min(MAX, sides))];
    }

    public static double[] sin(int sides) {
        return SIN[Math.max(1, Math.min(MAX, sides))];
    }
}
