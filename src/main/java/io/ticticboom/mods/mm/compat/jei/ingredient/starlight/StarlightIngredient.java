package io.ticticboom.mods.mm.compat.jei.ingredient.starlight;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record StarlightIngredient(
        @Nullable ResourceLocation constellation,
        int min,
        int max
) {
}
