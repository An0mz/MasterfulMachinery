package io.ticticboom.mods.mm.compat.jei.ingredient.essentia;

import net.minecraft.resources.ResourceLocation;

public record EssentiaStack(
        ResourceLocation aspect,
        int min,
        int max
) {
}
