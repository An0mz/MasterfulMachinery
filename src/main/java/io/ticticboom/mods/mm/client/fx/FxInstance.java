package io.ticticboom.mods.mm.client.fx;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.visuals.EffectVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class FxInstance {
    private static final double SCAN_INTERVAL = 1.0;

    final StructureModel structure;
    final Rotation rotation;
    final int generation;
    final List<Effect> effects;
    final double centerX;
    final double centerY;
    final double centerZ;
    private double lastTime = -1;
    private double lastScan = -1e9;
    private final AABB relativeBounds;
    private AABB renderBounds;
    private AABB renderBoundsVisual;

    record Effect(FxDefinition definition, FxIntensity intensity, FxShared shared, double viewDistance, List<FxLayer.State> states) {
    }

    private FxInstance(StructureModel structure, Rotation rotation, int generation, List<Effect> effects, AABB bounds) {
        this.structure = structure;
        this.rotation = rotation;
        this.generation = generation;
        this.effects = effects;
        this.relativeBounds = bounds == null ? new AABB(0, 0, 0, 1, 1, 1) : bounds.minmax(new AABB(0, 0, 0, 1, 1, 1));
        var center = bounds == null ? new Vec3(0.5, 0.5, 0.5) : bounds.getCenter();
        this.centerX = center.x;
        this.centerY = center.y;
        this.centerZ = center.z;
    }

    static FxInstance create(StructureModel structure, Rotation rotation, BlockPos pos, List<EffectVisual> visuals) {
        var effects = new ArrayList<Effect>();
        AABB bounds = null;
        long seed = pos.asLong() * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < visuals.size(); i++) {
            EffectVisual visual = visuals.get(i);
            FxDefinition definition = FxManager.get(visual.getFx());
            if (definition == null) {
                Ref.LOG.warn("Structure {}: no effect file for {}", structure.id(), visual.getFx());
                continue;
            }
            var anchors = visual.resolve(structure.layout(), rotation);
            var shared = new FxShared(anchors, seed + i * 31L, rotation);
            var states = new ArrayList<FxLayer.State>();
            for (int l = 0; l < definition.layers().size(); l++) {
                var state = definition.layers().get(l).create(shared);
                if (state != null) {
                    states.add(state);
                }
            }
            if (anchors.bounds() != null) {
                double reach = 0;
                for (FxLayer.State state : states) {
                    reach = Math.max(reach, state.reach());
                }
                AABB box = anchors.bounds().inflate(reach);
                bounds = bounds == null ? box : bounds.minmax(box);
            }
            effects.add(new Effect(definition, new FxIntensity(definition.rise(), definition.fall(), definition.hold()),
                    shared, definition.viewDistance(), List.copyOf(states)));
        }
        return new FxInstance(structure, rotation, FxManager.generation(), List.copyOf(effects), bounds);
    }

    public AABB renderBounds(BlockPos pos, AABB visual) {
        if (renderBounds == null || renderBoundsVisual != visual) {
            AABB box = relativeBounds.move(pos);
            renderBounds = visual == null ? box : box.minmax(visual);
            renderBoundsVisual = visual;
        }
        return renderBounds;
    }

    public boolean matches(StructureModel structure, Rotation rotation) {
        return this.structure == structure && this.rotation == rotation && this.generation == FxManager.generation();
    }

    void tick(FxContext ctx, boolean working) {
        double time = ctx.time;
        boolean reset = lastTime < 0 || time - lastTime > 2.0 || time < lastTime - 2.0;
        ctx.dt = reset ? 0 : Math.max(0, time - lastTime);
        if (reset) {
            for (int i = 0; i < effects.size(); i++) {
                effects.get(i).intensity().reset(working, time);
                effects.get(i).shared().reset(working, time);
                var states = effects.get(i).states();
                for (int s = 0; s < states.size(); s++) {
                    states.get(s).reset();
                }
            }
        } else if (ctx.dt > 0) {
            for (int i = 0; i < effects.size(); i++) {
                effects.get(i).intensity().update(working, time, ctx.dt);
            }
        }
        if (reset) {
            lastTime = time;
        } else if (ctx.dt > 0) {
            lastTime = time;
        }
        ctx.time = Math.max(time, lastTime);
        boolean scan = time - lastScan > SCAN_INTERVAL || time < lastScan;
        if (scan) {
            lastScan = time;
        }
        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            ctx.intensity = effect.intensity();
            var states = effect.states();
            if (scan) {
                effect.shared().scan(ctx);
                for (int s = 0; s < states.size(); s++) {
                    states.get(s).scan(ctx);
                }
            }
            if (ctx.dt <= 0) {
                continue;
            }
            for (int s = 0; s < states.size(); s++) {
                states.get(s).update(ctx);
            }
        }
    }

    void render(FxContext ctx) {
        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            double view = effect.viewDistance();
            if (ctx.distance > view) {
                continue;
            }
            double fadeStart = view * 0.83;
            ctx.fade = (float) Math.min(1.0, (view - ctx.distance) / Math.max(1e-3, view - fadeStart));
            ctx.intensity = effect.intensity();
            var states = effect.states();
            for (int s = 0; s < states.size(); s++) {
                states.get(s).render(ctx);
            }
        }
    }
}
