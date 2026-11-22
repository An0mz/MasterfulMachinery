package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class FxRenderTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> SOFT = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> SOFT_ADDITIVE = new HashMap<>();

    private FxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling, boolean sort,
                          Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    public static RenderType soft(ResourceLocation texture, boolean additive) {
        if (additive) {
            return SOFT_ADDITIVE.computeIfAbsent(texture, t -> create("mm_fx_soft_additive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536,
                    false, true, CompositeState.builder()
                            .setShaderState(RENDERTYPE_EYES_SHADER)
                            .setTextureState(new TextureStateShard(t, true, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));
        }
        return SOFT.computeIfAbsent(texture, t -> create("mm_fx_soft", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536,
                false, true, CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(t, true, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false)));
    }
}
