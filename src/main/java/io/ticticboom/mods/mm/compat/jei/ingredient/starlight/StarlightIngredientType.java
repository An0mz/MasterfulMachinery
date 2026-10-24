package io.ticticboom.mods.mm.compat.jei.ingredient.starlight;

import mezz.jei.api.ingredients.IIngredientType;
import org.jetbrains.annotations.NotNull;

public class StarlightIngredientType implements IIngredientType<StarlightIngredient> {
    @Override
    public @NotNull Class<? extends StarlightIngredient> getIngredientClass() {
        return StarlightIngredient.class;
    }
}
