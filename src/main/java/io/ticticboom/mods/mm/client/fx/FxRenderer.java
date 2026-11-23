package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.client.fx.studio.EffectStudio;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.attachment.visuals.EffectVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public final class FxRenderer {
    private static final FxContext CONTEXT = new FxContext();
    private static final FxContext PREVIEW = new FxContext();

    private FxRenderer() {
    }

    public static boolean enabled() {
        return MMConfigSetup.CLIENT.machineEffects.get();
    }

    public static FxInstance instance(MachineControllerBlockEntity be, List<EffectVisual> visuals) {
        var structure = be.getStructure();
        var rotation = be.getFormedRotation();
        if (be.getFxState() instanceof FxInstance existing && existing.matches(structure, rotation, visuals)) {
            return existing;
        }
        FxInstance created = FxInstance.create(structure, rotation, be.getBlockPos(), visuals);
        be.setFxState(created);
        return created;
    }

    public static FxInstance preview(@Nullable FxInstance current, StructureModel structure, BlockPos seed, List<EffectVisual> visuals) {
        if (current != null && current.matches(structure, Rotation.NONE, visuals)) {
            return current;
        }
        FxInstance created = FxInstance.create(structure, Rotation.NONE, seed, visuals);
        created.colors(List.of());
        return created;
    }

    public static void renderPreview(FxInstance instance, BlockAndTintGetter level, PoseStack pose, MultiBufferSource buffers,
                                     Vector3f camera, Vector3f left, Vector3f up, Vector3f look, boolean working, float partialTick) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || instance.effects.isEmpty()) {
            return;
        }
        FxContext ctx = PREVIEW;
        ctx.level = level;
        ctx.origin = BlockPos.ZERO;
        ctx.fullBright = true;
        ctx.lod = 0;
        ctx.time = (mc.level.getGameTime() + (double) partialTick) / 20.0;
        ctx.distance = 8;
        ctx.pose = pose.last();
        ctx.buffers = buffers;
        ctx.leftX = left.x();
        ctx.leftY = left.y();
        ctx.leftZ = left.z();
        ctx.upX = up.x();
        ctx.upY = up.y();
        ctx.upZ = up.z();
        ctx.lookX = look.x();
        ctx.lookY = look.y();
        ctx.lookZ = look.z();
        ctx.cameraX = camera.x();
        ctx.cameraY = camera.y();
        ctx.cameraZ = camera.z();
        instance.tick(ctx, working);
        instance.render(ctx);
        ctx.level = null;
        ctx.buffers = null;
        ctx.pose = null;
    }

    public static void render(MachineControllerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        long start = System.nanoTime();
        try {
            renderMachine(be, partialTick, pose, buffers);
        } finally {
            FxStats.nanos += System.nanoTime() - start;
        }
    }

    private static void renderMachine(MachineControllerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        List<EffectVisual> visuals = EffectStudio.effects(be);
        var level = be.getLevel();
        if (visuals == null || level == null || !enabled() || !be.getBlockState().hasProperty(ControllerState.PROPERTY)) {
            be.setFxState(null);
            return;
        }
        FxInstance instance = instance(be, visuals);
        if (instance.effects.isEmpty()) {
            return;
        }
        var pos = be.getBlockPos();
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 camera = cam.getPosition();
        double dx = camera.x - (pos.getX() + instance.centerX);
        double dy = camera.y - (pos.getY() + instance.centerY);
        double dz = camera.z - (pos.getZ() + instance.centerZ);
        FxContext ctx = CONTEXT;
        ctx.level = level;
        ctx.origin = pos;
        ctx.time = (level.getGameTime() + (double) partialTick) / 20.0;
        ctx.distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (ctx.distance > FxSettings.distance) {
            ctx.level = null;
            return;
        }
        ctx.lod = FxSettings.lod(ctx.distance);
        ctx.pose = pose.last();
        ctx.buffers = level == Minecraft.getInstance().level ? FxBuffers.BUFFERS : buffers;
        var left = cam.getLeftVector();
        var up = cam.getUpVector();
        var look = cam.getLookVector();
        ctx.leftX = left.x();
        ctx.leftY = left.y();
        ctx.leftZ = left.z();
        ctx.upX = up.x();
        ctx.upY = up.y();
        ctx.upZ = up.z();
        ctx.lookX = look.x();
        ctx.lookY = look.y();
        ctx.lookZ = look.z();
        ctx.cameraX = camera.x - pos.getX();
        ctx.cameraY = camera.y - pos.getY();
        ctx.cameraZ = camera.z - pos.getZ();
        boolean working = EffectStudio.working(be, be.getBlockState().getValue(ControllerState.PROPERTY) == ControllerState.WORKING);
        instance.colors(EffectStudio.active(be) ? List.of() : be.getEffectFluids());
        FxStats.machine();
        instance.tick(ctx, working);
        instance.render(ctx);
        ctx.portColor = -1;
        ctx.portStill = null;
        ctx.portFlowing = null;
        ctx.level = null;
        ctx.buffers = null;
        ctx.pose = null;
    }
}
