package io.ticticboom.mods.mm.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class FxFluidSprite {
    private static final float INSET = 0.02f;

    private static RenderType plain;
    private static RenderType culled;

    private final float u0;
    private final float v0;
    private final float du;
    private final float dv;

    private FxFluidSprite(TextureAtlasSprite sprite) {
        u0 = sprite.getU0();
        v0 = sprite.getV0();
        du = sprite.getU1() - u0;
        dv = sprite.getV1() - v0;
    }

    @Nullable
    public static FxFluidSprite still(FluidStack fluid) {
        return of(IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid));
    }

    @Nullable
    public static FxFluidSprite flowing(FluidStack fluid) {
        FxFluidSprite flowing = of(IClientFluidTypeExtensions.of(fluid.getFluid()).getFlowingTexture(fluid));
        return flowing != null ? flowing : still(fluid);
    }

    @Nullable
    private static FxFluidSprite of(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : new FxFluidSprite(sprite);
    }

    public static RenderType renderType(boolean cull) {
        if (cull) {
            if (culled == null) {
                culled = RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
            }
            return culled;
        }
        if (plain == null) {
            plain = RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
        }
        return plain;
    }

    public float u(double u) {
        return u0 + du * (INSET + (1 - 2 * INSET) * (float) Math.max(0, Math.min(1, u)));
    }

    public float v(double v) {
        return v0 + dv * (INSET + (1 - 2 * INSET) * (float) Math.max(0, Math.min(1, v)));
    }
}
