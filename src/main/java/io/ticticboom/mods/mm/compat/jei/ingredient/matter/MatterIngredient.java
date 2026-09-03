package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import net.minecraft.resources.ResourceLocation;

public record MatterIngredient(
        ResourceLocation matter,
        int amount,
        int color
) {
}
