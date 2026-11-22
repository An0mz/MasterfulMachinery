package io.ticticboom.mods.mm.client.fx;

import io.ticticboom.mods.mm.Ref;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public final class FxStats {
    static long nanos;
    static int vertices;
    static int particles;
    static int machines;
    static int lastParticles;

    private static double avgMs;
    private static double avgQuads;
    private static double avgParticles;
    private static double avgMachines;

    private static final int BENCH_SECONDS = parse(System.getenv("MM_FXBENCH"));
    private static final String BENCH_WORLD = System.getenv("MM_FXBENCH_WORLD") == null ? "New World" : System.getenv("MM_FXBENCH_WORLD");
    private static int phase;
    private static long phaseStart;
    private static long lastFrame;
    private static long benchFrames;
    private static double benchFrameNanos;
    private static double benchFxNanos;
    private static double benchQuads;
    private static double benchParticles;
    private static double benchMachines;
    private static int savedLimit;
    private static boolean savedVsync;
    private static boolean savedPause;
    private static boolean savedHideGui;

    private FxStats() {
    }

    private static int parse(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final java.util.Map<Class<?>, long[]> LAYERS = new java.util.IdentityHashMap<>();

    static void layer(Object state, int vertices) {
        if (phase == 2 && vertices > 0) {
            LAYERS.computeIfAbsent(state.getClass(), k -> new long[1])[0] += vertices;
        }
    }

    static void machine() {
        machines++;
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        double ms = nanos / 1e6;
        avgMs += (ms - avgMs) * 0.05;
        avgQuads += (vertices / 4.0 - avgQuads) * 0.05;
        avgParticles += (particles - avgParticles) * 0.05;
        avgMachines += (machines - avgMachines) * 0.05;
        long now = System.nanoTime();
        if (phase == 2) {
            benchFrames++;
            benchFrameNanos += lastFrame == 0 ? 0 : now - lastFrame;
            benchFxNanos += nanos;
            benchQuads += vertices / 4.0;
            benchParticles += particles;
            benchMachines += machines;
        }
        lastFrame = now;
        lastParticles = particles;
        FxSettings.refresh();
        nanos = 0;
        vertices = 0;
        particles = 0;
        machines = 0;
    }

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (avgMachines < 0.05) {
            return;
        }
        event.getLeft().add(String.format("MM effects: %.2f ms, %d quads, %d particles, %d machines",
                avgMs, Math.round(avgQuads), Math.round(avgParticles), Math.round(avgMachines)));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (BENCH_SECONDS <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        switch (phase) {
            case 0 -> {
                if (mc.screen instanceof TitleScreen) {
                    var options = mc.options;
                    savedLimit = options.framerateLimit().get();
                    savedVsync = options.enableVsync().get();
                    savedPause = options.pauseOnLostFocus;
                    savedHideGui = options.hideGui;
                    options.framerateLimit().set(260);
                    options.enableVsync().set(false);
                    options.pauseOnLostFocus = false;
                    phase = 1;
                    phaseStart = now;
                    mc.createWorldOpenFlows().openWorld(BENCH_WORLD, () -> {
                    });
                }
            }
            case 1 -> {
                if (mc.level == null || mc.player == null) {
                    phaseStart = now;
                    return;
                }
                if (mc.screen != null) {
                    mc.setScreen(null);
                }
                mc.options.hideGui = true;
                if (now - phaseStart > 15_000_000_000L) {
                    phase = 2;
                    phaseStart = now;
                    benchFrames = 0;
                    benchFrameNanos = 0;
                    benchFxNanos = 0;
                    benchQuads = 0;
                    benchParticles = 0;
                    benchMachines = 0;
                }
            }
            case 2 -> {
                if (now - phaseStart > BENCH_SECONDS * 1_000_000_000L && benchFrames > 0) {
                    double seconds = benchFrameNanos / 1e9;
                    Ref.LOG.info("MM FX BENCH: fps={} fx_ms={} quads={} particles={} machines={} frames={}",
                            String.format("%.1f", benchFrames / Math.max(1e-9, seconds)),
                            String.format("%.3f", benchFxNanos / benchFrames / 1e6),
                            Math.round(benchQuads / benchFrames),
                            Math.round(benchParticles / benchFrames),
                            String.format("%.1f", benchMachines / benchFrames),
                            benchFrames);
                    for (var entry : LAYERS.entrySet()) {
                        Class<?> type = entry.getKey();
                        String name = type.getEnclosingClass() == null ? type.getSimpleName() : type.getEnclosingClass().getSimpleName() + "." + type.getSimpleName();
                        Ref.LOG.info("MM FX BENCH layer {}: {} quads/frame", name, Math.round(entry.getValue()[0] / 4.0 / benchFrames));
                    }
                    var options = mc.options;
                    options.framerateLimit().set(savedLimit);
                    options.enableVsync().set(savedVsync);
                    options.pauseOnLostFocus = savedPause;
                    options.hideGui = savedHideGui;
                    phase = 3;
                    mc.stop();
                }
            }
            default -> {
            }
        }
    }
}
