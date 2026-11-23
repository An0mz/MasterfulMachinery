package io.ticticboom.mods.mm.client.fx;

import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.visuals.EffectVisual;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FxInstance {
    private static final double SCAN_INTERVAL = 1.0;
    private static final double LIGHT_INTERVAL = 0.25;

    final StructureModel structure;
    final List<EffectVisual> visuals;
    final Rotation rotation;
    final int generation;
    final List<Effect> effects;
    final double centerX;
    final double centerY;
    final double centerZ;
    private double lastTime = -1;
    private double lastScan = -1e9;
    private double lastLight = -1e9;
    private final double maxView;
    private final AABB relativeBounds;
    private AABB renderBounds;
    private AABB renderBoundsVisual;
    private List<FluidStack> fluids;

    record Effect(int visual, FxDefinition definition, FxIntensity intensity, FxShared shared, double viewDistance, List<FxLayer.State> states,
                  FxLayer.State[] slots) {
    }

    private FxInstance(StructureModel structure, List<EffectVisual> visuals, Rotation rotation, int generation, List<Effect> effects, AABB bounds) {
        this.structure = structure;
        this.visuals = visuals;
        this.rotation = rotation;
        this.generation = generation;
        this.effects = effects;
        double view = 0;
        for (Effect effect : effects) {
            view = Math.max(view, effect.viewDistance());
        }
        this.maxView = view;
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
                FxLog.warnOnce("Structure {}: no effect file for {}", structure.id(), visual.getFx());
                continue;
            }
            var anchors = visual.resolve(structure.layout(), rotation);
            var shared = new FxShared(anchors, seed + i * 31L + definition.seed() * 0x2545F4914F6CDD1DL, rotation);
            var states = new ArrayList<FxLayer.State>();
            var slots = new FxLayer.State[definition.layers().size()];
            for (int l = 0; l < slots.length; l++) {
                var state = definition.layers().get(l).create(shared);
                slots[l] = state;
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
            effects.add(new Effect(i, definition, new FxIntensity(definition.rise(), definition.fall(), definition.hold()),
                    shared, definition.viewDistance(), List.copyOf(states), slots));
        }
        return new FxInstance(structure, visuals, rotation, FxManager.generation(), List.copyOf(effects), bounds);
    }

    public AABB renderBounds(BlockPos pos, AABB visual) {
        if (renderBounds == null || renderBoundsVisual != visual) {
            AABB box = relativeBounds.move(pos);
            renderBounds = visual == null ? box : box.minmax(visual);
            renderBoundsVisual = visual;
        }
        return renderBounds;
    }

    @Nullable
    public String problem(int visual, int layer) {
        for (Effect effect : effects) {
            if (effect.visual() != visual) {
                continue;
            }
            if (layer < 0 || layer >= effect.slots().length) {
                return null;
            }
            FxLayer.State state = effect.slots()[layer];
            return state == null ? "no_anchor" : state.problem(effect.intensity().now());
        }
        return null;
    }

    public boolean matches(StructureModel structure, Rotation rotation, List<EffectVisual> visuals) {
        return this.structure == structure && this.visuals == visuals && this.rotation == rotation && this.generation == FxManager.generation();
    }

    void colors(List<FluidStack> next) {
        if (next == fluids) {
            return;
        }
        fluids = next;
        for (int i = 0; i < effects.size(); i++) {
            Effect effect = effects.get(i);
            FxShared shared = effect.shared();
            FluidStack fluid = effect.visual() < next.size() ? next.get(effect.visual()) : FluidStack.EMPTY;
            shared.portColor = fluid.isEmpty() ? -1 : FxPortColor.of(fluid);
            shared.portTint = FxPortColor.tint(fluid);
            shared.portStill = fluid.isEmpty() ? null : FxFluidSprite.still(fluid);
            shared.portFlowing = fluid.isEmpty() ? null : FxFluidSprite.flowing(fluid);
        }
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
        boolean relight = (time - lastLight > LIGHT_INTERVAL || time < lastLight) && ctx.distance <= maxView;
        if (relight) {
            lastLight = time;
            for (int i = 0; i < effects.size(); i++) {
                effects.get(i).shared().refreshLight(ctx);
            }
        }
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
            double view = ctx.fullBright ? effect.viewDistance() : Math.min(effect.viewDistance(), FxSettings.distance);
            if (ctx.distance > view) {
                continue;
            }
            double fadeStart = view * 0.83;
            ctx.fade = (float) Math.min(1.0, (view - ctx.distance) / Math.max(1e-3, view - fadeStart));
            ctx.intensity = effect.intensity();
            ctx.portColor = effect.shared().portColor;
            ctx.portTint = effect.shared().portTint;
            ctx.portStill = effect.shared().portStill;
            ctx.portFlowing = effect.shared().portFlowing;
            var states = effect.states();
            for (int s = 0; s < states.size(); s++) {
                int before = FxStats.vertices;
                states.get(s).render(ctx);
                FxStats.layer(states.get(s), FxStats.vertices - before);
            }
        }
    }
}
