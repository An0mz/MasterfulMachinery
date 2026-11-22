package io.ticticboom.mods.mm.compat.jei.ingredient.entity;

import io.ticticboom.mods.mm.port.entity.EntityGroup;
import net.minecraft.resources.ResourceLocation;

public record EntityIngredient(
        ResourceLocation id,
        boolean tag,
        EntityGroup group,
        int amount
) {
}
