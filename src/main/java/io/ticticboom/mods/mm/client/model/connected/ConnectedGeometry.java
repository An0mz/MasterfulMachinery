package io.ticticboom.mods.mm.client.model.connected;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

public class ConnectedGeometry implements IUnbakedGeometry<ConnectedGeometry> {

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
        var tiles = new TextureAtlasSprite[ConnectedBakedModel.TILE_COUNT];
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = spriteGetter.apply(context.getMaterial("tile" + i));
        }
        var overlay = spriteGetter.apply(context.getMaterial("overlay"));
        return new ConnectedBakedModel(tiles, overlay, modelState, context, overrides);
    }
}
