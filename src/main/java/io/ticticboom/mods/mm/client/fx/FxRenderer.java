package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.attachment.visuals.EffectVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FxRenderer {
    private static final FxContext CONTEXT = new FxContext();

    private FxRenderer() {
    }

    public static FxInstance instance(MachineControllerBlockEntity be, List<EffectVisual> visuals) {
        var structure = be.getStructure();
        var rotation = be.getFormedRotation();
        if (be.getFxState() instanceof FxInstance existing && existing.matches(structure, rotation)) {
            return existing;
        }
        FxInstance created = FxInstance.create(structure, rotation, be.getBlockPos(), visuals);
        be.setFxState(created);
        return created;
    }

    public static void render(MachineControllerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        List<EffectVisual> visuals = be.effects();
        var level = be.getLevel();
        if (visuals == null || level == null || !be.getBlockState().hasProperty(ControllerState.PROPERTY)) {
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
        ctx.pose = pose.last();
        ctx.buffers = buffers;
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
        boolean working = be.getBlockState().getValue(ControllerState.PROPERTY) == ControllerState.WORKING;
        instance.tick(ctx, working);
        instance.render(ctx);
        ctx.level = null;
        ctx.buffers = null;
        ctx.pose = null;
    }
}
