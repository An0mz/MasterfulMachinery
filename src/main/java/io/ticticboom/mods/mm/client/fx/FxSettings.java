package io.ticticboom.mods.mm.client.fx;

import io.ticticboom.mods.mm.config.MMClientConfig;
import io.ticticboom.mods.mm.config.MMConfigSetup;

public final class FxSettings {
    static double distance = 64;
    static double particleScale = 1;
    static int maxParticles = 1500;
    private static double near = 24;
    private static double far = 48;
    private static int minLod;

    private FxSettings() {
    }

    static void refresh() {
        MMClientConfig.EffectQuality quality;
        try {
            distance = MMConfigSetup.CLIENT.effectDistance.get();
            particleScale = MMConfigSetup.CLIENT.effectParticles.get() / 100.0;
            maxParticles = MMConfigSetup.CLIENT.effectMaxParticles.get();
            quality = MMConfigSetup.CLIENT.effectQuality.get();
        } catch (IllegalStateException e) {
            return;
        }
        switch (quality) {
            case LOW -> {
                near = 0;
                far = 16;
                minLod = 1;
            }
            case MEDIUM -> {
                near = 12;
                far = 32;
                minLod = 0;
            }
            default -> {
                near = 24;
                far = 48;
                minLod = 0;
            }
        }
    }

    static int lod(double distance) {
        int lod = distance < near ? 0 : distance < far ? 1 : 2;
        return Math.max(lod, minLod);
    }
}
