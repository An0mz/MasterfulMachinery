package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import net.minecraft.resources.ResourceLocation;

public record EntityIngredient(
        ResourceLocation id,
        boolean tag,
        int amount
) {
}
