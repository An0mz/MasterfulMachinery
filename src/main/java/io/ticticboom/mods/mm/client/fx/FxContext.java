package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class FxContext {
    public Level level;
    public BlockPos origin;
    public FxIntensity intensity;
    public double time;
    public double dt;
    public float fade;
    public double distance;
    public int portColor = -1;
    public float leftX;
    public float leftY;
    public float leftZ;
    public float upX;
    public float upY;
    public float upZ;
    public float lookX;
    public float lookY;
    public float lookZ;
    public double cameraX;
    public double cameraY;
    public double cameraZ;
    public PoseStack.Pose pose;
    public MultiBufferSource buffers;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public int light(double x, double y, double z) {
        cursor.set(origin.getX() + (int) Math.floor(x), origin.getY() + (int) Math.floor(y), origin.getZ() + (int) Math.floor(z));
        return LevelRenderer.getLightColor(level, cursor);
    }

    public boolean passable(int x, int y, int z) {
        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
        BlockState state = level.getBlockState(cursor);
        return state.isAir() || (state.getCollisionShape(level, cursor).isEmpty() && state.getFluidState().isEmpty());
    }

    public boolean solid(int x, int y, int z) {
        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
        return !level.getBlockState(cursor).isAir();
    }
}
