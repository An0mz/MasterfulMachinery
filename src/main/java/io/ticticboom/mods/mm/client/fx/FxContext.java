package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FxContext {
    public BlockAndTintGetter level;
    public boolean fullBright;
    public BlockPos origin;
    public FxIntensity intensity;
    public double time;
    public double dt;
    public float fade;
    public double distance;
    public int portColor = -1;
    public int lod;
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
        if (fullBright) {
            return LightTexture.FULL_BRIGHT;
        }
        cursor.set(origin.getX() + (int) Math.floor(x), origin.getY() + (int) Math.floor(y), origin.getZ() + (int) Math.floor(z));
        return LevelRenderer.getLightColor(level, cursor);
    }

    public boolean passable(int x, int y, int z) {
        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
        BlockState state = level.getBlockState(cursor);
        return state.isAir() || (state.getCollisionShape(level, cursor).isEmpty() && state.getFluidState().isEmpty());
    }

    public boolean passable(int x, int y, int z, double localX, double localZ) {
        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
        BlockState state = level.getBlockState(cursor);
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        VoxelShape shape = state.getCollisionShape(level, cursor);
        return shape.isEmpty() || shape.max(Direction.Axis.Y, localZ, localX) == Double.NEGATIVE_INFINITY;
    }

    public VoxelShape shape(long cell) {
        cursor.set(origin.getX() + BlockPos.getX(cell), origin.getY() + BlockPos.getY(cell), origin.getZ() + BlockPos.getZ(cell));
        return level.getBlockState(cursor).getShape(level, cursor);
    }

    public boolean solid(int x, int y, int z) {
        cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
        return !level.getBlockState(cursor).isAir();
    }
}
