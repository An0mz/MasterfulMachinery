package io.ticticboom.mods.mm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.attachment.visuals.FluidVolumeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public class ControllerVisualsRenderer implements BlockEntityRenderer<MachineControllerBlockEntity> {
    private static final int MIN_ALPHA = 0xA0;

    @Override
    public void render(MachineControllerBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        for (FluidVolumeState volume : be.getFluidVolumes()) {
            if (!volume.getFluid().isEmpty() && volume.getFill() > 0 && volume.getBounds() != null) {
                renderFluidVolume(be, volume, pose, buffers);
            }
        }
    }

    private void renderFluidVolume(MachineControllerBlockEntity be, FluidVolumeState volume, PoseStack pose, MultiBufferSource buffers) {
        var fluid = volume.getFluid();
        var ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));

        var center = volume.getBounds().getCenter();
        var centerPos = be.getBlockPos().offset((int) Math.floor(center.x), (int) Math.floor(center.y), (int) Math.floor(center.z));
        int color = ext.getTintColor(fluid.getFluid().defaultFluidState(), be.getLevel(), centerPos);
        color = Math.max(color >>> 24, MIN_ALPHA) << 24 | (color & 0xFFFFFF);
        int packed = LevelRenderer.getLightColor(be.getLevel(), centerPos);
        int fluidLight = fluid.getFluid().getFluidType().getLightLevel(fluid);
        int light = LightTexture.pack(Math.max(LightTexture.block(packed), fluidLight), LightTexture.sky(packed));

        var cells = volume.getCellSet();
        boolean gas = volume.isGas();
        float level = volume.getLevel();
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        var last = pose.last();
        for (long cell : volume.getCells()) {
            int cx = BlockPos.getX(cell), cy = BlockPos.getY(cell), cz = BlockPos.getZ(cell);
            float lo = gas ? Math.max(cy, level) : cy;
            float hi = gas ? cy + 1 : Math.min(cy + 1, level);
            if (lo >= hi) {
                continue;
            }
            boolean west = cells.contains(BlockPos.offset(cell, -1, 0, 0));
            boolean east = cells.contains(BlockPos.offset(cell, 1, 0, 0));
            boolean north = cells.contains(BlockPos.offset(cell, 0, 0, -1));
            boolean south = cells.contains(BlockPos.offset(cell, 0, 0, 1));
            boolean below = cells.contains(BlockPos.offset(cell, 0, -1, 0));
            boolean above = cells.contains(BlockPos.offset(cell, 0, 1, 0));
            boolean openTop = gas ? !above : !(above && level > cy + 1);
            boolean openBottom = gas ? !(below && level < cy) : !below;
            float ax = cx, bx = cx + 1, az = cz, bz = cz + 1, ay = lo, by = hi;
            if (openTop) {
                face(vc, last, sprite, color, light, Direction.UP, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (openBottom) {
                face(vc, last, sprite, color, light, Direction.DOWN, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!west) {
                face(vc, last, sprite, color, light, Direction.WEST, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!east) {
                face(vc, last, sprite, color, light, Direction.EAST, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!north) {
                face(vc, last, sprite, color, light, Direction.NORTH, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
            if (!south) {
                face(vc, last, sprite, color, light, Direction.SOUTH, cx, cy, cz, ax, ay, az, bx, by, bz);
            }
        }
    }

    private static void face(VertexConsumer vc, PoseStack.Pose last, TextureAtlasSprite sprite, int color, int light, Direction face,
                             int cx, int cy, int cz, float ax, float ay, float az, float bx, float by, float bz) {
        float[][] corners = switch (face) {
            case UP -> new float[][]{{ax, by, az}, {ax, by, bz}, {bx, by, bz}, {bx, by, az}};
            case DOWN -> new float[][]{{ax, ay, bz}, {ax, ay, az}, {bx, ay, az}, {bx, ay, bz}};
            case NORTH -> new float[][]{{bx, by, az}, {bx, ay, az}, {ax, ay, az}, {ax, by, az}};
            case SOUTH -> new float[][]{{ax, by, bz}, {ax, ay, bz}, {bx, ay, bz}, {bx, by, bz}};
            case WEST -> new float[][]{{ax, by, az}, {ax, ay, az}, {ax, ay, bz}, {ax, by, bz}};
            case EAST -> new float[][]{{bx, by, bz}, {bx, ay, bz}, {bx, ay, az}, {bx, by, az}};
        };
        for (float[] c : corners) {
            float u;
            float v;
            switch (face.getAxis()) {
                case Y -> {
                    u = c[0] - cx;
                    v = c[2] - cz;
                }
                case Z -> {
                    u = c[0] - cx;
                    v = 1 - (c[1] - cy);
                }
                default -> {
                    u = c[2] - cz;
                    v = 1 - (c[1] - cy);
                }
            }
            vc.addVertex(last, c[0], c[1], c[2])
                    .setColor(color)
                    .setUv(sprite.getU(u), sprite.getV(v))
                    .setLight(light)
                    .setNormal(last, face.getStepX(), face.getStepY(), face.getStepZ());
        }
    }

    @Override
    public boolean shouldRenderOffScreen(MachineControllerBlockEntity be) {
        return be.getVisualBounds() != null;
    }

    @Override
    public AABB getRenderBoundingBox(MachineControllerBlockEntity be) {
        var bounds = be.getVisualBounds();
        return bounds != null ? bounds : BlockEntityRenderer.super.getRenderBoundingBox(be);
    }
}
