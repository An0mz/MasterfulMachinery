package io.ticticboom.mods.mm.client.model.connected;

import io.ticticboom.mods.mm.extra.IExtraBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ConnectedBakedModel implements IDynamicBakedModel {

    public static final int TILE_COUNT = 5;

    private static final int ALONE = 0;
    private static final int JOINED = 1;
    private static final int JOINED_ALONG_U = 2;
    private static final int JOINED_ALONG_V = 3;
    private static final int INNER_CORNER = 4;

    private static final ModelProperty<byte[]> TILES = new ModelProperty<>();
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.solid(), RenderType.translucent());

    private final BakedQuad[][][] base = new BakedQuad[6][4][TILE_COUNT];
    private final BakedQuad[][] overlay = new BakedQuad[6][4];
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final ItemOverrides overrides;
    private final boolean ambientOcclusion;
    private final boolean gui3d;
    private final boolean blockLight;

    public ConnectedBakedModel(TextureAtlasSprite[] tiles, TextureAtlasSprite overlaySprite, ModelState modelState, IGeometryBakingContext context, ItemOverrides overrides) {
        this.particle = tiles[ALONE];
        this.transforms = context.getTransforms();
        this.overrides = overrides;
        this.ambientOcclusion = context.useAmbientOcclusion();
        this.gui3d = context.isGui3d();
        this.blockLight = context.useBlockLight();
        var bakery = new FaceBakery();
        for (Direction face : Direction.values()) {
            for (int quarter = 0; quarter < 4; quarter++) {
                var from = faceFrom(face);
                var to = faceTo(face);
                int a = quarter / 2;
                int b = quarter % 2;
                from[uAxis(face)] = a * 8;
                to[uAxis(face)] = a * 8 + 8;
                from[vAxis(face)] = b * 8;
                to[vAxis(face)] = b * 8 + 8;
                for (int tile = 0; tile < TILE_COUNT; tile++) {
                    base[face.ordinal()][quarter][tile] = bake(bakery, face, from, to, tiles[tile], modelState);
                }
                overlay[face.ordinal()][quarter] = bake(bakery, face, from, to, overlaySprite, modelState);
            }
        }
    }

    private static BakedQuad bake(FaceBakery bakery, Direction face, float[] from, float[] to, TextureAtlasSprite sprite, ModelState modelState) {
        var uv = new BlockFaceUV(uvs(face, from, to), 0);
        var element = new BlockElementFace(face, -1, "", uv);
        return bakery.bakeQuad(new Vector3f(from[0], from[1], from[2]), new Vector3f(to[0], to[1], to[2]), element, sprite, face, modelState, null, true);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (!(state.getBlock() instanceof IExtraBlock self)) {
            return modelData;
        }
        var group = self.getModel().resolvedConnectGroup();
        var tiles = new byte[24];
        for (Direction face : Direction.values()) {
            for (int quarter = 0; quarter < 4; quarter++) {
                tiles[face.ordinal() * 4 + quarter] = (byte) tileFor(level, pos, group, face, quarter);
            }
        }
        return ModelData.of(TILES, tiles);
    }

    private static int tileFor(BlockAndTintGetter level, BlockPos pos, String group, Direction face, int quarter) {
        var u = uNeighbour(face, quarter / 2);
        var v = vNeighbour(face, quarter % 2);
        boolean alongU = joins(level, pos.relative(u), group);
        boolean alongV = joins(level, pos.relative(v), group);
        if (!alongU && !alongV) {
            return ALONE;
        }
        if (alongU && !alongV) {
            return JOINED_ALONG_U;
        }
        if (!alongU) {
            return JOINED_ALONG_V;
        }
        return joins(level, pos.relative(u).relative(v), group) ? JOINED : INNER_CORNER;
    }

    private static boolean joins(BlockAndTintGetter level, BlockPos pos, String group) {
        return level.getBlockState(pos).getBlock() instanceof IExtraBlock other
                && group.equals(other.getModel().resolvedConnectGroup());
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side == null) {
            return List.of();
        }
        boolean everything = renderType != RenderType.solid() && renderType != RenderType.translucent();
        var result = new ArrayList<BakedQuad>(5);
        if (everything || renderType == RenderType.solid()) {
            var tiles = data.has(TILES) ? data.get(TILES) : null;
            for (int quarter = 0; quarter < 4; quarter++) {
                int tile = tiles == null ? ALONE : tiles[side.ordinal() * 4 + quarter];
                result.add(base[side.ordinal()][quarter][tile]);
            }
        }
        if (everything || renderType == RenderType.translucent()) {
            for (int quarter = 0; quarter < 4; quarter++) {
                result.add(overlay[side.ordinal()][quarter]);
            }
        }
        return result;
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return RENDER_TYPES;
    }

    @Override
    public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
        return List.of(Sheets.translucentCullBlockSheet());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean usesBlockLight() {
        return blockLight;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
    }

    private static float[] faceFrom(Direction face) {
        return switch (face) {
            case UP -> new float[]{0, 16, 0};
            case SOUTH -> new float[]{0, 0, 16};
            case EAST -> new float[]{16, 0, 0};
            default -> new float[]{0, 0, 0};
        };
    }

    private static float[] faceTo(Direction face) {
        return switch (face) {
            case DOWN -> new float[]{16, 0, 16};
            case NORTH -> new float[]{16, 16, 0};
            case WEST -> new float[]{0, 16, 16};
            default -> new float[]{16, 16, 16};
        };
    }

    private static int uAxis(Direction face) {
        return face.getAxis() == Direction.Axis.X ? 2 : 0;
    }

    private static int vAxis(Direction face) {
        return face.getAxis() == Direction.Axis.Y ? 2 : 1;
    }

    private static Direction uNeighbour(Direction face, int half) {
        if (face.getAxis() == Direction.Axis.X) {
            return half == 0 ? Direction.NORTH : Direction.SOUTH;
        }
        return half == 0 ? Direction.WEST : Direction.EAST;
    }

    private static Direction vNeighbour(Direction face, int half) {
        if (face.getAxis() == Direction.Axis.Y) {
            return half == 0 ? Direction.NORTH : Direction.SOUTH;
        }
        return half == 0 ? Direction.DOWN : Direction.UP;
    }

    private static float[] uvs(Direction face, float[] from, float[] to) {
        return switch (face) {
            case DOWN -> new float[]{from[0], 16 - to[2], to[0], 16 - from[2]};
            case UP -> new float[]{from[0], from[2], to[0], to[2]};
            case NORTH -> new float[]{16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1]};
            case SOUTH -> new float[]{from[0], 16 - to[1], to[0], 16 - from[1]};
            case WEST -> new float[]{from[2], 16 - to[1], to[2], 16 - from[1]};
            case EAST -> new float[]{16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1]};
        };
    }
}
